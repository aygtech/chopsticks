package com.chopsticks.core.rocketmq.caller;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.chopsticks.core.Const;
import com.chopsticks.core.caller.Caller;
import com.chopsticks.core.caller.InvokeCommand;
import com.chopsticks.core.caller.InvokeResult;
import com.chopsticks.core.caller.NoticeCommand;
import com.chopsticks.core.caller.NoticeResult;
import com.chopsticks.core.concurrent.Promise;
import com.chopsticks.core.concurrent.impl.GuavaPromise;
import com.chopsticks.core.concurrent.impl.GuavaTimeoutPromise;
import com.chopsticks.core.exception.InvokeException;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeCommand;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.handler.InvokeResponse;
import com.chopsticks.core.utils.Reflect;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder; 

public class DefaultCaller implements Caller {
	
	private static final Logger log = LoggerFactory.getLogger(DefaultCaller.class);
	
	static {
		ParserConfig.getGlobalInstance().setAutoTypeSupport(true); 
	}
	
	private String namesrvAddr;
	
	private String groupName;
	
	private DefaultMQProducer producer;
	
	private DefaultMQPushConsumer callerInvokeConsumer;
	
	private ExecutorService promiseExecutor;
	
	private volatile boolean started;
	
	private static final long DEFAULT_TIMEOUT_MILLIS = 1000 * 30L;
	
	private static final MessageQueueSelector DEFAULT_MESSAGE_QUEUE_SELECTOR = new OrderedMessageQueueSelector();
	
	/**
	 *  <msgid, timeoutGuavaPromise>
	 */
	private Map<String, GuavaPromise<BaseInvokeResult>> callerInvokePromiseMap;
	
	public DefaultCaller(String groupName) {
		checkArgument(!isNullOrEmpty(groupName), "groupName cannot be null or empty");
		this.groupName = groupName;
	}
	
	private void testCaller() {
		try {
			InvokeResponse resp = new InvokeResponse("testCaller", com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow(), com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow(), new byte[0]);
			producer.send(new Message(buildRespTopic(), com.chopsticks.core.rocketmq.Const.INVOCE_RESP_TAG_SUFFIX, JSON.toJSONBytes(resp)));
		}catch (Throwable e) {
			if(e instanceof MQClientException) {
				String errMsg = e.getMessage();
				if(errMsg.contains(com.chopsticks.core.rocketmq.Const.ERROR_MSG_NO_ROUTE_INFO_OF_THIS_TOPIC)){
					e = new InvokeException("namesrv connection error", e);
				}else if(errMsg.contains(com.chopsticks.core.rocketmq.Const.ERROR_MSG_NO_NAME_SERVER_ADDRESS)) {
					e = new InvokeException("namesrv ip undefined", e);
				}
			}
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public synchronized void start() {
		if(!started) {
			callerInvokePromiseMap = new ConcurrentHashMap<String, GuavaPromise<BaseInvokeResult>>();
			buildAndStartProducer();
			buildAndStartCallerInvokeConsumer();
			testCaller();
			started = true;
		}
	}
	
	@Override
	public synchronized void shutdown() {
		if(producer != null) {
			producer.shutdown();	
		}
		if(promiseExecutor != null) {
			promiseExecutor.shutdown();
		}
		if(callerInvokeConsumer != null) {
			callerInvokeConsumer.shutdown();
		}
		started = false;
	}

	private void buildAndStartCallerInvokeConsumer() {
		callerInvokeConsumer = new DefaultMQPushConsumer(com.chopsticks.core.rocketmq.Const.CONSUMER_PREFIX + getGroupName() + com.chopsticks.core.rocketmq.Const.CALLER_INVOKE_CONSUMER_SUFFIX);
		callerInvokeConsumer.setNamesrvAddr(namesrvAddr);
		callerInvokeConsumer.setConsumeThreadMin(Const.AVAILABLE_PROCESSORS);
		callerInvokeConsumer.setConsumeThreadMax(Const.AVAILABLE_PROCESSORS);
		callerInvokeConsumer.setMessageModel(MessageModel.BROADCASTING);
		callerInvokeConsumer.setConsumeMessageBatchMaxSize(10);
		callerInvokeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
		callerInvokeConsumer.registerMessageListener(new CallerInvokeListener(callerInvokePromiseMap));
		callerInvokeConsumer.setPullThresholdSizeForTopic(50);
		try {
			callerInvokeConsumer.subscribe(buildRespTopic(), com.chopsticks.core.rocketmq.Const.ALL_TAGS);
			callerInvokeConsumer.start();
			Reflect.on(callerInvokeConsumer)
				   .field("defaultMQPushConsumerImpl")
				   .field("consumeMessageService")
				   .field("consumeExecutor")
   					.set("threadFactory", new ThreadFactoryBuilder()
											.setDaemon(true)
											.setNameFormat(callerInvokeConsumer.getConsumerGroup() + "_%d")
											.build());
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}

	private void buildAndStartProducer() {
		producer = new DefaultMQProducer(com.chopsticks.core.rocketmq.Const.PRODUCER_PREFIX + getGroupName());
		producer.setNamesrvAddr(namesrvAddr);
		producer.setRetryAnotherBrokerWhenNotStoreOK(true);
		producer.setDefaultTopicQueueNums(16);
		try {
			producer.start();
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	public BaseInvokeResult invoke(BaseInvokeCommand cmd) {
		return this.invoke(cmd, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	public BaseInvokeResult invoke(BaseInvokeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		try {
			return this.asyncInvoke(cmd, timeout, timeoutUnit).get();
		} catch (Throwable e) {
			if(e instanceof ExecutionException) {
				e = e.getCause();
			}
			if(e instanceof CancellationException) {
				e = new TimeoutException();
			}
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}

	public Promise<BaseInvokeResult> asyncInvoke(BaseInvokeCommand cmd) {
		return this.asyncInvoke(cmd, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	public Promise<BaseInvokeResult> asyncInvoke(final BaseInvokeCommand cmd, final long timeout, final TimeUnit timeoutUnit) {
		checkArgument(started, "must be call method start");
		final GuavaTimeoutPromise<BaseInvokeResult> promise = new GuavaTimeoutPromise<BaseInvokeResult>(timeout, timeoutUnit);
		try {
			InvokeRequest req = buildInvokeRequest(cmd, timeout, timeoutUnit);
			callerInvokePromiseMap.put(req.getReqId(), promise);
			promise.addListener(new CallerTimoutPromiseListener(callerInvokePromiseMap, req.getReqId()));
			Message msg = buildInvokeMessage(req, cmd, timeout, timeoutUnit);
			producer.send(msg, new InvokeSendCallback(promise));
		} catch (Throwable e) {
			promise.setException(e);
		}
	
		return promise;
	}
	
	
	public BaseNoticeResult notice(BaseNoticeCommand cmd) {
		return this.notice(cmd, (String)null);
	}

	public BaseNoticeResult notice(BaseNoticeCommand cmd, Object orderKey) {
		try {
			return this.asyncNotice(cmd, orderKey).get();
		}catch (Throwable e) {
			if(e instanceof ExecutionException) {
				e = e.getCause();
			}
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	public Promise<BaseNoticeResult> asyncNotice(BaseNoticeCommand cmd) {
		return this.asyncNotice(cmd, null);
	}
	
	public Promise<BaseNoticeResult> asyncNotice(final BaseNoticeCommand cmd, final Object orderKey) {
		checkArgument(started, "must be call method start");
		final GuavaPromise<BaseNoticeResult> promise = new GuavaPromise<BaseNoticeResult>();
		try {
			Message msg = buildNoticeMessage(cmd, orderKey);
			NoticeSendCallback callback = new NoticeSendCallback(promise);
			if(orderKey == null) {
				producer.send(msg, callback);
			}else {
				producer.send(msg, DEFAULT_MESSAGE_QUEUE_SELECTOR , orderKey, callback);
			}
		}catch (Throwable e) {
			promise.setException(e);
		}
	
		return promise;
	}
	
	private Message buildInvokeMessage(InvokeRequest req, BaseInvokeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		Message msg = new Message(buildTopic(cmd), cmd.getTag(), cmd.getBody());
		msg.putUserProperty(com.chopsticks.core.rocketmq.Const.INVOKE_REQUEST_KEY, JSON.toJSONString(req));
		return msg;
	}
	
	private DelayNoticeRequest buildDelayNoticeRequest(BaseNoticeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		DelayNoticeRequest req = new DelayNoticeRequest();
		req.setExecuteTime(com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow() + timeoutUnit.toMillis(timeout));
		return req;
	}

	private InvokeRequest buildInvokeRequest(BaseInvokeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		InvokeRequest req = new InvokeRequest();
		req.setReqId(UUID.randomUUID().toString());
		req.setReqTime(com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow());
		req.setDeadline(req.getReqTime() + timeoutUnit.toMillis(timeout));
		req.setRespTopic(buildRespTopic());
		req.setRespTag(cmd.getTag() + com.chopsticks.core.rocketmq.Const.INVOCE_RESP_TAG_SUFFIX);
		return req;
	}

	private String buildRespTopic() {
		return getGroupName() + com.chopsticks.core.rocketmq.Const.INVOCE_RESP_TOPIC_SUFFIX;
	}
	
	private Message buildDelayNoticeMessage(BaseNoticeCommand cmd, Long delay, TimeUnit delayTimeUnit) {
		Message msg = buildNoticeMessage(cmd, null);
		if(delay != null 
		&& delayTimeUnit != null
		&& delay > 0) {
			Optional<Entry<Long, Integer>> delayLevel = com.chopsticks.core.rocketmq.Const.getDelayLevel(delayTimeUnit.toMillis(delay));
			if(delayLevel.isPresent()) {
				if(!delay.equals(delayLevel.get().getKey())) {
					DelayNoticeRequest req = buildDelayNoticeRequest(cmd, delay, delayTimeUnit);
					msg.putUserProperty(com.chopsticks.core.rocketmq.Const.DELAY_NOTICE_REQUEST_KEY, JSON.toJSONString(req));
				}
				msg.setDelayTimeLevel(delayLevel.get().getValue());
			}else {
				log.warn("unsupport notice delay");
			}
		}
		return msg;
	}
	
	private Message buildNoticeMessage(BaseNoticeCommand cmd, Object orderKey) {
		Message msg = new Message(buildTopic(cmd, orderKey), cmd.getTag(), cmd.getBody());
		return msg;
	}
	
	private String buildTopic(BaseCommand cmd) {
		return buildTopic(cmd, null);
	}
	
	private String buildTopic(BaseCommand cmd, Object orderKey) {
		String topic = cmd.getTopic();
		if(cmd instanceof BaseInvokeCommand) {
			topic = buildInvokeTopic(topic);
		}else if(cmd instanceof BaseNoticeCommand) {
			if(orderKey == null) {
				topic = buildNoticeTopic(topic);
			}else {
				topic = buildOrderNoticeTopic(topic);
			}
		}
		return buildSuccessTopic(topic);
	}
	
	protected String buildSuccessTopic(String topic) {
		return topic.replaceAll("\\.", "_").replaceAll("\\$", "-");
	}
	
	protected String buildOrderNoticeTopic(String topic) {
		return topic + com.chopsticks.core.rocketmq.Const.ORDERED_NOTICE_TOPIC_SUFFIX;
	}
	
	protected String buildNoticeTopic(String topic) {
		return topic + com.chopsticks.core.rocketmq.Const.NOTICE_TOPIC_SUFFIX;
	}
	
	protected String buildInvokeTopic(String topic) {
		return topic + com.chopsticks.core.rocketmq.Const.INVOKE_TOPIC_SUFFIX;
	}

	protected DefaultMQProducer getProducer() {
		return producer;
	}
	
	protected String getNamesrvAddr() {
		return namesrvAddr;
	}
	public void setNamesrvAddr(String namesrvAddr) {
		this.namesrvAddr = namesrvAddr;
	}
	protected String getGroupName() {
		return groupName;
	}

	@Override
	public InvokeResult invoke(InvokeCommand cmd) {
		return this.invoke(cmd, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	@Override
	public InvokeResult invoke(InvokeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		try {
			return this.asyncInvoke(cmd, timeout, timeoutUnit).get();
		} catch (Throwable e) {
			if(e instanceof ExecutionException) {
				e = e.getCause();
			}
			if(e instanceof CancellationException) {
				e = new TimeoutException();
			}
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Promise<? extends InvokeResult> asyncInvoke(InvokeCommand cmd) {
		return this.asyncInvoke(cmd, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	@Override
	public Promise<? extends InvokeResult> asyncInvoke(InvokeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		return this.asyncInvoke(buildBaseInvokeCommand(cmd), timeout, timeoutUnit);
	}

	@Override
	public NoticeResult notice(NoticeCommand cmd) {
		return this.notice(cmd, null);
	}

	@Override
	public NoticeResult notice(NoticeCommand cmd, Object orderKey) {
		try {
			return this.asyncNotice(cmd, orderKey).get();
		}catch (Throwable e) {
			if(e instanceof ExecutionException) {
				e = e.getCause();
			}
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Promise<? extends NoticeResult> asyncNotice(NoticeCommand cmd) {
		return this.asyncNotice(cmd, null);
	}

	@Override
	public Promise<? extends NoticeResult> asyncNotice(NoticeCommand cmd, Object orderKey) {
		return this.asyncNotice(buildBaseNoticeCommand(cmd), orderKey);
	}
	
	private BaseNoticeCommand buildBaseNoticeCommand(NoticeCommand cmd) {
		if(cmd instanceof BaseNoticeCommand) {
			return (BaseNoticeCommand) cmd;
		}else {
			return new DefaultNoticeCommand(com.chopsticks.core.rocketmq.Const.DEFAULT_TOPIC, cmd.getMethod(), cmd.getBody());
		}
	}
	
	private BaseInvokeCommand buildBaseInvokeCommand(InvokeCommand cmd) {
		if(cmd instanceof BaseInvokeCommand) {
			return (BaseInvokeCommand) cmd;
		}else {
			return new DefaultInvokeCommand(com.chopsticks.core.rocketmq.Const.DEFAULT_TOPIC, cmd.getMethod(), cmd.getBody());
		}
	}
	
	@Override
	public NoticeResult notice(NoticeCommand cmd, Long delay, TimeUnit delayTimeUnit) {
		return this.notice(buildBaseNoticeCommand(cmd), delay, delayTimeUnit);
	}

	@Override
	public Promise<? extends NoticeResult> asyncNotice(NoticeCommand cmd, Long delay, TimeUnit delayTimeUnit) {
		return this.asyncNotice(buildBaseNoticeCommand(cmd), delay, delayTimeUnit);
	}
	
	
	public BaseNoticeResult notice(BaseNoticeCommand cmd, Long delay, TimeUnit delayTimeUnit) {
		try {
			return this.asyncNotice(cmd, delay, delayTimeUnit).get();
		}catch (Throwable e) {
			if(e instanceof ExecutionException) {
				e = e.getCause();
			}
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	public Promise<BaseNoticeResult> asyncNotice(final BaseNoticeCommand cmd, final Long delay, final TimeUnit delayTimeUnit) {
		checkArgument(started, "must be call method start");
		final GuavaPromise<BaseNoticeResult> promise = new GuavaPromise<BaseNoticeResult>();
		try {
			Message msg = buildDelayNoticeMessage(cmd, delay, delayTimeUnit);
			NoticeSendCallback callback = new NoticeSendCallback(promise);
			producer.send(msg, callback);
		}catch (Throwable e) {
			promise.setException(e);
		}
	
		return promise;
	}
}
