package com.chopsticks.core.rocketmq.handler;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.chopsticks.core.handler.Handler;

public abstract class BaseHandler implements Handler {
	
	private String topic;
	
	private String tag;
	
	private boolean supportInvoke = true;
	private boolean supportNotice = true;
	private boolean supportDelayNotice = true;
	private boolean supportOrderedNotice = true;
	
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
	@Override
	public boolean isSupportDelayNotice() {
		return supportDelayNotice;
	}
	@Override
	public boolean isSupportInvoke() {
		return supportInvoke;
	}
	@Override
	public boolean isSupportNotice() {
		return supportNotice;
	}
	@Override
	public boolean isSupportOrderedNotice() {
		return supportOrderedNotice;
	}
	public void setSupportOrderedNotice(boolean supportOrderedNotice) {
		this.supportOrderedNotice = supportOrderedNotice;
	}
	public void setSupportInvoke(boolean supportInvoke) {
		this.supportInvoke = supportInvoke;
	}
	public void setSupportNotice(boolean supportNotice) {
		this.supportNotice = supportNotice;
	}
	public void setSupportDelayNotice(boolean supportDelayNotice) {
		this.supportDelayNotice = supportDelayNotice;
	}
	
}
