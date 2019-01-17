package com.chopsticks.core.rocketmq.caller;

public class InvokeRequest extends BaseRequest{
	private String reqId;
	private long deadline;
	private boolean compress;
	private String respTopic;
	private String respTag;
	private boolean respCompress;
	
	
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
	public String getReqId() {
		return reqId;
	}
	public void setReqId(String reqId) {
		this.reqId = reqId;
	}
	public boolean isCompress() {
		return compress;
	}
	public void setCompress(boolean compress) {
		this.compress = compress;
	}
	public boolean isRespCompress() {
		return respCompress;
	}
	public void setRespCompress(boolean respCompress) {
		this.respCompress = respCompress;
	}
}
