package com.chopsticks.core.rocketmq.handler;

import com.chopsticks.common.concurrent.Promise;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;

public abstract class BaseHandlerResult extends BaseInvokeResult implements HandlerResult{
	
	private Promise<HandlerResult> promise;
	
	public BaseHandlerResult(byte[] body) {
		super(body);
	}
	
	@Override
	public Promise<HandlerResult> getPromise() {
		return promise;
	}
	public void setPromise(Promise<HandlerResult> promise) {
		this.promise = promise;
	}
}
