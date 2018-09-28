package com.chopsticks.core.rockctmq.modern.caller.impl;

import com.chopsticks.core.rockctmq.modern.caller.BaseModernInvokeCommand;

public class DefaultModernInvokeCommand extends BaseModernInvokeCommand {
	
	public DefaultModernInvokeCommand(String method, Object... params) {
		super(method, params);
	}
	
}
