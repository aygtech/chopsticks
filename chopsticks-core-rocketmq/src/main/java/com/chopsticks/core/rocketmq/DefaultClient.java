package com.chopsticks.core.rocketmq;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
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
			}else {
				baseHandlers.add(new BaseHandlerWapper(handler, Const.DEFAULT_TOPIC, handler.getMethod()));
			}
		}
		return baseHandlers;
	}

	@Override
	public synchronized void shutdown() {
		super.shutdown();
		if(invokeConsumer != null) {
			invokeConsumer.shutdown();
		}
		if(delayNoticeConsumer != null) {
			delayNoticeConsumer.shutdown();
		}
		if(noticeConsumer != null){
			noticeConsumer.shutdown();
		}
		if(orderedNoticeConsumer != null) {
			orderedNoticeConsumer.shutdown();
		}
		started = false;
	}
	
	@Override
	public synchronized void start() {
		log.info("InvokeExecutable : {}", isInvokeExecutable());
		log.info("NoticeExecutable : {}", isNoticeExecutable());
		log.info("DelayNoticeExecutable : {}", isDelayNoticeExecutable());
		log.info("OrderedNoticeExecutable : {}", isOrderedNoticeExecutable());
		if(!started) {
			super.start();
			buildTopicTagsAndTopicTagHandlers();
			if(!topicTags.isEmpty()) {
				addTest();
				invokeConsumer = buildAndStartInvokeCosumer();
				delayNoticeConsumer = buildAndStartDelayNoticeCosumer();
				noticeConsumer = buildAndStartNoticeCosumer();
				orderedNoticeConsumer = buildAndStartOrderedNoticeCosumer();
			}
			started = true;
		}
	}
	
	private void addTest() {
		for(String topic : topicTags.keySet()) {
			EmptyHandler handler = new EmptyHandler(topic, Const.CLIENT_TEST_TAG);
			topicTags.put(handler.getTopic(), handler.getTag());
			topicTagHandlers.put(handler.getTopic() + handler.getTag(), handler);
		}
	}

	private void testInvokeClient(DefaultMQPushConsumer invokeConsumer) {
		try {
			for(String topic : topicTags.keySet()) {
				BaseInvokeCommand cmd = new DefaultInvokeCommand(topic, Const.CLIENT_TEST_TAG, Const.CLIENT_TEST_TAG.getBytes());
				InvokeRequest req = buildInvokeRequest(cmd, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
				Message msg = buildInvokeMessage(req, cmd, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
				SendResult ret = getProducer().send(msg);
				if(ret.getSendStatus() != SendStatus.SEND_OK) {
					throw new RuntimeException(ret.getSendStatus().name());
				}
				invokeConsumer.fetchSubscribeMessageQueues(buildInvokeTopic(topic));
			}
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	private void testDelayNoticeClient(DefaultMQPushConsumer delayNoticeConsumer) {
		try {
			for(String topic : topicTags.keySet()) {
				this.notice(new DefaultNoticeCommand(topic, Const.CLIENT_TEST_TAG, Const.CLIENT_TEST_TAG.getBytes()), DEFAULT_ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
				delayNoticeConsumer.fetchSubscribeMessageQueues(buildDelayNoticeTopic(topic));
			}
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	private void testNoticeClient(DefaultMQPushConsumer noticeConsumer) {
		try {
			for(String topic : topicTags.keySet()) {
				this.notice(new DefaultNoticeCommand(topic, Const.CLIENT_TEST_TAG, Const.CLIENT_TEST_TAG.getBytes()));
				noticeConsumer.fetchSubscribeMessageQueues(buildNoticeTopic(topic));
			}
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	private void testOrderedNoticeClient(DefaultMQPushConsumer orderedNoticeConsumer) {
		try {
			for(String topic : topicTags.keySet()) {
				this.notice(new DefaultNoticeCommand(topic, Const.CLIENT_TEST_TAG, Const.CLIENT_TEST_TAG.getBytes()), Const.CLIENT_TEST_TAG);
				orderedNoticeConsumer.fetchSubscribeMessageQueues(buildOrderedNoticeTopic(topic));
			}
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
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
			orderedNoticeConsumer.setMaxReconsumeTimes(Integer.MAX_VALUE);
			orderedNoticeConsumer.setConsumeMessageBatchMaxSize(1);
			orderedNoticeConsumer.setSuspendCurrentQueueTimeMillis(TimeUnit.SECONDS.toMillis(5L));
			orderedNoticeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			orderedNoticeConsumer.registerMessageListener(new HandlerOrderedNoticeListener(topicTags, topicTagHandlers));
			orderedNoticeConsumer.setPullThresholdSizeForTopic(20);
			try {
				for(Entry<String, Collection<String>> entry: topicTags.asMap().entrySet()) {
					String topic = entry.getKey();
					Collection<String> tags = entry.getValue();
					topic = buildOrderedNoticeTopic(topic);
					realConsumeFromLastOffset(orderedNoticeConsumer.getMessageModel(), orderedNoticeConsumer.getInstanceName(), orderedNoticeConsumer.getConsumerGroup(), topic);
					if(tags.contains(Const.ALL_TAGS)) {
						orderedNoticeConsumer.subscribe(topic, Const.ALL_TAGS);
					}else {
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
			noticeConsumer.setMaxReconsumeTimes(Integer.MAX_VALUE);
			noticeConsumer.setConsumeMessageBatchMaxSize(1);
			noticeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			noticeConsumer.registerMessageListener(new HandlerNoticeListener(noticeConsumer, getProducer(), topicTags, topicTagHandlers));
			noticeConsumer.setPullThresholdSizeForTopic(50);
			try {
				for(Entry<String, Collection<String>> entry: topicTags.asMap().entrySet()) {
					String topic = entry.getKey();
					Collection<String> tags = entry.getValue();
					topic = buildNoticeTopic(topic);
					realConsumeFromLastOffset(noticeConsumer.getMessageModel(), noticeConsumer.getInstanceName(), noticeConsumer.getConsumerGroup(), topic);
					if(tags.contains(Const.ALL_TAGS)) {
						noticeConsumer.subscribe(topic, Const.ALL_TAGS);
					}else {
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
			delayNoticeConsumer.setMaxReconsumeTimes(Integer.MAX_VALUE);
			delayNoticeConsumer.setConsumeMessageBatchMaxSize(1);
			delayNoticeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			delayNoticeConsumer.registerMessageListener(new HandlerDelayNoticeListener(delayNoticeConsumer, getProducer(), topicTags, topicTagHandlers));
			delayNoticeConsumer.setPullThresholdSizeForTopic(50);
			try {
				for(Entry<String, Collection<String>> entry: topicTags.asMap().entrySet()) {
					String topic = entry.getKey();
					Collection<String> tags = entry.getValue();
					topic = buildDelayNoticeTopic(topic);
					realConsumeFromLastOffset(delayNoticeConsumer.getMessageModel(), delayNoticeConsumer.getInstanceName(), delayNoticeConsumer.getConsumerGroup(), topic);
					if(tags.contains(Const.ALL_TAGS)) {
						delayNoticeConsumer.subscribe(topic, Const.ALL_TAGS);
					}else {
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
			invokeConsumer.setConsumeMessageBatchMaxSize(1);
			invokeConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			invokeConsumer.registerMessageListener(new HandlerInvokeListener(getProducer(), topicTagHandlers));
			invokeConsumer.setPullThresholdSizeForTopic(50);
			try {
				for(Entry<String, Collection<String>> entry: topicTags.asMap().entrySet()) {
					String topic = entry.getKey();
					Collection<String> tags = entry.getValue();
					topic = buildInvokeTopic(topic);
					resetNow(invokeConsumer.getMessageModel(), invokeConsumer.getInstanceName(), invokeConsumer.getConsumerGroup(), topic);
					if(tags.contains(Const.ALL_TAGS)) {
						invokeConsumer.subscribe(topic, Const.ALL_TAGS);
					}else {
						invokeConsumer.subscribe(topic, Joiner.on("||").join(tags));
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
}
