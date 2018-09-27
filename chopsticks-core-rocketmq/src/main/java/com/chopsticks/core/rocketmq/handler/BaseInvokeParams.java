package com.chopsticks.core.rocketmq.handler;

import com.chopsticks.core.handler.InvokeParams;

public abstract class BaseInvokeParams extends BaseHandlerParams implements InvokeParams {

	public BaseInvokeParams(String topic, String tag, byte[] body) {
		super(topic, tag, body);
	}


}
