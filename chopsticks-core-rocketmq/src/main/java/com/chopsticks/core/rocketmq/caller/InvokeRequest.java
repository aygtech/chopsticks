package com.chopsticks.core.rocketmq.caller;

public class InvokeRequest {
	private long beginTime;
	private long deadline;
	private String respTopic;
	private String respTag;
	public long getBeginTime() {
		return beginTime;
	}
	public void setBeginTime(long beginTime) {
		this.beginTime = beginTime;
	}
	public long getDeadline() {
		return deadline;
	}
	public void setDeadline(long deadline) {
		this.deadline = deadline;
	}
	public String getRespTopic() {
		return respTopic;
	}
	public void setRespTopic(String respTopic) {
		this.respTopic = respTopic;
	}
	public String getRespTag() {
		return respTag;
	}
	public void setRespTag(String respTag) {
		this.respTag = respTag;
	}
}
