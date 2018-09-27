package com.chopsticks.core.rockctmq.modern.caller;

import com.chopsticks.core.modern.caller.ExtNoticeCommand;

public abstract class BaseExtNoticeCommand implements ExtNoticeCommand {
	
	private String method;
	private Object[] params;
	
	public BaseExtNoticeCommand(String method) {
		this.method = method;
	}
	
	public BaseExtNoticeCommand(String method, Object... params) {
		this.method = method;
		this.params = params;
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public Object[] getParams() {
		return params;
	}

}
