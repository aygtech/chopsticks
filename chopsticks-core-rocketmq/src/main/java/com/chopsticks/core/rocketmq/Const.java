package com.chopsticks.core.rocketmq;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;

import com.alibaba.fastjson.parser.ParserConfig;
import com.chopsticks.common.utils.Reflect;
import com.chopsticks.common.utils.SyncSystemMillis;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class Const {

	static {
		ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
	}

	public static final SyncSystemMillis CLIENT_TIME = new SyncSystemMillis(TimeUnit.MILLISECONDS.toMillis(500L));
	// <delay, level>
	private static final TreeMap<Long, Integer> DELAY_LEVEL = Maps.newTreeMap();

	static {
		// defualt 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
		TreeMap<Long, Integer> delayLevel = Maps.newTreeMap();
		delayLevel.put(TimeUnit.SECONDS.toMillis(1), 1);
		delayLevel.put(TimeUnit.SECONDS.toMillis(5), 2);
		delayLevel.put(TimeUnit.SECONDS.toMillis(10), 3);
		delayLevel.put(TimeUnit.SECONDS.toMillis(30), 4);
		delayLevel.put(TimeUnit.MINUTES.toMillis(1), 5);
		delayLevel.put(TimeUnit.MINUTES.toMillis(2), 6);
		delayLevel.put(TimeUnit.MINUTES.toMillis(3), 7);
		delayLevel.put(TimeUnit.MINUTES.toMillis(4), 8);
		delayLevel.put(TimeUnit.MINUTES.toMillis(5), 9);
		delayLevel.put(TimeUnit.MINUTES.toMillis(6), 10);
		delayLevel.put(TimeUnit.MINUTES.toMillis(7), 11);
		delayLevel.put(TimeUnit.MINUTES.toMillis(8), 12);
		delayLevel.put(TimeUnit.MINUTES.toMillis(9), 13);
		delayLevel.put(TimeUnit.MINUTES.toMillis(10), 14);
		delayLevel.put(TimeUnit.MINUTES.toMillis(20), 15);
		delayLevel.put(TimeUnit.MINUTES.toMillis(30), 16);
		delayLevel.put(TimeUnit.HOURS.toMillis(1), 17);
		delayLevel.put(TimeUnit.HOURS.toMillis(2), 18);
		setDelayLevel(delayLevel);
	}

	public static final String DEFAULT_TOPIC = "_DEFAILT_TOPIC_";

	public static final String ALL_TAGS = "*";
	public static final String INVOKE_TOPIC_SUFFIX = "_INVOKE_TOPIC";
	public static final String NOTICE_TOPIC_SUFFIX = "_NOTICE_TOPIC";
	public static final String DELAY_NOTICE_TOPIC_SUFFIX = "_DELAY_NOTICE_TOPIC";
	public static final String ORDERED_NOTICE_TOPIC_SUFFIX = "_ORDERED_NOTICE_TOPIC";

	public static final String INVOKE_REQUEST_KEY = "_INVOKE_REQUEST_";
	public static final String NOTICE_REQUEST_KEY = "_NOTICE_REQUEST_";
	public static final String DELAY_NOTICE_REQUEST_KEY = "_DELAY_NOTICE_REQUEST_";
	public static final String ORDERED_NOTICE_REQUEST_KEY = "_ORDERED_NOTICE_REQUEST_";
	public static final String INVOCE_RESP_TOPIC_SUFFIX = "_RESP_TOPIC";
	public static final String INVOCE_RESP_TAG_SUFFIX = "_RESP_TAG";

	public static final String PRODUCER_PREFIX = "PID_";
	public static final String CONSUMER_PREFIX = "CID_";
	
	public static final String TRACE_METHOD_PREFIX = "TRACE_METHOD_";
	public static final String TRACE_INVOKE_REQ_PREFIX = "TRACE_REQ_";
	public static final String TRACE_ORDERED_PREFIX = "TRACE_ORDERED_";
	
	
	public static final String INVOKE_ADMIN_EXT_SUFFIX = "_INVOKE_ADMIN_EXT";

	public static final String INVOKE_CONSUMER_SUFFIX = "_INVOKE_CONSUMER";
	public static final String NOTICE_CONSUMER_SUFFIX = "_NOTICE_CONSUMER";
	public static final String DELAY_NOTICE_CONSUMER_SUFFIX = "_DELAY_NOTICE_CONSUMER";
	public static final String ORDERED_NOTICE_CONSUMER_SUFFIX = "_ORDERED_NOTICE_CONSUMER";

	public static final String CALLER_INVOKE_CONSUMER_SUFFIX = "_CALLER_INVOKE_CONSUMER";
	public static final String CLIENT_TEST_TAG = "_CLIENT_TEST_TAG";
	
	public static final int DEFAULT_TOPIC_QUEUE_SIZE = 32;

	public static final String ERROR_MSG_CAN_NOT_FIND_MESSAGE_QUEUE = "Can not find Message Queue";

	public static void setDelayLevel(TreeMap<Long, Integer> delayLevel) {
		synchronized (DELAY_LEVEL) {
			DELAY_LEVEL.clear();
			DELAY_LEVEL.putAll(delayLevel);
		}
	}

	public static Optional<Entry<Long, Integer>> getDelayLevel(Long delay) {
		if (DELAY_LEVEL.isEmpty() || delay == null || delay <= 0) {
			return Optional.fromNullable(null);
		}
		return Optional.fromNullable(DELAY_LEVEL.floorEntry(delay));
	}
	
	public static DefaultMQPushConsumer buildConsumer(DefaultMQPushConsumer consumer) {
		Reflect consumeMessageService = Reflect.on(consumer)
											   .field("defaultMQPushConsumerImpl")
											   .field("consumeMessageService");
		String group = consumer.getConsumerGroup();
		if(group.contains("%")) {
			group = group.substring(group.indexOf("%") + 1); 
		}
		consumeMessageService.field("consumeExecutor")
			   				 .set("threadFactory", new ThreadFactoryBuilder().setNameFormat(group + "_%d")
																			 .build());
		//已经启动并启动好了一个调度线程，不太好改名字了
//		if(consumeMessageService.fields().containsKey("cleanExpireMsgExecutors")) {
//			consumeMessageService.field("cleanExpireMsgExecutors")
//								 .field("e")
//								 .set("threadFactory", new ThreadFactoryBuilder().setDaemon(true)
//																				 .setNameFormat(consumer.getConsumerGroup() + "_clearExpireMsg_%d")
//																				 .build());
//		}
//		consumeMessageService.field("scheduledExecutorService")
//							 .field("e")
//		 					 .set("threadFactory", new ThreadFactoryBuilder().setDaemon(true)
//																			 .setNameFormat(consumer.getConsumerGroup() + "_schedule_%d")
//																			 .build());
		
		return consumer;
	}
	
	public static String buildCustomTag(String groupName, String tag) {
		return groupName + "-" + tag;
	}
	
	public static String getOriginTag(String groupName, String customTag) {
		return customTag.replaceFirst(groupName + "-", "");
	}
	
	public static String buildTestTag(String groupName) {
		return groupName + CLIENT_TEST_TAG;
	}
	
	public static String buildTraceInvokeReqId(String reqId) {
		return TRACE_INVOKE_REQ_PREFIX + reqId;
	}
	
	public static String buildTraceNoByMethod(String method) {
		return TRACE_METHOD_PREFIX + method;
	}
	public static String buildTraceNoByOrdered(String orderKey) {
		return TRACE_ORDERED_PREFIX + orderKey;
	}
}
