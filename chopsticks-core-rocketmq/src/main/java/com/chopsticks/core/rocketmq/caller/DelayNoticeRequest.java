package com.chopsticks.core.rocketmq.caller;

public class DelayNoticeRequest extends BaseRequest{
	private String rootId;
	private long invokeTime;
	private long executeTime;
	private String executeGroupName;

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
	public long getInvokeTime() {
		return invokeTime;
	}
	public void setInvokeTime(long invokeTime) {
		this.invokeTime = invokeTime;
	}
	public String getExecuteGroupName() {
		return executeGroupName;
	}
	public void setExecuteGroupName(String executeGroupName) {
		this.executeGroupName = executeGroupName;
	}
}
