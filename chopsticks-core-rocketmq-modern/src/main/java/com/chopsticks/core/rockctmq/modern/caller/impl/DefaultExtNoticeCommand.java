package com.chopsticks.core.rockctmq.modern.caller.impl;

import com.chopsticks.core.rockctmq.modern.caller.BaseExtNoticeCommand;

public class DefaultExtNoticeCommand extends BaseExtNoticeCommand {
	
	public DefaultExtNoticeCommand(String method) {
		super(method);
	}
	
	public DefaultExtNoticeCommand(String method, Object... params) {
		super(method, params);
	}
	
}
