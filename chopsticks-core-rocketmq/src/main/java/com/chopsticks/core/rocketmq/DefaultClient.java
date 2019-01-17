package com.chopsticks.core.rocketmq;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.common.ClientErrorCode;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.Client;
import com.chopsticks.core.handler.Handler;
import com.chopsticks.core.rocketmq.caller.BaseInvokeCommand;
import com.chopsticks.core.rocketmq.caller.DefaultCaller;
import com.chopsticks.core.rocketmq.caller.InvokeRequest;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeCommand;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.chopsticks.core.rocketmq.handler.HandlerDelayNoticeListener;
import com.chopsticks.core.rocketmq.handler.HandlerInvokeListener;
import com.chopsticks.core.rocketmq.handler.HandlerNoticeListener;
import com.chopsticks.core.rocketmq.handler.HandlerOrderedNoticeListener;
import com.chopsticks.core.rocketmq.handler.impl.BaseHandlerWapper;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets; 

/**
 * 默认客户端实现
 * @author zilong.li
 *
 */
public class DefaultClient extends DefaultCaller implements Client{
	
	private static final Logger log = LoggerFactory.getLogger(DefaultClient.class);
	
	private volatile boolean started;
	
	private Set<BaseHandler> handlers;
	
	/**
	 *  <topic,Set<tag>>
	 */
	private Multimap<String, String> topicTags;
	
	/**
	 *  <topic + tag, baseHandler>
	 */
	private Map<String, BaseHandler> topicTagHandlers;
	
	private DefaultMQPushConsumer invokeConsumer;
	private DefaultMQPushConsumer noticeConsumer;
	private DefaultMQPushConsumer delayNoticeConsumer;
	private DefaultMQPushConsumer orderedNoticeConsumer;
	
	private boolean invokeExecutable = true;
	private boolean noticeExecutable = true;
	private boolean delayNoticeExecutable = true;
	private boolean orderedNoticeExecutable = true;
	
	private int invokeExecutableNum = 15;
	private int noticeExecutableNum = 10;
	private int delayNoticeExecutableNum = 10;
	private int orderedNoticeExecutableNum = 5;
	
	private int noticeExcecutableRetryCount = Integer.MAX_VALUE;
	private int delayNoticeExecutableRetryCount = Integer.MAX_VALUE;
	private int orderedNoticeExecutableRetryCount = Integer.MAX_VALUE;
	
	private long invokeMaxExecutableTime = TimeUnit.MINUTES.toMinutes(15);
	private long noticeMaxExecutableTime = TimeUnit.MINUTES.toMinutes(15);
	private long delayNoticeMaxExecutableTime = TimeUnit.MINUTES.toMinutes(15);
	private long orderedNoticeMaxExecutableTime = TimeUnit.MINUTES.toMinutes(15);
	
	public DefaultClient(String groupName) {
		super(groupName);
	}
	
	@Override
	public void register(Set<? extends Handler> handlers) {
		this.handlers = buildBaseHandlers(handlers);
	}
	
	private Set<BaseHandler> buildBaseHandlers(Set<? extends Handler> handlers) {
		Set<BaseHandler> baseHandlers = Sets.newHashSet();
		for(Handler handler : handlers) {
			if(handler instanceof BaseHandler) {
				baseHandlers.add((BaseHandler)handler);
				baseHandlers.add(new BaseHandlerWapper(handler, ((BaseHandler) handler).getTopic() , Const.buildCustomTag(getGroupName(), ((BaseHandler) handler).getTag())));
			}else {
				baseHandlers.add(new BaseHandlerWapper(handler, Const.DEFAULT_TOPIC, handler.getMethod()));
				baseHandlers.add(new BaseHandlerWapper(handler, Const.DEFAULT_TOPIC, Const.buildCustomTag(getGroupName(), handler.getMethod())));
			}
		}
		return baseHandlers;
	}

	@Override
	public synchronized void shutdown() {
		super.shutdown();
		if(invokeConsumer != null) {
			invokeConsumer.shutdown();
			invokeConsumer = null;
		}
		if(delayNoticeConsumer != null) {
			delayNoticeConsumer.shutdown();
			delayNoticeConsumer = null;
		}
		if(noticeConsumer != null){
			noticeConsumer.shutdown();
			noticeConsumer = null;
		}
		if(orderedNoticeConsumer != null) {
			orderedNoticeConsumer.shutdown();
			orderedNoticeConsumer = null;
		}
		started = false;
	}
	
	@Override
	public synchronized void start() {
		if(!started) {
			super.start();
			buildTopicTagsAndTopicTagHandlers();
			if(!topicTags.isEmpty()) {
				addTest();
				try {
					invokeConsumer = buildAndStartInvokeCosumer();
					delayNoticeConsumer = buildAndStartDelayNoticeCosumer();
					noticeConsumer = buildAndStartNoticeCosumer();
					orderedNoticeConsumer = buildAndStartOrderedNoticeCosumer();
				}catch (Throwable e) {
					if(invokeConsumer != null) {
						invokeConsumer.shutdown();
					}
					if(delayNoticeConsumer != null) {
						delayNoticeConsumer.shutdown();
					}
					if(noticeConsumer != null) {
						noticeConsumer.shutdown();
					}
					if(orderedNoticeConsumer != null) {
						orderedNoticeConsumer.shutdown();
					}
					Throwables.throwIfUnchecked(e);
					throw new RuntimeException(e);
				}
			}
			started = true;
		}
	}
	
	private void addTest() {
		for(String topic : topicTags.keySet()) {
			EmptyHandler handler = new EmptyHandler(topic, Const.buildTestTag(getGroupName()));
			topicTags.put(handler.getTopic(), handler.getTag());
			topicTagHandlers.put(handler.getTopic() + handler.getTag(), handler);
		}
	}

	private void testInvokeClient(DefaultMQPushConsumer invokeConsumer) {
		try {
			for(String topic : topicTags.keySet()) {
				BaseInvokeCommand cmd = new DefaultInvokeCommand(topic, Const.buildTestTag(getGroupName()), Const.CLIENT_TEST_TAG.getBytes());
				InvokeRequest req = buildInvokeRequest(cmd, DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
				req.setRespTopic(null);
				Message msg = buildInvokeMessage(req, cmd, DEFAULT_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
				SendResult ret = getProducer().send(msg);
				if(ret.getSendStatus() != SendStatus.SEND_OK) {
					throw new RuntimeException(ret.getSendStatus().name());
				}
				invokeConsumer.fetchSubscribeMessageQueues(buildInvokeTopic(topic));
			}
		}catch (Throwable e) {
			if(e instanceof MQClientException) {
				MQClientException se = (MQClientException)e;
				if(se.getResponseCode() == ClientErrorCode.NOT_FOUND_TOPIC_EXCEPTION){
					e = new DefaultCoreException("namesrv connection error").setCode(DefaultCoreException.TEST_INVOKE_CLIENT_NAME_SERVER_CONNECTION_ERROR);
				}else if(se.getResponseCode() == ClientErrorCode.NO_NAME_SERVER_EXCEPTION) {
					e = new DefaultCoreException("namesrv ip undefined").setCode(DefaultCoreException.TEST_INVOKE_CLIENT_NO_NAME_SERVER_ERROR);
				}
			}
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	private void testDelayNoticeClient(DefaultMQPushConsumer delayNoticeConsumer) {
		try {
			for(String topic : topicTags.keySet()) {
				this.notice(new DefaultNoticeCommand(topic, Const.buildTestTag(getGroupName()), Const.CLIENT_TEST_TAG.getBytes()), TimeUnit.SECONDS.toMillis(1L), TimeUnit.MILLISECONDS);
				delayNoticeConsumer.fetchSubscribeMessageQueues(buildDelayNoticeTopic(topic));
			}
		}catch (Throwable e) {
			if(e instanceof MQClientException) {
				MQClientException se = (MQClientException)e;
				if(se.getResponseCode() == ClientErrorCode.NOT_FOUND_TOPIC_EXCEPTION){
					e = new DefaultCoreException("namesrv connection error").setCode(DefaultCoreException.TEST_DELAY_NOTICE_CLIENT_BROKER_CONNECTION_ERROR);
				}else if(se.getResponseCode() == ClientErrorCode.NO_NAME_SERVER_EXCEPTION) {
					e = new DefaultCoreException("namesrv ip undefined").setCode(DefaultCoreException.TEST_DELAY_NOTICE_CLIENT_NAME_SERVER_CONNECTION_ERROR);
				}else {
					e = new DefaultCoreException(e).setCode(DefaultCoreException.TEST_DELAY_NOTICE_CLIENT_ERROR);
				}
			}else {
				e = new DefaultCoreException(e).setCode(DefaultCoreException.TEST_DELAY_NOTICE_CLIENT_ERROR);
			}
			Throwables.throwIfUnchecked(e);
		}
	}
	
	private void testNoticeClient(DefaultMQPushConsumer noticeConsumer) {
		try {
			for(String topic : topicTags.keySet()) {
				this.notice(new DefaultNoticeCommand(topic, Const.buildTestTag(getGroupName()), Const.CLIENT_TEST_TAG.getBytes()));
				noticeConsumer.fetchSubscribeMessageQueues(buildNoticeTopic(topic));
			}
		}catch (Throwable e) {
			if(e instanceof MQClientException) {
				MQClientException se = (MQClientException)e;
				if(se.getResponseCode() == ClientErrorCode.NOT_FOUND_TOPIC_EXCEPTION){
					e = new DefaultCoreException("namesrv connection error").setCode(DefaultCoreException.TEST_NOTICE_CLIENT_BROKER_CONNECTION_ERROR);
				}else if(se.getResponseCode() == ClientErrorCode.NO_NAME_SERVER_EXCEPTION) {
					e = new DefaultCoreException("namesrv ip undefined").setCode(DefaultCoreException.TEST_NOTICE_CLIENT_NAME_SERVER_CONNECTION_ERROR);
				}else {
					e = new DefaultCoreException(e).setCode(DefaultCoreException.TEST_NOTICE_CLIENT_ERROR);
				}
			}else {
				e = new DefaultCoreException(e).setCode(DefaultCoreException.TEST_NOTICE_CLIENT_ERROR);
			}
			Throwables.throwIfUnchecked(e);
		}
	}
	
	private void testOrderedNoticeClient(DefaultMQPushConsumer orderedNoticeConsumer) {
		try {
			for(String topic : topicTags.keySet()) {
				this.notice(new DefaultNoticeCommand(topic, Const.buildTestTag(getGroupName()), Const.CLIENT_TEST_TAG.getBytes()), Const.CLIENT_TEST_TAG);
				orderedNoticeConsumer.fetchSubscribeMessageQueues(buildOrderedNoticeTopic(topic));
			}
		}catch (Throwable e) {
			if(e instanceof MQClientException) {
				MQClientException se = (MQClientException)e;
				if(se.getResponseCode() == ClientErrorCode.NOT_FOUND_TOPIC_EXCEPTION){
					e = new DefaultCoreException("namesrv connection error").setCode(DefaultCoreException.TEST_ORDERED_NOTICE_CLIENT_BROKER_CONNECTION_ERROR);
				}else if(se.getResponseCode() == ClientErrorCode.NO_NAME_SERVER_EXCEPTION) {
					e = new DefaultCoreException("namesrv ip undefined").setCode(DefaultCoreException.TEST_ORDERED_NOTICE_CLIENT_NAME_SERVER_CONNECTION_ERROR);
				}else {
					e = new DefaultCoreException(e).setCode(DefaultCoreException.TEST_ORDERED_NOTICE_CLIENT_ERROR);
				}
			}else {
				e = new DefaultCoreException(e).setCode(DefaultCoreException.TEST_ORDERED_NOTICE_CLIENT_ERROR);
			}
			Throwables.throwIfUnchecked(e);
		}
	}

	private void buildTopicTagsAndTopicTagHandlers() {
		topicTags = MultimapBuilder.hashKeys().hashSetValues().build();
		topicTagHandlers = Maps.newHashMap();
		if(handlers != null) {
			for(BaseHandler handler : handlers) {
				if(Strings.isNullOrEmpty(handler.getTopic()) 
				|| Strings.isNullOrEmpty(handler.getTag())) {
					log.warn("handler topic and tag cannot be null or empty : {}", handler);
					continue;
				}
				topicTags.put(buildSuccessTopic(handler.getTopic()), handler.getTag());
				topicTagHandlers.put(buildSuccessTopic(handler.getTopic()) + handler.getTag(), handler);
			}
		}
	}
	
	
	private DefaultMQPushConsumer buildAndStartOrderedNoticeCosumer() {
		DefaultMQPushConsumer orderedNoticeConsumer = null;
		if(isOrderedNoticeExecutable()) {
			String groupName = Const.CONSUMER_PREFIX + getGroupName() + Const.ORDERED_NOTICE_CONSUMER_SUFFIX;
			orderedNoticeConsumer = new DefaultMQPushConsumer(groupName);
			orderedNoticeConsumer.setNamesrvAddr(getNamesrvAddr());
			orderedNoticeConsumer.setConsumeThreadMin(getOrderedNoticeExecutableNum());
			orderedNoticeConsumer.setConsumeThreadMax(getOrderedNoticeExecutableNum());
			orderedNoticeConsumer.setMessageModel(MessageModel.CLUSTERING);
			orderedNoticeConsumer.setMaxReconsumeTimes(getOrderedNoticeExecutableRetryCount());
			orderedNoticeConsumer.setConsumeMessageBatchMaxSize(1);
			orderedNoticeConsumer.setConsumeTimeout(getOrderedNoticeMaxExecutableTime());
			orderedNoticeConsumer.setSuspendCurrentQueueTimeMillis(TimeUnit.SECONDS.toMillis(5L));
			orderedNoticeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			orderedNoticeConsumer.registerMessageListener(new HandlerOrderedNoticeListener(this, orderedNoticeConsumer, topicTags, topicTagHandlers));
			orderedNoticeConsumer.setPullThresholdSizeForTopic(10);
			try {
				for(Entry<String, Collection<String>> entry: topicTags.asMap().entrySet()) {
					String topic = entry.getKey();
					Collection<String> tags = entry.getValue();
					topic = buildOrderedNoticeTopic(topic);
					realConsumeFromLastOffset(orderedNoticeConsumer.getMessageModel(), orderedNoticeConsumer.getInstanceName(), orderedNoticeConsumer.getConsumerGroup(), topic);
					if(tags.contains(Const.ALL_TAGS)) {
						orderedNoticeConsumer.subscribe(topic, Const.ALL_TAGS);
					}else {
						Set<String> newTags = Sets.newHashSet(tags);
						for(Iterator<String> iter = newTags.iterator(); iter.hasNext();) {
							String tag = Const.getOriginTag(getGroupName(), iter.next());
							if(!topicTagHandlers.get(buildSuccessTopic(entry.getKey()) + tag).isSupportOrderedNotice()) {
								iter.remove();
							}
						}
						orderedNoticeConsumer.subscribe(topic, Joiner.on("||").join(tags));
					}
				}
				orderedNoticeConsumer.start();
				orderedNoticeConsumer = Const.buildConsumer(orderedNoticeConsumer);
				testOrderedNoticeClient(orderedNoticeConsumer);
			}catch (Throwable e) {
				if(orderedNoticeConsumer != null) {
					orderedNoticeConsumer.shutdown();
				}
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}
		return orderedNoticeConsumer;
	}
	
	private DefaultMQPushConsumer buildAndStartNoticeCosumer() {
		DefaultMQPushConsumer noticeConsumer = null;
		if(isNoticeExecutable()) {
			String groupName = Const.CONSUMER_PREFIX + getGroupName() + Const.NOTICE_CONSUMER_SUFFIX;
			noticeConsumer = new DefaultMQPushConsumer(groupName);
			noticeConsumer.setNamesrvAddr(getNamesrvAddr());
			noticeConsumer.setConsumeThreadMin(getNoticeExecutableNum());
			noticeConsumer.setConsumeThreadMax(getNoticeExecutableNum());
			noticeConsumer.setMessageModel(MessageModel.CLUSTERING);
			noticeConsumer.setMaxReconsumeTimes(getNoticeExcecutableRetryCount());
			noticeConsumer.setConsumeMessageBatchMaxSize(1);
			noticeConsumer.setConsumeTimeout(getNoticeMaxExecutableTime());
			noticeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			noticeConsumer.registerMessageListener(new HandlerNoticeListener(this, noticeConsumer, topicTags, topicTagHandlers));
			noticeConsumer.setPullThresholdSizeForTopic(10);
			try {
				for(Entry<String, Collection<String>> entry: topicTags.asMap().entrySet()) {
					String topic = entry.getKey();
					Collection<String> tags = entry.getValue();
					topic = buildNoticeTopic(topic);
					realConsumeFromLastOffset(noticeConsumer.getMessageModel(), noticeConsumer.getInstanceName(), noticeConsumer.getConsumerGroup(), topic);
					if(tags.contains(Const.ALL_TAGS)) {
						noticeConsumer.subscribe(topic, Const.ALL_TAGS);
					}else {
						Set<String> newTags = Sets.newHashSet(tags);
						for(Iterator<String> iter = newTags.iterator(); iter.hasNext();) {
							String tag = Const.getOriginTag(getGroupName(), iter.next());
							if(!topicTagHandlers.get(buildSuccessTopic(entry.getKey()) + tag).isSupportNotice()) {
								iter.remove();
							}
						}
						noticeConsumer.subscribe(topic, Joiner.on("||").join(tags));
					}
				}
				noticeConsumer.start();
				noticeConsumer = Const.buildConsumer(noticeConsumer);
				testNoticeClient(noticeConsumer);
			}catch (Throwable e) {
				if(noticeConsumer != null) {
					noticeConsumer.shutdown();
				}
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}
		return noticeConsumer;
	}
	
	private DefaultMQPushConsumer buildAndStartDelayNoticeCosumer() {
		DefaultMQPushConsumer delayNoticeConsumer = null;
		if(isDelayNoticeExecutable()) {
			String groupName = Const.CONSUMER_PREFIX + getGroupName() + Const.DELAY_NOTICE_CONSUMER_SUFFIX;
			delayNoticeConsumer = new DefaultMQPushConsumer(groupName);
			delayNoticeConsumer.setNamesrvAddr(getNamesrvAddr());
			delayNoticeConsumer.setConsumeThreadMin(getDelayNoticeExecutableNum());
			delayNoticeConsumer.setConsumeThreadMax(getDelayNoticeExecutableNum());
			delayNoticeConsumer.setMessageModel(MessageModel.CLUSTERING);
			delayNoticeConsumer.setMaxReconsumeTimes(getDelayNoticeExecutableRetryCount());
			delayNoticeConsumer.setConsumeMessageBatchMaxSize(1);
			delayNoticeConsumer.setConsumeTimeout(getDelayNoticeMaxExecutableTime());
			delayNoticeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			delayNoticeConsumer.registerMessageListener(new HandlerDelayNoticeListener(this, delayNoticeConsumer, topicTags, topicTagHandlers));
			delayNoticeConsumer.setPullThresholdSizeForTopic(10);
			try {
				for(Entry<String, Collection<String>> entry: topicTags.asMap().entrySet()) {
					String topic = entry.getKey();
					Collection<String> tags = entry.getValue();
					topic = buildDelayNoticeTopic(topic);
					realConsumeFromLastOffset(delayNoticeConsumer.getMessageModel(), delayNoticeConsumer.getInstanceName(), delayNoticeConsumer.getConsumerGroup(), topic);
					if(tags.contains(Const.ALL_TAGS)) {
						delayNoticeConsumer.subscribe(topic, Const.ALL_TAGS);
					}else {
						Set<String> newTags = Sets.newHashSet(tags);
						for(Iterator<String> iter = newTags.iterator(); iter.hasNext();) {
							String tag = Const.getOriginTag(getGroupName(), iter.next());
							if(!topicTagHandlers.get(buildSuccessTopic(entry.getKey()) + tag).isSupportDelayNotice()) {
								iter.remove();
							}
						}
						delayNoticeConsumer.subscribe(topic, Joiner.on("||").join(tags));
					}
				}
				delayNoticeConsumer.start();
				delayNoticeConsumer = Const.buildConsumer(delayNoticeConsumer);
				testDelayNoticeClient(delayNoticeConsumer);
			}catch (Throwable e) {
				if(delayNoticeConsumer != null) {
					delayNoticeConsumer.shutdown();
				}
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}
		return delayNoticeConsumer;
	}
	
	private DefaultMQPushConsumer buildAndStartInvokeCosumer() {
		DefaultMQPushConsumer invokeConsumer = null;
		if(isInvokeExecutable()) {
			String groupName = Const.CONSUMER_PREFIX + getGroupName() + Const.INVOKE_CONSUMER_SUFFIX;
			invokeConsumer = new DefaultMQPushConsumer(groupName);
			invokeConsumer.setNamesrvAddr(getNamesrvAddr());
			invokeConsumer.setConsumeThreadMin(getInvokeExecutableNum());
			invokeConsumer.setConsumeThreadMax(getInvokeExecutableNum());
			invokeConsumer.setMessageModel(MessageModel.CLUSTERING);
			invokeConsumer.setMaxReconsumeTimes(0);
			invokeConsumer.setConsumeTimeout(getInvokeMaxExecutableTime());
			invokeConsumer.setConsumeMessageBatchMaxSize(1);
			invokeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			invokeConsumer.registerMessageListener(new HandlerInvokeListener(this, topicTagHandlers));
			invokeConsumer.setPullThresholdSizeForTopic(10);
			try {
				for(Entry<String, Collection<String>> entry: topicTags.asMap().entrySet()) {
					String topic = entry.getKey();
					Collection<String> tags = entry.getValue();
					topic = buildInvokeTopic(topic);
					resetNow(invokeConsumer.getMessageModel(), invokeConsumer.getInstanceName(), invokeConsumer.getConsumerGroup(), topic);
					if(tags.contains(Const.ALL_TAGS)) {
						invokeConsumer.subscribe(topic, Const.ALL_TAGS);
					}else {
						Set<String> newTags = Sets.newHashSet(tags);
						for(Iterator<String> iter = newTags.iterator(); iter.hasNext();) {
							String tag = Const.getOriginTag(getGroupName(), iter.next());
							if(!topicTagHandlers.get(buildSuccessTopic(entry.getKey()) + tag).isSupportInvoke()) {
								iter.remove();
							}
						}
						invokeConsumer.subscribe(topic, Joiner.on("||").join(newTags));
					}
				}
				invokeConsumer.start();
				invokeConsumer = Const.buildConsumer(invokeConsumer);
				testInvokeClient(invokeConsumer);
			}catch (Throwable e) {
				if(invokeConsumer != null) {
					invokeConsumer.shutdown();
				}
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}
		return invokeConsumer;
	}
	
	protected void setInvokeExecutable(boolean invokeExecutable) {
		this.invokeExecutable = invokeExecutable;
	}
	protected boolean isInvokeExecutable() {
		return invokeExecutable;
	}
	protected void setNoticeExecutable(boolean noticeExecutable) {
		this.noticeExecutable = noticeExecutable;
	}
	protected boolean isNoticeExecutable() {
		return noticeExecutable;
	}
	protected void setOrderedNoticeExecutable(boolean orderedNoticeExecutable) {
		this.orderedNoticeExecutable = orderedNoticeExecutable;
	}
	protected boolean isOrderedNoticeExecutable() {
		return orderedNoticeExecutable;
	}
	protected int getInvokeExecutableNum() {
		return invokeExecutableNum;
	}
	protected void setInvokeExecutableNum(int invokeExecutableNum) {
		this.invokeExecutableNum = invokeExecutableNum;
	}
	protected int getNoticeExecutableNum() {
		return noticeExecutableNum;
	}
	protected void setNoticeExecutableNum(int noticeExecutableNum) {
		this.noticeExecutableNum = noticeExecutableNum;
	}
	protected int getOrderedNoticeExecutableNum() {
		return orderedNoticeExecutableNum;
	}
	protected void setOrderedNoticeExecutableNum(int orderedNoticeExecutableNum) {
		this.orderedNoticeExecutableNum = orderedNoticeExecutableNum;
	}
	protected boolean isDelayNoticeExecutable() {
		return delayNoticeExecutable;
	}
	protected void setDelayNoticeExecutable(boolean delayNoticeExecutable) {
		this.delayNoticeExecutable = delayNoticeExecutable;
	}
	protected int getDelayNoticeExecutableNum() {
		return delayNoticeExecutableNum;
	}
	protected void setDelayNoticeExecutableNum(int delayNoticeExecutableNum) {
		this.delayNoticeExecutableNum = delayNoticeExecutableNum;
	}
	protected int getNoticeExcecutableRetryCount() {
		return noticeExcecutableRetryCount;
	}
	public void setNoticeExcecutableRetryCount(int noticeExcecutableRetryCount) {
		this.noticeExcecutableRetryCount = noticeExcecutableRetryCount;
	}
	protected int getDelayNoticeExecutableRetryCount() {
		return delayNoticeExecutableRetryCount;
	}
	public void setDelayNoticeExecutableRetryCount(int delayNoticeExecutableRetryCount) {
		this.delayNoticeExecutableRetryCount = delayNoticeExecutableRetryCount;
	}
	protected int getOrderedNoticeExecutableRetryCount() {
		return orderedNoticeExecutableRetryCount;
	}
	public void setOrderedNoticeExecutableRetryCount(int orderedNoticeExecutableRetryCount) {
		this.orderedNoticeExecutableRetryCount = orderedNoticeExecutableRetryCount;
	}
	public void setRetryCount(int retryCount) {
		setNoticeExcecutableRetryCount(retryCount);
		setDelayNoticeExecutableRetryCount(retryCount);
		setOrderedNoticeExecutableRetryCount(retryCount);
	}
	protected long getNoticeMaxExecutableTime() {
		return noticeMaxExecutableTime;
	}
	public void setNoticeMaxExecutableTime(long noticeMaxExecutableTime) {
		this.noticeMaxExecutableTime = noticeMaxExecutableTime;
	}
	protected long getDelayNoticeMaxExecutableTime() {
		return delayNoticeMaxExecutableTime;
	}
	public void setDelayNoticeMaxExecutableTime(long delayNoticeMaxExecutableTime) {
		this.delayNoticeMaxExecutableTime = delayNoticeMaxExecutableTime;
	}
	protected long getOrderedNoticeMaxExecutableTime() {
		return orderedNoticeMaxExecutableTime;
	}
	public void setOrderedNoticeMaxExecutableTime(long orderedNoticeMaxExecutableTime) {
		this.orderedNoticeMaxExecutableTime = orderedNoticeMaxExecutableTime;
	}
	protected long getInvokeMaxExecutableTime() {
		return invokeMaxExecutableTime;
	}
	public void setInvokeMaxExecutableTime(long invokeMaxExecutableTime) {
		this.invokeMaxExecutableTime = invokeMaxExecutableTime;
	}
	public void setMaxExecutableTime(long maxExecutableTime) {
		setInvokeMaxExecutableTime(maxExecutableTime);
		setNoticeMaxExecutableTime(maxExecutableTime);
		setDelayNoticeMaxExecutableTime(maxExecutableTime);
		setOrderedNoticeMaxExecutableTime(maxExecutableTime);
	}
}
