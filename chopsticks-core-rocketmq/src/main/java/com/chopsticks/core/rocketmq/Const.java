package com.chopsticks.core.rocketmq;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.utils.SyncSystemMillis;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

public class Const {
	
	private static final Logger log = LoggerFactory.getLogger(Const.class);
	
	public static final SyncSystemMillis CLIENT_TIME = new SyncSystemMillis(500L);
	// <delay, level>
	private static TreeMap<Long, Integer> DELAY_LEVEL = Maps.newTreeMap();
	
	public static final String DEFAULT_TOPIC = "_DEFAILT_TOPIC_";
	
	public static final String ALL_TAGS = "*";
	public static final String INVOKE_TOPIC_SUFFIX = "_INVOKE_TOPIC";
	public static final String NOTICE_TOPIC_SUFFIX = "_NOTICE_TOPIC";
	public static final String ORDERED_NOTICE_TOPIC_SUFFIX = "_ORDERED_NOTICE_TOPIC";
	
	public static final String INVOKE_REQUEST_KEY = "_INVOKE_REQUEST_";
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
	
	public static Optional<Integer> getDelayLevel(Long delay) {
		if(DELAY_LEVEL.isEmpty() || delay == null || delay <= 0) {
			log.warn("delayLevel is empty");
			return Optional.fromNullable(null);
		}
		Entry<Long, Integer> floor = DELAY_LEVEL.floorEntry(delay);
		if(floor != null && floor.getKey().equals(delay)) {
			log.trace("delay is match : {}", delay);
			return Optional.of(floor.getValue());
		}
		Entry<Long, Integer> ceil = DELAY_LEVEL.ceilingEntry(delay);
		if(ceil != null && ceil.getKey().equals(delay)) {
			log.trace("delay is match : {}, level", delay);
			return Optional.of(ceil.getValue());
		}
		if(floor == null && ceil == null) {
			log.error("delayLevel is error, delay {}, delayLevel {}", delay, DELAY_LEVEL);
			return Optional.fromNullable(null);
		}
		if(floor != null && ceil == null) {
			log.warn("delayLevel not match, src delay {} choose delay {} level {}", delay, floor.getKey(), floor.getValue());
			return Optional.of(floor.getValue());
		}
		if(floor == null && ceil != null) {
			log.warn("delayLevel not match, src delay {} choose delay {} level {}", delay, ceil.getKey(), ceil.getValue());
			return Optional.of(ceil.getValue());
		}
		Long floorDelay = floor.getKey();
		Long ceilDelay = ceil.getKey();
		if(delay - floorDelay < ceilDelay - delay) {
			log.warn("delayLevel not match, src delay {} choose delay {} level {}", delay, floor.getKey(), floor.getValue());
			return Optional.of(floor.getValue());
		}else {
			log.warn("delayLevel not match, src delay {} choose delay {} level {}", delay, ceil.getKey(), ceil.getValue());
			return Optional.of(ceil.getValue());
		}
	}
	
}
