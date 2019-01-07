package com.chopsticks.core.rocketmq.caller.impl;

import java.util.Arrays;

import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;

public class DefaultInvokeResult extends BaseInvokeResult {

	public DefaultInvokeResult(byte[] body) {
		super(body);
	}

	@Override
	public String toString() {
		return "DefaultInvokeResult [getBody()=" + Arrays.toString(getBody()) + ", getTraceNos()=" + getTraceNos()
				+ "]";
	}
	
}
