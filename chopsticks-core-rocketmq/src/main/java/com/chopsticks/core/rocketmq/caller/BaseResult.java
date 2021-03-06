package com.chopsticks.core.rocketmq.caller;

import java.util.Set;

import com.google.common.collect.Sets;

public abstract class BaseResult {
	private Set<String> traceNos = Sets.newHashSet();
	
	public void setTraceNos(Set<String> traceNos) {
		this.traceNos = traceNos;
	}
	public Set<String> getTraceNos() {
		return traceNos;
	}
}
