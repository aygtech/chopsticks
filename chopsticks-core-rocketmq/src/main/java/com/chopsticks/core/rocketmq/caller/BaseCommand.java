package com.chopsticks.core.rocketmq.caller;

import com.chopsticks.core.caller.Command;

public abstract class BaseCommand implements Command {
	
	private String topic;
	private String tag;
	private byte[] body;
	public BaseCommand(String topic, String tag, byte[] body) {
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
	public String getMethod() {
		return tag;
	}
	@Override
	public byte[] getBody() {
		return body;
	}
}
