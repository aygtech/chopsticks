package com.chopsticks.core.rocketmq.modern.caller;

import java.util.Map;
import java.util.Set;

import com.chopsticks.core.modern.caller.ModernCommand;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class BaseModernCommand implements ModernCommand {
	private String method;
	private Object[] params;
	private Set<String> traceNos = Sets.newHashSet();
	private Map<String, String> extParams = Maps.newHashMap();
	
	public BaseModernCommand(String method, Object... params) {
		this.method = method;
		this.params = params;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	@Override
	public String getMethod() {
		return method;
	}
	public void setParams(Object[] params) {
		this.params = params;
	}
	@Override
	public Object[] getParams() {
		return params;
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
