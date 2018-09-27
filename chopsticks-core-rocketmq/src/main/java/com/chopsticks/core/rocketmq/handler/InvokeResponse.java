package com.chopsticks.core.rocketmq.handler;

public class InvokeResponse {
	
	private String reqMsgId;
	private String respExceptionBody;
	private byte[] respBody;
	
	public InvokeResponse() {
		super();
	}
	public InvokeResponse(String reqMsgId, String respExceptionBody) {
		super();
		this.reqMsgId = reqMsgId;
		this.respExceptionBody = respExceptionBody;
	}
	public InvokeResponse(String reqMsgId, byte[] respBody) {
		super();
		this.reqMsgId = reqMsgId;
		this.respBody = respBody;
	}
	public String getReqMsgId() {
		return reqMsgId;
	}
	public void setReqMsgId(String reqMsgId) {
		this.reqMsgId = reqMsgId;
	}
	public byte[] getRespBody() {
		return respBody;
	}
	public void setRespBody(byte[] respBody) {
		this.respBody = respBody;
	}
	public String getRespExceptionBody() {
		return respExceptionBody;
	}
	public void setRespExceptionBody(String respExceptionBody) {
		this.respExceptionBody = respExceptionBody;
	}
}
