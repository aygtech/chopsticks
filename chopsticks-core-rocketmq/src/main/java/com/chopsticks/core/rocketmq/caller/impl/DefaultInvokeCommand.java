package com.chopsticks.core.rocketmq.caller.impl;

import com.chopsticks.core.rocketmq.caller.BaseInvokeCommand;

public class DefaultInvokeCommand extends BaseInvokeCommand{

	public DefaultInvokeCommand(String topic, String tag, byte[] body) {
		super(topic, tag, body);
	}
	
}
