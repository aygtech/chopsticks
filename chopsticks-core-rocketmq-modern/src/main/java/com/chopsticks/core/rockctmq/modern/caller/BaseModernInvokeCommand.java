package com.chopsticks.core.rockctmq.modern.caller;

import com.chopsticks.core.modern.caller.InvokeCommand;

public abstract class BaseModernInvokeCommand implements InvokeCommand {
	private String method;
	private Object[] params;
	public BaseModernInvokeCommand(String method, Object... params) {
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
