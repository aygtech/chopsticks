package com.chopsticks.core.rocketmq.caller;

import com.chopsticks.core.caller.InvokeCommand;

public abstract class BaseInvokeCommand extends BaseCommand implements InvokeCommand{

	public BaseInvokeCommand(String topic, String tag, byte[] body) {
		super(topic, tag, body);
	}
	
}
