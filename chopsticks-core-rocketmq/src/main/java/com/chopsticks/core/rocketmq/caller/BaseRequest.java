package com.chopsticks.core.rocketmq.caller;

import java.util.Map;

import com.google.common.collect.Maps;

public abstract class BaseRequest {
	private Map<String, String> extParams = Maps.newHashMap();
	
	public void setExtParams(Map<String, String> extParams) {
		this.extParams = extParams;
	}
	public Map<String, String> getExtParams() {
		return extParams;
	}
}
