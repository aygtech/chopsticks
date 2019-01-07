package com.chopsticks.core.rocketmq.caller;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class BaseRequest {
	private Map<String, String> extParams = Maps.newHashMap();
	private Set<String> traceNos = Sets.newHashSet();
	
	public void setExtParams(Map<String, String> extParams) {
		this.extParams = extParams;
	}
	public Map<String, String> getExtParams() {
		return extParams;
	}
	public void setTraceNos(Set<String> traceNos) {
		this.traceNos = traceNos;
	}
	public Set<String> getTraceNos() {
		return traceNos;
	}
}
