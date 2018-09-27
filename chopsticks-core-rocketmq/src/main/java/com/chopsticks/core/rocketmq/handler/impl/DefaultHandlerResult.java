package com.chopsticks.core.rocketmq.handler.impl;

import com.chopsticks.core.rocketmq.handler.BaseHandlerResult;

public class DefaultHandlerResult extends BaseHandlerResult {
	
	public DefaultHandlerResult() {
		super(null);
	}
	
	public DefaultHandlerResult(byte[] body) {
		super(body);
	}

}
