package com.chopsticks.core.rocketmq.handler.impl;

import com.chopsticks.core.rocketmq.handler.BaseInvokeParams;

public class DefaultInvokeParams extends BaseInvokeParams {

	public DefaultInvokeParams(String topic, String tag, byte[] body) {
		super(topic, tag, body);
	}
}
