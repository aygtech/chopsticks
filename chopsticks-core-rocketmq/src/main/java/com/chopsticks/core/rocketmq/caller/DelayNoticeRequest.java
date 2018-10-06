package com.chopsticks.core.rocketmq.caller;

public class DelayNoticeRequest {
	private String rootId;
	private long executeTime;

	public long getExecuteTime() {
		return executeTime;
	}
	public void setExecuteTime(long executeTime) {
		this.executeTime = executeTime;
	}
	public String getRootId() {
		return rootId;
	}
	public void setRootId(String rootId) {
		this.rootId = rootId;
	}
}
