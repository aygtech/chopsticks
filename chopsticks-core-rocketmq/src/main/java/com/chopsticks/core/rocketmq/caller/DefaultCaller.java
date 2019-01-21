package com.chopsticks.core.rocketmq.caller;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.rocketmq.client.common.ClientErrorCode;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.store.ReadOffsetType;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.UtilAll;
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
import com.chopsticks.core.Const;
import com.chopsticks.core.caller.Caller;
import com.chopsticks.core.caller.InvokeCommand;
import com.chopsticks.core.caller.InvokeResult;
import com.chopsticks.core.caller.NoticeCommand;
import com.chopsticks.core.caller.NoticeResult;
import com.chopsticks.core.concurrent.Promise;
import com.chopsticks.core.concurrent.impl.GuavaTimeoutPromise;
import com.chopsticks.core.exception.CoreException;
import com.chopsticks.core.rocketmq.caller.impl.BatchInvokerSender;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeCommand;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.caller.impl.SingleInvokeSender;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.InvokeResponse;
import com.chopsticks.core.utils.Reflect;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets; 

/**
 * 默认发送者实现
 * @author zilong.li
 *
 */
public class DefaultCaller implements Caller {
	
	private static final Logger log = LoggerFactory.getLogger(DefaultCaller.class);
	
	private String namesrvAddr;
	
	private String groupName;
	
	private DefaultMQProducer producer;
	
	private DefaultMQPushConsumer callerInvokeConsumer;
	
	private volatile boolean started;
	
	protected static final long DEFAULT_SYNC_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30);
	
	protected static final long DEFAULT_ASYNC_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);
	
	private static final MessageQueueSelector DEFAULT_MESSAGE_QUEUE_SELECTOR = new OrderedMessageQueueSelector();
	
	private long batchExecuteIntervalMillis = TimeUnit.MILLISECONDS.toMillis(100L);
	
	private InvokeSender invokeSender;
	
	private boolean invokable = true;
	
	private DefaultMQAdminExt mqAdminExt;
	
	private static final Cache</*topic + tag*/String, /*consumer exist*/Boolean> INVOKE_TOPIC_TAG_MONITOR = CacheBuilder.newBuilder().expireAfterWrite(DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).build();
	private static final Cache</*topic*/String, Set<ConsumerConnection>> INVOKE_TOPIC_MONITOR = CacheBuilder.newBuilder().expireAfterWrite(DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).build();
	
	/**
	 *  <msgid, timeoutGuavaPromise>
	 */
	private Map<String, GuavaTimeoutPromise<BaseInvokeResult>> callerInvokePromiseMap;
	
	public DefaultCaller(String groupName) {
		checkArgument(!isNullOrEmpty(groupName), "groupName cannot be null or empty");
		this.groupName = groupName;
	}
	
	private void testCaller() {
		try {
			String topic = buildRespTopic();
			InvokeResponse resp = new InvokeResponse("testCaller", com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow(), com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow(), new byte[0]);
			SendResult ret = producer.send(new Message(topic, com.chopsticks.core.rocketmq.Const.INVOCE_RESP_TAG_SUFFIX, JSON.toJSONBytes(resp)));
			if(ret.getSendStatus() != SendStatus.SEND_OK) {
				throw new RuntimeException(ret.getSendStatus().name());
			}
		}catch (Throwable e) {
			if(e instanceof MQClientException) {
				MQClientException se = (MQClientException)e;
				if(se.getResponseCode() == ClientErrorCode.NOT_FOUND_TOPIC_EXCEPTION){
					e = new DefaultCoreException("namesrv connection error").setCode(DefaultCoreException.TEST_CALLER_NAME_SERVER_CONNECTION_ERROR);
				}else if(se.getResponseCode() == ClientErrorCode.NO_NAME_SERVER_EXCEPTION) {
					e = new DefaultCoreException("namesrv ip undefined").setCode(DefaultCoreException.TEST_CALLER_NO_NAME_SERVER_ERROR);
				}
			}
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public synchronized void start() {
		if(!started) {
			try {
				callerInvokePromiseMap = new ConcurrentHashMap<String, GuavaTimeoutPromise<BaseInvokeResult>>();
				producer = buildAndStartProducer();
				invokeSender = buildInvokeSender(producer, batchExecuteIntervalMillis);
				mqAdminExt = buildAdminExt();
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
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}
	}
	


	private DefaultMQAdminExt buildAdminExt() {
		if(isInvokable()) {
			DefaultMQAdminExt mqAdminExt = new DefaultMQAdminExt(getGroupName() + com.chopsticks.core.rocketmq.Const.INVOKE_ADMIN_EXT_SUFFIX);
			try {
				mqAdminExt.setNamesrvAddr(namesrvAddr);
				mqAdminExt.start();
			}catch (Throwable e) {
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
			return mqAdminExt;
		}else {
			return null;
		}
		
	}

	private InvokeSender buildInvokeSender(DefaultMQProducer producer, long batchExecuteIntervalMillis) {
		InvokeSender invokeSender = null;
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
			callerInvokeConsumer.setMessageModel(MessageModel.BROADCASTING);
			callerInvokeConsumer.setConsumeMessageBatchMaxSize(10);
			callerInvokeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			callerInvokeConsumer.registerMessageListener(new CallerInvokeListener(callerInvokePromiseMap));
			callerInvokeConsumer.setPullThresholdSizeForTopic(10);
			try {
				String topic = buildRespTopic();
				callerInvokeConsumer.subscribe(topic, com.chopsticks.core.rocketmq.Const.ALL_TAGS);
				callerInvokeConsumer.start();
				callerInvokeConsumer = com.chopsticks.core.rocketmq.Const.buildConsumer(callerInvokeConsumer);
				ThreadPoolExecutor consumeExecutor = Reflect.on(callerInvokeConsumer)
														    .field("defaultMQPushConsumerImpl")
														    .field("consumeMessageService")
														    .field("consumeExecutor")
														    .get();
				Stopwatch watch = Stopwatch.createStarted();
				do {
					if(watch.elapsed(TimeUnit.MILLISECONDS) > DEFAULT_SYNC_TIMEOUT_MILLIS) {
						throw new DefaultCoreException("caller connection server timeout, pls try again.").setCode(DefaultCoreException.INVOKE_CONSUMER_START_TIMEOUT);
					}
					if(consumeExecutor.getTaskCount() > 0) {
						break;
					}else {
						testCaller();
						callerInvokeConsumer.fetchSubscribeMessageQueues(buildRespTopic());
						TimeUnit.SECONDS.sleep(1L);
					}
				}while(true);
			}catch (Throwable e) {
				if(callerInvokeConsumer != null) {
					callerInvokeConsumer.shutdown();
				}
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}
		return callerInvokeConsumer;
	}

	private DefaultMQProducer buildAndStartProducer() {
		DefaultMQProducer producer = null;
		producer = new DefaultMQProducer(com.chopsticks.core.rocketmq.Const.PRODUCER_PREFIX + getGroupName());
		producer.setNamesrvAddr(namesrvAddr);
		producer.setRetryAnotherBrokerWhenNotStoreOK(true);
		producer.setDefaultTopicQueueNums(com.chopsticks.core.rocketmq.Const.DEFAULT_TOPIC_QUEUE_SIZE);
		try {
			producer.start();
			return producer;
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	public BaseInvokeResult invoke(BaseInvokeCommand cmd) {
		return this.invoke(cmd, DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
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
		return this.asyncInvoke(cmd, DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}
	
	public Promise<BaseInvokeResult> asyncInvoke(final BaseInvokeCommand cmd, final long timeout, final TimeUnit timeoutUnit) {
		checkArgument(started, "must be call method start");
		checkArgument(invokable, "must be support invokable");
		final GuavaTimeoutPromise<BaseInvokeResult> promise = new GuavaTimeoutPromise<BaseInvokeResult>(timeout, timeoutUnit);
		try {
			InvokeRequest req = buildInvokeRequest(cmd, timeout, timeoutUnit);
			callerInvokePromiseMap.put(req.getReqId(), promise);
			final Message msg = buildInvokeMessage(req, cmd, timeout, timeoutUnit);
			if(!INVOKE_TOPIC_TAG_MONITOR.get(msg.getTopic() + msg.getTags(), new Callable<Boolean>() {
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
						try {
							for(Entry<String, SubscriptionData> entry : consumerConn.getSubscriptionTable().entrySet()) {
								if(entry.getKey().equals(msg.getTopic()) && (entry.getValue().getTagsSet().contains(msg.getTags()) || entry.getValue().getTagsSet().contains(com.chopsticks.core.rocketmq.Const.ALL_TAGS))) {
									examineConsumerConnectionInfo = true;
								}
							}
						}catch (Throwable e) {
						
						}
					}
					return examineConsumerConnectionInfo;
				}
			})) {
				throw new DefaultCoreException(String.format("%s.%s cannot found executor", cmd.getTopic(), cmd.getTag())).setCode(DefaultCoreException.INVOKE_EXECUTOR_NOT_FOUND);
			}
			Message compressMsg = compressInvokeMsgBody(msg);
			invokeSender.send(compressMsg, promise);
			promise.addListener(new CallerInvokeTimoutPromiseListener(callerInvokePromiseMap, req));
		} catch (Throwable e) {
			promise.setException(e);
		}
	
		return promise;
	}
	private Message compressInvokeMsgBody(Message msg) {
		try {
			InvokeRequest req = JSON.parseObject(msg.getUserProperty(com.chopsticks.core.rocketmq.Const.INVOKE_REQUEST_KEY), InvokeRequest.class);
			req.setCompress(true);
			int level = Reflect.on(producer).field("defaultMQProducerImpl").field("zipCompressLevel").get();
			byte[] body = UtilAll.compress(msg.getBody(), level);
			msg.setBody(body);
			msg.putUserProperty(com.chopsticks.core.rocketmq.Const.INVOKE_REQUEST_KEY, JSON.toJSONString(req));
		}catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
		return msg;
	}
	
	public BaseNoticeResult notice(BaseNoticeCommand cmd) {
		try {
			return this.asyncNotice(cmd).get();
		}catch (Throwable e) {
			if(e instanceof ExecutionException) {
				e = e.getCause();
			}
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
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
		checkArgument(started, "must be call method start");
		final GuavaTimeoutPromise<BaseNoticeResult> promise = new GuavaTimeoutPromise<BaseNoticeResult>(DEFAULT_ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
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
		msg.setKeys(Joiner.on(" ").join(cmd.getTraceNos()));
		return msg;
	}

	public Promise<BaseNoticeResult> asyncNotice(final BaseNoticeCommand cmd, final Object orderKey) {
		checkArgument(started, "must be call method start");
		checkArgument(orderKey != null, "orderKey cannot be null");
		final GuavaTimeoutPromise<BaseNoticeResult> promise = new GuavaTimeoutPromise<BaseNoticeResult>(DEFAULT_ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
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
	
	protected Message buildInvokeMessage(InvokeRequest req, BaseInvokeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		Message msg = new Message(buildInvokeTopic(cmd.getTopic()), cmd.getTag(), cmd.getBody());
		msg.putUserProperty(com.chopsticks.core.rocketmq.Const.INVOKE_REQUEST_KEY, JSON.toJSONString(req));
		Set<String> traceNos = Sets.newHashSet(cmd.getTraceNos());
		traceNos.add(req.getReqId());
		traceNos.add(buildTraceTag(cmd.getTag()));
		msg.setKeys(Joiner.on(" ").join(traceNos));
		return msg;
	}
	
	private DelayNoticeRequest buildDelayNoticeRequest(BaseNoticeCommand cmd, long timeout, TimeUnit timeoutUnit) {
		DelayNoticeRequest req = new DelayNoticeRequest();
		req.setReqTime(com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow());
		req.setInvokeTime(com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow());
		req.setExecuteTime(req.getInvokeTime() + timeoutUnit.toMillis(timeout));
		req.setExtParams(cmd.getExtParams());
		req.setTraceNos(cmd.getTraceNos());
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
		req.setReqId(UUID.randomUUID().toString());
		req.setReqTime(com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow());
		req.setDeadline(req.getReqTime() + timeoutUnit.toMillis(timeout));
		req.setRespTopic(buildRespTopic());
		req.setRespTag(cmd.getTag() + com.chopsticks.core.rocketmq.Const.INVOCE_RESP_TAG_SUFFIX);
		req.setRespCompress(true);
		req.setExtParams(cmd.getExtParams());
		req.setTraceNos(cmd.getTraceNos());
		return req;
	}

	private String buildRespTopic() {
		return getGroupName() + com.chopsticks.core.rocketmq.Const.INVOCE_RESP_TOPIC_SUFFIX;
	}
	
	private Message buildDelayNoticeMessage(BaseNoticeCommand cmd, Long delay, TimeUnit delayTimeUnit) {
		Message msg = new Message(buildDelayNoticeTopic(cmd.getTopic()), cmd.getTag(), cmd.getBody());
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
				Optional<Entry<Long, Integer>> level = com.chopsticks.core.rocketmq.Const.getDelayLevel(TimeUnit.SECONDS.toMillis(1L));
				if(level.isPresent()) {
					log.warn("delay notice is short : {}, change to : {}", delay, level.get().getKey());
					msg.setDelayTimeLevel(level.get().getValue());
				}
			}
		}
		Set<String> traceNo = Sets.newHashSet(cmd.getTraceNos());
		traceNo.add(buildTraceTag(cmd.getTag()));
		msg.setKeys(Joiner.on(" ").join(cmd.getTraceNos()));
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
		msg.setKeys(Joiner.on(" ").join(cmd.getTraceNos()));
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
		checkArgument(delay > 0, "delay must > 0, cur : %s", delay);
		final GuavaTimeoutPromise<BaseNoticeResult> promise = new GuavaTimeoutPromise<BaseNoticeResult>(DEFAULT_ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
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
	
	protected void setInvokable(boolean invokable) {
		this.invokable = invokable;
	}
	
	protected boolean isInvokable() {
		return invokable;
	}
	
	protected void resetNow(final MessageModel messageModel, final String instanceName, final String consumerGroup,
			final String topic) throws Throwable {

		DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(consumerGroup);
		consumer.setInstanceName(instanceName);
		consumer.setMessageModel(messageModel);
		consumer.start();

		Set<MessageQueue> mqs = null;
		try {
			mqs = consumer.fetchSubscribeMessageQueues(topic);
			if (mqs != null && !mqs.isEmpty()) {
				TreeSet<MessageQueue> mqsNew = new TreeSet<MessageQueue>(mqs);
				for (MessageQueue mq : mqsNew) {
					long offset = consumer.maxOffset(mq);
					if (offset >= 0) {
						consumer.updateConsumeOffset(mq, offset);
					}
				}
			}
		}catch (Throwable e) {
			if(!e.getMessage().contains(com.chopsticks.core.rocketmq.Const.ERROR_MSG_CAN_NOT_FIND_MESSAGE_QUEUE)) {
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}finally {
			if (mqs != null) {
				consumer.getDefaultMQPullConsumerImpl().getOffsetStore().persistAll(mqs);
			}
			consumer.shutdown();
		}
	}
	
	protected void realConsumeFromLastOffset(final MessageModel messageModel, final String instanceName, final String consumerGroup,
			final String topic) throws Throwable{
		DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(consumerGroup);
		consumer.setInstanceName(instanceName);
		consumer.setMessageModel(messageModel);
		consumer.start();
		Set<MessageQueue> mqs = null;
		try {
			mqs = consumer.fetchSubscribeMessageQueues(topic);
			if (mqs != null && !mqs.isEmpty()) {
				TreeSet<MessageQueue> mqsNew = new TreeSet<MessageQueue>(mqs);
				for (MessageQueue mq : mqsNew) {
					long offset = consumer.getOffsetStore().readOffset(mq, ReadOffsetType.READ_FROM_STORE);
					long maxOffset = consumer.maxOffset(mq);
					if (maxOffset > 0 && offset <= 0) {
						consumer.updateConsumeOffset(mq, maxOffset);
					}
				}
			}
		}catch (Throwable e) {
			if(!e.getMessage().contains(com.chopsticks.core.rocketmq.Const.ERROR_MSG_CAN_NOT_FIND_MESSAGE_QUEUE)) {
				throw new DefaultCoreException(e).setCode(CoreException.UNKNOW_EXCEPTION);
			}
		}finally {
			if (mqs != null) {
				consumer.getDefaultMQPullConsumerImpl().getOffsetStore().persistAll(mqs);
			}
			consumer.shutdown();
		}
	}
	
	public String buildTraceTag(String tag) {
		return String.format("_%s_", tag);
	}
}
