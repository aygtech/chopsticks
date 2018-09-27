package com.chopsticks.core.rocketmq.handler;

import com.chopsticks.core.handler.HandlerParams;

public abstract class BaseHandlerParams implements HandlerParams{
	private String topic;
	private String tag;
	private byte[] body;
	
	public BaseHandlerParams(String topic, String tag, byte[] body) {
		super();
		this.topic = topic;
		this.tag = tag;
		this.body = body;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public String getTag() {
		return tag;
	}
	
	@Override
	public byte[] getBody() {
		return body;
	}
	@Override
	public String getMethod() {
		return tag;
	}
}
