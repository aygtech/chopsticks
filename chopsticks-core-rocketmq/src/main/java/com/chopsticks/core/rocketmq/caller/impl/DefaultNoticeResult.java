package com.chopsticks.core.rocketmq.caller.impl;

import com.chopsticks.core.rocketmq.caller.BaseNoticeResult;

public class DefaultNoticeResult extends BaseNoticeResult{
	
	public DefaultNoticeResult(String id) {
		super(id);
	}

	@Override
	public String toString() {
		return "DefaultNoticeResult [getId()=" + getId() + ", getTraceNos()=" + getTraceNos() + "]";
	}
}
