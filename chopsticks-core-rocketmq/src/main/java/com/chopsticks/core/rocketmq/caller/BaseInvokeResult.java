package com.chopsticks.core.rocketmq.caller;

import com.chopsticks.core.caller.InvokeResult;

public abstract class BaseInvokeResult extends BaseResult implements InvokeResult {
	
	private byte[] body;
	
	public BaseInvokeResult(byte[] body) {
		this.body = body;
	}

	@Override
	public byte[] getBody() {
		return body;
	}

}
