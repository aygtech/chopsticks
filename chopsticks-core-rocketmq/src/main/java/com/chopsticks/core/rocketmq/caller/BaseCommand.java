package com.chopsticks.core.rocketmq.caller;

import java.util.Map;
import java.util.Set;

import com.chopsticks.core.caller.Command;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class BaseCommand implements Command {
	
	private String topic;
	private String tag;
	private byte[] body;
	private Set<String> traceNos = Sets.newHashSet();
	private Map<String, String> extParams = Maps.newHashMap();
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
	public void setTraceNos(Set<String> traceNos) {
		this.traceNos = traceNos;
	}
	public Set<String> getTraceNos() {
		return traceNos;
	}
	public void setExtParams(Map<String, String> extParams) {
		this.extParams = extParams;
	}
	public Map<String, String> getExtParams() {
		return extParams;
	}
}
