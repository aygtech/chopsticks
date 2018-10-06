package com.chopsticks.core.rocketmq;

import java.util.Map.Entry;
import java.util.TreeMap;

import com.chopsticks.core.utils.SyncSystemMillis;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

public class Const {
	
	public static final SyncSystemMillis CLIENT_TIME = new SyncSystemMillis(500L);
	// <delay, level>
	private static TreeMap<Long, Integer> DELAY_LEVEL = Maps.newTreeMap();
	
	public static final String DEFAULT_TOPIC = "_DEFAILT_TOPIC_";
	
	public static final String ALL_TAGS = "*";
	public static final String INVOKE_TOPIC_SUFFIX = "_INVOKE_TOPIC";
	public static final String NOTICE_TOPIC_SUFFIX = "_NOTICE_TOPIC";
	public static final String ORDERED_NOTICE_TOPIC_SUFFIX = "_ORDERED_NOTICE_TOPIC";
	
	public static final String INVOKE_REQUEST_KEY = "_INVOKE_REQUEST_";
	public static final String DELAY_NOTICE_REQUEST_KEY = "_DELAY_NOTICE_REQUEST_";
	public static final String INVOCE_RESP_TOPIC_SUFFIX = "_RESP_TOPIC";
	public static final String INVOCE_RESP_TAG_SUFFIX = "_RESP_TAG";
	
	public static final String PRODUCER_PREFIX = "PID_";
	public static final String CONSUMER_PREFIX = "CID_";
	
	public static final String INVOKE_CONSUMER_SUFFIX = "_INVOKE_CONSUMER";
	public static final String NOTICE_CONSUMER_SUFFIX = "_NOTICE_CONSUMER";
	public static final String ORDERED_NOTICE_CONSUMER_SUFFIX = "_ORDERED_NOTICE_CONSUMER";
	
	public static final String CALLER_INVOKE_CONSUMER_SUFFIX = "_CALLER_INVOKE_CONSUMER";
	public static final String CLIENT_TEST_TAG = "_CLIENT_TEST_TAG";
	
	
	public static final String ERROR_MSG_NO_ROUTE_INFO_OF_THIS_TOPIC = "No route info of this topic";
	public static final String ERROR_MSG_NO_NAME_SERVER_ADDRESS = "No name server address";
	
	
	static void setDelayLevel(TreeMap<Long, Integer> delayLevel) {
		synchronized (DELAY_LEVEL) {
			DELAY_LEVEL.clear();
			DELAY_LEVEL.putAll(delayLevel);
		}
	}
	
	public static Optional<Entry<Long, Integer>> getDelayLevel(Long delay) {
		if(DELAY_LEVEL.isEmpty() || delay == null || delay <= 0) {
			return Optional.fromNullable(null);
		}
		if(DELAY_LEVEL.containsKey(delay)) {
			return Optional.of(DELAY_LEVEL.floorEntry(delay));
		}
		Entry<Long, Integer> lower = DELAY_LEVEL.lowerEntry(delay);
		return Optional.fromNullable(lower);
	}
}
