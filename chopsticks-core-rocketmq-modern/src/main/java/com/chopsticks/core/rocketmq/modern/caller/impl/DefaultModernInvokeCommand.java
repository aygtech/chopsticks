package com.chopsticks.core.rocketmq.modern.caller.impl;

import com.chopsticks.core.modern.caller.ModernInvokeCommand;
import com.chopsticks.core.rocketmq.modern.caller.BaseModernCommand;

public class DefaultModernInvokeCommand extends BaseModernCommand implements ModernInvokeCommand{
	
	public DefaultModernInvokeCommand(String method, Object... params) {
		super(method, params);
	}
	
}
