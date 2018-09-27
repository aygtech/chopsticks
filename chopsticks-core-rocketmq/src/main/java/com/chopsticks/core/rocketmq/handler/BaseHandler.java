package com.chopsticks.core.rocketmq.handler;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.chopsticks.core.handler.Handler;

public abstract class BaseHandler implements Handler {
	
	private String topic;
	
	private String tag;
	
	public BaseHandler(String topic, String tag) {
		checkArgument(!isNullOrEmpty(topic), "topic cannot be null or empty");
		checkArgument(!isNullOrEmpty(tag), "topic cannot be null or empty");
		
		this.topic = topic;
		this.tag = tag;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public String getTag() {
		return tag;
	}
	
	@Override
	public String getMethod() {
		return tag;
	}
}
