package com.chopsticks.core.rocketmq.modern.caller.impl;

import com.chopsticks.core.modern.caller.ModernNoticeCommand;
import com.chopsticks.core.rocketmq.modern.caller.BaseModernCommand;

public class DefaultModernNoticeCommand extends BaseModernCommand implements ModernNoticeCommand{
	
	private boolean transaction;
	
	public DefaultModernNoticeCommand(String method, Object... params) {
		super(method, params);
	}
	
	
	public boolean isTransaction() {
		return transaction;
	}
	public <T extends BaseModernCommand> T setTransaction(boolean transaction) {
		this.transaction = transaction;
		@SuppressWarnings("unchecked")
		T ret = (T) this;
		return ret;
	}
	
}
