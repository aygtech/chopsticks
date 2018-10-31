package com.chopsticks.core.rocketmq.modern.caller.impl;

import com.chopsticks.core.rocketmq.modern.caller.BaseModernInvokeCommand;

public class DefaultModernInvokeCommand extends BaseModernInvokeCommand {
	
	public DefaultModernInvokeCommand(String method, Object... params) {
		super(method, params);
	}
	
}
