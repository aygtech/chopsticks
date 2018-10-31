package com.chopsticks.core.rocketmq.modern.caller;

import com.chopsticks.core.modern.caller.NoticeCommand;

public abstract class BaseModernNoticeCommand implements NoticeCommand {
	private String method;
	private Object[] params;
	public BaseModernNoticeCommand(String method, Object... params) {
		this.method = method;
		this.params = params;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public Object[] getParams() {
		return params;
	}
	public void setParams(Object[] params) {
		this.params = params;
	}
}
