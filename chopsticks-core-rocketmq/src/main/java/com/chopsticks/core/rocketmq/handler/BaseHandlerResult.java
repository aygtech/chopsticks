package com.chopsticks.core.rocketmq.handler;

import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;

public abstract class BaseHandlerResult extends BaseInvokeResult implements HandlerResult{

	public BaseHandlerResult(byte[] body) {
		super(body);
	}
	
}
