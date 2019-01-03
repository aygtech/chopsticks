package com.chopsticks.core.rocketmq.modern.caller.impl;

import com.chopsticks.core.modern.caller.ModernNoticeCommand;
import com.chopsticks.core.rocketmq.modern.caller.BaseModernCommand;

public class DefaultModernNoticeCommand extends BaseModernCommand implements ModernNoticeCommand{
	
	public DefaultModernNoticeCommand(String method, Object... params) {
		super(method, params);
	}
	
}
