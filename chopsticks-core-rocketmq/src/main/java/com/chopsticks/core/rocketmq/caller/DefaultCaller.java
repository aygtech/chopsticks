package com.chopsticks.core.rocketmq.caller;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.common.concurrent.Promise;
import com.chopsticks.common.concurrent.impl.DefaultTimeoutPromise;
import com.chopsticks.core.Const;
import com.chopsticks.core.caller.Caller;
import com.chopsticks.core.caller.InvokeCommand;
import com.chopsticks.core.caller.InvokeResult;
import com.chopsticks.core.caller.NoticeCommand;
import com.chopsticks.core.caller.NoticeResult;
import com.chopsticks.core.exception.CoreException;
import com.chopsticks.core.rocketmq.caller.impl.BatchInvokerSender;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeCommand;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.caller.impl.SingleInvokeSender;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.netty.util.internal.ThreadLocalRandom; 

/**
 * 默认发送者实现
 * @author zilong.li
 *
 */
public class DefaultCaller implements Caller {
	
	private static final Logger log = LoggerFactory.getLogger(DefaultCaller.class);
	
	private static final Map<String, String> SAFE_NAMES = ImmutableMap.of("\\.", "_-_");
	
	private String namesrvAddr;
	
	private String groupName;
	
	private DefaultMQProducer producer;
	
	private DefaultMQPushConsumer callerInvokeConsumer;
	
	private volatile boolean started;
	
	protected static final long DEFAULT_SYNC_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30);
	
	protected static final long DEFAULT_ASYNC_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);
	
	private static final MessageQueueSelector DEFAULT_MESSAGE_QUEUE_SELECTOR = new OrderedMessageQueueSelector();
	
	private long batchExecuteIntervalMillis = TimeUnit.MILLISECONDS.toMillis(100L);
	
	private BaseInvokeSender invokeSender;
	
	private boolean invokable = true;
	
	private DefaultMQAdminExt mqAdminExt;
	
	private static final Cache</*topic + tag*/String, /*consumer exist*/Boolean> INVOKE_TOPIC_TAG_MONITOR = CacheBuilder.newBuilder().expireAfterWrite(DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).build();
	private static final Cache</*topic*/String, Set<ConsumerConnection>> INVOKE_TOPIC_MONITOR = CacheBuilder.newBuilder().expireAfterWrite(DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).build();
	
	
	private static final Cache</*topic + tag*/String, /*consumer exist*/Boolean> NOTICE_TOPIC_TAG_MONITOR = CacheBuilder.newBuilder().build();
	private static final Cache</*topic*/String, Set<ConsumerConnection>> NOTICE_TOPIC_MONITOR = CacheBuilder.newBuilder().build();
	
	/**
	 *  <msgid, timeoutGuavaPromise>
	 */
	private Map<String, DefaultTimeoutPromise<BaseInvokeResult>> callerInvokePromiseMap;
	
	public DefaultCaller(String groupName) {
		checkArgument(!isNullOrEmpty(groupName), "groupName cannot be null or empty");
		this.groupName = groupName;
		for(Entry<String, String> entry : SAFE_NAMES.entrySet()) {
			this.groupName = this.groupName.replaceAll(entry.getKey(), entry.getValue());
		}
	}
	
	@Override
	public synchronized void start() {
		if(!started) {
			try {
				mqAdminExt = buildAdminExt();
				callerInvokePromiseMap = new ConcurrentHashMap<String, DefaultTimeoutPromise<BaseInvokeResult>>();
				producer = buildAndStartProducer();
				invokeSender = buildInvokeSender(producer, batchExecuteIntervalMillis);
				callerInvokeConsumer = buildAndStartCallerInvokeConsumer();
				started = true;	
			}catch (Throwable e) {
				if(producer != null) {
					producer.shutdown();
				}
				if(invokeSender != null) {
					invokeSender.shutdown();
				}
				if(callerInvokeConsumer != null) {
					callerInvokeConsumer.shutdown();
				}
				if(e instanceof CoreException) {
					throw (CoreException)e;
				}else {
					throw new DefaultCoreException(e);
				}
			}
		}
	}
	


	private DefaultMQAdminExt buildAdminExt() {
		DefaultMQAdminExt mqAdminExt = new DefaultMQAdminExt(getGroupName() + com.chopsticks.core.rocketmq.Const.INVOKE_ADMIN_EXT_SUFFIX, TimeUnit.MINUTES.toMillis(1L));
		try {
			mqAdminExt.setNamesrvAddr(namesrvAddr);
			mqAdminExt.start();
		}catch (Throwable e) {
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new DefaultCoreException(e);
			}
		}
		return mqAdminExt;
		
	}

	private BaseInvokeSender buildInvokeSender(DefaultMQProducer producer, long batchExecuteIntervalMillis) {
		BaseInvokeSender invokeSender = null;
		if(isInvokable()) {
			if(batchExecuteIntervalMillis > 0L) {
				invokeSender = new BatchInvokerSender(producer, batchExecuteIntervalMillis);
			}else {
				invokeSender = new SingleInvokeSender(producer);
			}
		}
		return invokeSender;
	}

	@Override
	public synchronized void shutdown() {
		if(producer != null) {
			producer.shutdown();
			producer = null;
			if(invokeSender != null) {
				invokeSender.shutdown();
				invokeSender = null;
			}
			if(mqAdminExt != null) {
				mqAdminExt.shutdown();
				mqAdminExt = null;
			}
		}
		if(callerInvokeConsumer != null) {
			callerInvokeConsumer.shutdown();
			callerInvokeConsumer = null;
		}
		started = false;
	}
	
	private DefaultMQPushConsumer buildAndStartCallerInvokeConsumer() {
		DefaultMQPushConsumer callerInvokeConsumer = null;
		if(isInvokable()) {
			callerInvokeConsumer = new DefaultMQPushConsumer(com.chopsticks.core.rocketmq.Const.CONSUMER_PREFIX + getGroupName() + com.chopsticks.core.rocketmq.Const.CALLER_INVOKE_CONSUMER_SUFFIX);
			callerInvokeConsumer.setNamesrvAddr(namesrvAddr);
			callerInvokeConsumer.setConsumeThreadMin(Const.AVAILABLE_PROCESSORS);
			callerInvokeConsumer.setConsumeThreadMax(Const.AVAILABLE_PROCESSORS);
			callerInvokeConsumer.setMessageModel(MessageModel.CLUSTERING);
			callerInvokeConsumer.setConsumeMessageBatchMaxSize(10);
			callerInvokeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			callerInvokeConsumer.registerMessageListener(new CallerInvokeListener(callerInvokePromiseMap));
			callerInvokeConsumer.setPullThresholdSizeForTopic(10);
			try {
				String topic = buildRespTopic();
				callerInvokeConsumer.subscribe(topic, com.chopsticks.core.rocketmq.Const.ALL_TAGS);
				createTopics(Sets.newHashSet(topic));
				checkConsumerSubscription(callerInvokeConsumer);
				callerInvokeConsumer.start();
				callerInvokeConsumer = com.chopsticks.core.rocketmq.Const.buildConsumer(callerInvokeConsumer);
				long waitRebalanceMillis = 100L;
				while(getRespQueue(callerInvokeConsumer).isEmpty()) {
					TimeUnit.MILLISECONDS.sleep(waitRebalanceMillis);
					log.warn("continue wait rebalance time {}ms", waitRebalanceMillis);
				}
			}catch (Throwable e) {
				if(callerInvokeConsumer != null) {
					callerInvokeConsumer.shutdown();
				}
				if(e instanceof CoreException) {
					throw (CoreException)e;
				}else {
					throw new DefaultCoreException(e);
				}
			}
		}
		return callerInvokeConsumer;
	}

	private DefaultMQProducer buildAndStartProducer() {
		DefaultMQProducer producer = null;
		producer = new DefaultMQProducer(com.chopsticks.core.rocketmq.Const.PRODUCER_PREFIX + getGroupName());
		producer.setNamesrvAddr(namesrvAddr);
		producer.setSendMsgTimeout(Long.valueOf(DEFAULT_ASYNC_TIMEOUT_MILLIS).intValue());
		producer.setRetryAnotherBrokerWhenNotStoreOK(true);
		producer.setDefaultTopicQueueNums(com.chopsticks.core.rocketmq.Const.DEFAULT_TOPIC_QUEUE_SIZE);
		try {
			producer.start();
			return producer;
		}catch (Throwable e) {
			if(producer != null) {
				producer.shutdown();
			}
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new DefaultCoreException(e);
			}
		}
	}
	
	public BaseInvokeResult invoke(BaseInvokeCommand cmd) {
		return this.invoke(cmd, DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	public BaseInvokeResult invoke(BaseInvokeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		Promise<BaseInvokeResult> promise = null;
		try {
			promise = this.asyncInvoke(cmd, timeout, timeoutUnit);
			return promise.get();
		} catch (Throwable e) {
			while(e instanceof ExecutionException) {
				e = e.getCause();
			}
			if(e instanceof CancellationException) {
				e = new TimeoutException();
			}
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new DefaultCoreException(e);
			}
		}
	}

	public Promise<BaseInvokeResult> asyncInvoke(BaseInvokeCommand cmd) {
		return this.asyncInvoke(cmd, DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}
	
	public Promise<BaseInvokeResult> asyncInvoke(final BaseInvokeCommand cmd, final long timeout, final TimeUnit timeoutUnit) {
		checkArgument(started, "must be call method start");
		checkArgument(invokable, "must be support invokable");
		checkArgument(!Strings.isNullOrEmpty(cmd.getMethod()), "method cannot be null or empty");
		final DefaultTimeoutPromise<BaseInvokeResult> promise = new DefaultTimeoutPromise<BaseInvokeResult>(timeout, timeoutUnit);
		try {
			InvokeRequest req = buildInvokeRequest(cmd, timeout, timeoutUnit);
			callerInvokePromiseMap.put(req.getReqId(), promise);
			final Message msg = buildInvokeMessage(req, cmd, timeout, timeoutUnit);
			if(!checkInvokeMessage(msg)) {
				throw new DefaultCoreException(String.format("%s.%s cannot found executor", cmd.getTopic(), cmd.getTag())).setCode(DefaultCoreException.INVOKE_EXECUTOR_NOT_FOUND);
			}
			invokeSender.send(msg, promise);
			promise.addListener(new CallerInvokeTimoutPromiseListener(callerInvokePromiseMap, req));
		} catch (Throwable e) {
			promise.setException(e);
		}
	
		return promise;
	}
	
	// TODO 未实现，异步调用是否存在处理者
	/**
	 * 判断是否有消费者处理，不管在线离线 
	 * @param msg
	 * @return
	 * @throws ExecutionException
	 */
	private Boolean checkNoticeMessage(final Message msg) throws ExecutionException{
		return NOTICE_TOPIC_TAG_MONITOR.get(msg.getTopic() + msg.getTags(), new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				boolean examineConsumerConnectionInfo = false;
				return null;
			}
		});
	}
	/**
	 * 判断消息是否有在线的消费者处理
	 * @param msg
	 * @return
	 * @throws ExecutionException
	 */
	private Boolean checkInvokeMessage(final Message msg) throws ExecutionException {
		return INVOKE_TOPIC_TAG_MONITOR.get(msg.getTopic() + msg.getTags(), new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				boolean examineConsumerConnectionInfo = false;
				Set<ConsumerConnection> consumerConns = INVOKE_TOPIC_MONITOR.get(msg.getTopic(), new Callable<Set<ConsumerConnection>>() {
					@Override
					public Set<ConsumerConnection> call() throws Exception {
						Set<ConsumerConnection> consumerConns = Sets.newHashSet();
						try {
							GroupList groupList = mqAdminExt.queryTopicConsumeByWho(msg.getTopic());
							for(String groupName : groupList.getGroupList()) {
								try {
									if(groupName.endsWith(com.chopsticks.core.rocketmq.Const.INVOKE_CONSUMER_SUFFIX)) {
										ConsumerConnection consumerConn = mqAdminExt.examineConsumerConnectionInfo(groupName);
										consumerConns.add(consumerConn);
									}
								}catch (Throwable e) {
									continue;
								}
							}
						}catch (Throwable e) {
						}
						return consumerConns;
					}
				});
				for(ConsumerConnection consumerConn : consumerConns) {
					for(Entry<String, SubscriptionData> entry : consumerConn.getSubscriptionTable().entrySet()) {
						if(entry.getKey().equals(msg.getTopic())
						&& (entry.getValue().getTagsSet().contains(msg.getTags()) 
							|| entry.getValue().getTagsSet().contains(com.chopsticks.core.rocketmq.Const.ALL_TAGS))) {
							examineConsumerConnectionInfo = true;
							break;
						}
					}
				}
				return examineConsumerConnectionInfo;
			}
		});
	}
	
	public BaseNoticeResult notice(BaseNoticeCommand cmd) {
		try {
			return this.asyncNotice(cmd).get();
		}catch (Throwable e) {
			while(e instanceof ExecutionException) {
				e = e.getCause();
			}
			if(e instanceof CancellationException) {
				e = new TimeoutException();
			}
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new DefaultCoreException(e);
			}
		}
	}
	
	public BaseNoticeResult notice(BaseNoticeCommand cmd, Object orderKey) {
		try {
			return this.asyncNotice(cmd, orderKey).get();
		}catch (Throwable e) {
			while(e instanceof ExecutionException) {
				e = e.getCause();
			}
			if(e instanceof CancellationException) {
				e = new TimeoutException();
			}
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new DefaultCoreException(e);
			}
		}
	}
	
	public Promise<BaseNoticeResult> asyncNotice(BaseNoticeCommand cmd) {
		checkArgument(started, "must be call method start");
		checkArgument(!Strings.isNullOrEmpty(cmd.getMethod()), "method cannot be null or empty");
		final DefaultTimeoutPromise<BaseNoticeResult> promise = new DefaultTimeoutPromise<BaseNoticeResult>(DEFAULT_ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		try {
			Message msg = buildNoticeMessage(cmd);
			NoticeSendCallback callback = new NoticeSendCallback(promise);
			producer.send(msg, callback);
			promise.addListener(new CallerNoticeTimeoutPromiseListener(callback));
		}catch (Throwable e) {
			promise.setException(e);
		}
		return promise;
	}
	
	public Promise<BaseNoticeResult> asyncNotice(final BaseNoticeCommand cmd, final Object orderKey) {
		checkArgument(started, "must be call method start");
		checkArgument(orderKey != null, "orderKey cannot be null");
		checkArgument(!Strings.isNullOrEmpty(cmd.getMethod()), "method cannot be null or empty");
		final DefaultTimeoutPromise<BaseNoticeResult> promise = new DefaultTimeoutPromise<BaseNoticeResult>(DEFAULT_ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		try {
			Message msg = buildOrderedNoticeMessage(cmd, orderKey);
			NoticeSendCallback callback = new NoticeSendCallback(promise);
			producer.send(msg, DEFAULT_MESSAGE_QUEUE_SELECTOR , orderKey, callback);
			promise.addListener(new CallerNoticeTimeoutPromiseListener(callback));
		}catch (Throwable e) {
			promise.setException(e);
		}
	
		return promise;
	}
	
	private DelayNoticeRequest buildDelayNoticeRequest(BaseNoticeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		DelayNoticeRequest req = new DelayNoticeRequest();
		req.setReqTime(com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow());
		req.setInvokeTime(com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow());
		req.setExecuteTime(req.getInvokeTime() + timeoutUnit.toMillis(timeout));
		req.setExtParams(cmd.getExtParams());
		Set<String> traceNo = Sets.newHashSet(cmd.getTraceNos());
		req.setTraceNos(traceNo);
		return req;
	}
	
	private NoticeRequest buildNoticeRequest(BaseNoticeCommand cmd) {
		NoticeRequest req = new NoticeRequest();
		req.setReqTime(com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow());
		req.setTraceNos(cmd.getTraceNos());
		req.setExtParams(cmd.getExtParams());
		return req;
	}
	
	private OrderedNoticeRequest buildOrderedNoticeRequest(BaseNoticeCommand cmd, Object orderKey) {
		OrderedNoticeRequest req = new OrderedNoticeRequest();
		req.setReqTime(com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow());
		req.setExtParams(cmd.getExtParams());
		req.setTraceNos(cmd.getTraceNos());
		return req;
	}

	protected InvokeRequest buildInvokeRequest(BaseInvokeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		InvokeRequest req = new InvokeRequest();
		String respTopic = buildRespTopic();
		req.setReqId(UUID.randomUUID().toString());
		req.setReqTime(com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow());
		req.setDeadline(req.getReqTime() + timeoutUnit.toMillis(timeout));
		req.setRespTopic(respTopic);
		req.setRespTag(cmd.getTag() + com.chopsticks.core.rocketmq.Const.INVOCE_RESP_TAG_SUFFIX);
		req.setRespCompress(true);
		req.setExtParams(cmd.getExtParams());
		req.setTraceNos(cmd.getTraceNos());
		try {
			List<MessageQueue> mqList = getRespQueue(this.callerInvokeConsumer);
			MessageQueue mq = null;
			if(mqList.isEmpty()) {
				throw new DefaultCoreException(String.format("resp queue %s is empty", getGroupName()));
			}else {
				mq = mqList.get(ThreadLocalRandom.current().nextInt(mqList.size()));
			}
			req.setRespQueue(mq);
		}catch (Throwable e) {
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new DefaultCoreException(e);
			}
		}
		
		return req;
	}
	private List<MessageQueue> getRespQueue(DefaultMQPushConsumer callerInvokeConsumer){
		MessageQueue[] mqs = callerInvokeConsumer.getDefaultMQPushConsumerImpl().getRebalanceImpl().getProcessQueueTable().keySet().toArray(new MessageQueue[0]);
		List<MessageQueue> mqList = Lists.newArrayList(mqs);
		for(Iterator<MessageQueue> iter = mqList.iterator(); iter.hasNext();) {
			if(iter.next().getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
				iter.remove();
			}
		}
		return mqList;
	}
	private String buildRespTopic() {
		return getGroupName() + com.chopsticks.core.rocketmq.Const.INVOCE_RESP_TOPIC_SUFFIX;
	}
	
	protected Message buildInvokeMessage(InvokeRequest req, BaseInvokeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		Message msg = new Message(buildInvokeTopic(cmd.getTopic()), cmd.getTag(), cmd.getBody());
		msg.putUserProperty(com.chopsticks.core.rocketmq.Const.INVOKE_REQUEST_KEY, JSON.toJSONString(req));
		Set<String> traceNos = Sets.newHashSet(cmd.getTraceNos());
		traceNos.add(com.chopsticks.core.rocketmq.Const.buildTraceInvokeReqId(req.getReqId()));
		traceNos.add(buildTraceTag(cmd.getTag()));
		msg.setKeys(traceNos);
		return msg;
	}
	
	private Message buildNoticeMessage(BaseNoticeCommand cmd) {
		NoticeRequest req = buildNoticeRequest(cmd);
		Message msg = new Message(buildNoticeTopic(cmd.getTopic()), cmd.getTag(), cmd.getBody());
		msg.putUserProperty(com.chopsticks.core.rocketmq.Const.NOTICE_REQUEST_KEY, JSON.toJSONString(req));
		Optional<Entry<Long, Integer>> level = com.chopsticks.core.rocketmq.Const.getDelayLevel(TimeUnit.SECONDS.toMillis(1L));
		if(level.isPresent()) {
			msg.setDelayTimeLevel(level.get().getValue());
		}
		Set<String> traceNo = Sets.newHashSet(cmd.getTraceNos());
		traceNo.add(buildTraceTag(cmd.getTag()));
		msg.setKeys(traceNo);
		return msg;
	}
	
	private Message buildDelayNoticeMessage(BaseNoticeCommand cmd, Long delay, TimeUnit delayTimeUnit) {
		Message msg = new Message(buildDelayNoticeTopic(cmd.getTopic()), cmd.getTag(), cmd.getBody());
		if(delay != null 
		&& delayTimeUnit != null
		&& delay > 0) {
			Optional<Entry<Long, Integer>> delayLevel = com.chopsticks.core.rocketmq.Const.getDelayLevel(delayTimeUnit.toMillis(delay));
			if(delayLevel.isPresent()) {
				msg.setDelayTimeLevel(delayLevel.get().getValue());
			}else {
				Optional<Entry<Long, Integer>> level = com.chopsticks.core.rocketmq.Const.getDelayLevel(TimeUnit.SECONDS.toMillis(10L));
				if(level.isPresent()) {
					log.warn("delay notice is short : {}, change to : {}", delay, level.get().getKey());
					msg.setDelayTimeLevel(level.get().getValue());
				}else {
					throw new DefaultCoreException("not found delay");
				}
			}
			DelayNoticeRequest req = buildDelayNoticeRequest(cmd, delay, delayTimeUnit);
			msg.putUserProperty(com.chopsticks.core.rocketmq.Const.DELAY_NOTICE_REQUEST_KEY, JSON.toJSONString(req));
		}
		Set<String> traceNo = Sets.newHashSet(cmd.getTraceNos());
		traceNo.add(buildTraceTag(cmd.getTag()));
		msg.setKeys(traceNo);
		return msg;
	}
	
	private Message buildOrderedNoticeMessage(BaseNoticeCommand cmd, Object orderKey) {
		Message msg = new Message(buildOrderedNoticeTopic(cmd.getTopic()), cmd.getTag(), cmd.getBody());
		OrderedNoticeRequest req = buildOrderedNoticeRequest(cmd, orderKey);
		msg.putUserProperty(com.chopsticks.core.rocketmq.Const.ORDERED_NOTICE_REQUEST_KEY, JSON.toJSONString(req));
		Optional<Entry<Long, Integer>> level = com.chopsticks.core.rocketmq.Const.getDelayLevel(TimeUnit.SECONDS.toMillis(1L));
		if(level.isPresent()) {
			msg.setDelayTimeLevel(level.get().getValue());
		}
		Set<String> traceNo = Sets.newHashSet(cmd.getTraceNos());
		traceNo.add(buildTraceTag(cmd.getTag()));
		msg.setKeys(traceNo);
		return msg;
	}
	
	protected String buildSuccessTopic(String topic) {
		return topic.replaceAll("\\.", "_").replaceAll("\\$", "-");
	}
	
	protected String buildOrderedNoticeTopic(String topic) {
		return buildSuccessTopic(topic + com.chopsticks.core.rocketmq.Const.ORDERED_NOTICE_TOPIC_SUFFIX);
	}
	
	protected String buildNoticeTopic(String topic) {
		return buildSuccessTopic(topic + com.chopsticks.core.rocketmq.Const.NOTICE_TOPIC_SUFFIX);
	}
	
	protected String buildDelayNoticeTopic(String topic) {
		return buildSuccessTopic(topic + com.chopsticks.core.rocketmq.Const.DELAY_NOTICE_TOPIC_SUFFIX);
	}
	
	protected String buildInvokeTopic(String topic) {
		return buildSuccessTopic(topic + com.chopsticks.core.rocketmq.Const.INVOKE_TOPIC_SUFFIX);
	}

	public DefaultMQProducer getProducer() {
		return producer;
	}
	
	protected String getNamesrvAddr() {
		return namesrvAddr;
	}
	public void setNamesrvAddr(String namesrvAddr) {
		this.namesrvAddr = namesrvAddr;
	}
	public String getGroupName() {
		return groupName;
	}

	@Override
	public InvokeResult invoke(InvokeCommand cmd) {
		return this.invoke(cmd, DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	@Override
	public InvokeResult invoke(InvokeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		try {
			return this.asyncInvoke(cmd, timeout, timeoutUnit).get();
		} catch (Throwable e) {
			while(e instanceof ExecutionException) {
				e = e.getCause();
			}
			if(e instanceof CancellationException) {
				e = new TimeoutException();
			}
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new DefaultCoreException(e);
			}
		}
	}

	@Override
	public Promise<? extends InvokeResult> asyncInvoke(InvokeCommand cmd) {
		return this.asyncInvoke(cmd, DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
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
			while(e instanceof ExecutionException) {
				e = e.getCause();
			}
			if(e instanceof CancellationException) {
				e = new TimeoutException();
			}
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new DefaultCoreException(e);
			}
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
	public NoticeResult notice(NoticeCommand cmd, long delay, TimeUnit delayTimeUnit) {
		return this.notice(buildBaseNoticeCommand(cmd), delay, delayTimeUnit);
	}

	@Override
	public Promise<? extends NoticeResult> asyncNotice(NoticeCommand cmd, long delay, TimeUnit delayTimeUnit) {
		return this.asyncNotice(buildBaseNoticeCommand(cmd), delay, delayTimeUnit);
	}
	
	
	public BaseNoticeResult notice(BaseNoticeCommand cmd, long delay, TimeUnit delayTimeUnit) {
		try {
			return this.asyncNotice(cmd, delay, delayTimeUnit).get();
		}catch (Throwable e) {
			while(e instanceof ExecutionException) {
				e = e.getCause();
			}
			if(e instanceof CancellationException) {
				e = new TimeoutException();
			}
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new DefaultCoreException(e);
			}
		}
	}
	
	public Promise<BaseNoticeResult> asyncNotice(final BaseNoticeCommand cmd, final long delay, final TimeUnit delayTimeUnit) {
		checkArgument(started, "%s must be call method start", getGroupName());
		checkArgument(delay > 0, "delay must > 0, cur : %s", delay);
		checkArgument(!Strings.isNullOrEmpty(cmd.getMethod()), "method cannot be null or empty");
		final DefaultTimeoutPromise<BaseNoticeResult> promise = new DefaultTimeoutPromise<BaseNoticeResult>(DEFAULT_ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		try {
			Message msg = buildDelayNoticeMessage(cmd, delay, delayTimeUnit);
			NoticeSendCallback callback = new NoticeSendCallback(promise);
			producer.send(msg, callback);
			promise.addListener(new CallerNoticeTimeoutPromiseListener(callback));
		}catch (Throwable e) {
			promise.setException(e);
		}
	
		return promise;
	}
	
	public void setBatchExecuteIntervalMillis(long batchExecuteIntervalMillis) {
		this.batchExecuteIntervalMillis = batchExecuteIntervalMillis;
	}
	
	public void setInvokable(boolean invokable) {
		this.invokable = invokable;
	}
	
	protected boolean isInvokable() {
		return invokable;
	}
	
	protected void createTopics(Set<String> topics) {
		try {
			Set<String> all = mqAdminExt.fetchAllTopicList().getTopicList();
			Set<String> newTopics = Sets.newHashSet(topics);
			newTopics.removeAll(all);
			for(String topic : newTopics) {
				mqAdminExt.createTopic(mqAdminExt.getCreateTopicKey(), topic, com.chopsticks.core.rocketmq.Const.DEFAULT_TOPIC_QUEUE_SIZE);
			}
		}catch (Throwable e) {
			throw new DefaultCoreException(e);
		}
		
	}
	
	public String buildTraceTag(String tag) {
		return String.format("%s%s", com.chopsticks.core.rocketmq.Const.TRACE_PREFIX, tag);
	}
	
	protected void checkConsumerSubscription(DefaultMQPushConsumer consumer) {
		try {
			ConsumerConnection consumerConn = mqAdminExt.examineConsumerConnectionInfo(consumer.getConsumerGroup());
			ConcurrentMap<String, SubscriptionData> oldSubscriptionTable = consumerConn.getSubscriptionTable();
			Map<String, Set<String>> oldSubscription = Maps.newHashMap();
			for(Entry<String, SubscriptionData> entry : oldSubscriptionTable.entrySet()) {
				if(entry.getKey().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
					continue;
				}
				oldSubscription.put(entry.getKey(), entry.getValue().getTagsSet());
			}
			ConcurrentMap<String, SubscriptionData> newSubscriptionTable = consumer.getDefaultMQPushConsumerImpl().getRebalanceImpl().getSubscriptionInner();
			Map<String, Set<String>> newSubscription = Maps.newHashMap();
			for(Entry<String, SubscriptionData> entry : newSubscriptionTable.entrySet()) {
				newSubscription.put(entry.getKey(), entry.getValue().getTagsSet());
			}
			
			if(!newSubscription.equals(oldSubscription)) {
				throw new DefaultCoreException(String.format("%s service not match", getGroupName())).setCode(DefaultCoreException.SUBSCRIPTION_NOT_MATCH);
			}
		}catch (Throwable e) {
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				// ignore
			}
		}
	}
}
