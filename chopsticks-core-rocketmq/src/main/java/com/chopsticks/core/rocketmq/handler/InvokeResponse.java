package com.chopsticks.core.rocketmq.handler;

import java.util.Set;

import com.google.common.collect.Sets;

public class InvokeResponse {
	
	private String reqId;
	private Set<String> traceNos = Sets.newHashSet();
	private String respExceptionBody;
	private byte[] respBody;
	private boolean compressRespBody;
	private long respTime;
	private long reqTime;
	
	public InvokeResponse() {
		super();
	}
	public InvokeResponse(String reqId, long reqTime, long respTime, String respExceptionBody) {
		super();
		this.reqId = reqId;
		this.reqTime = reqTime;
		this.respTime = respTime;
		this.respExceptionBody = respExceptionBody;
	}
	public InvokeResponse(String reqId, long reqTime, long respTime, byte[] respBody) {
		super();
		this.reqId = reqId;
		this.reqTime = reqTime;
		this.respTime = respTime;
		this.respBody = respBody;
	}
	public String getReqId() {
		return reqId;
	}
	public void setReqId(String reqId) {
		this.reqId = reqId;
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
	public long getRespTime() {
		return respTime;
	}
	public void setRespTime(long respTime) {
		this.respTime = respTime;
	}
	public long getReqTime() {
		return reqTime;
	}
	public void setReqTime(long reqTime) {
		this.reqTime = reqTime;
	}
	public Set<String> getTraceNos() {
		return traceNos;
	}
	public void setTraceNos(Set<String> traceNos) {
		this.traceNos = traceNos;
	}
	public boolean isCompressRespBody() {
		return compressRespBody;
	}
	public void setCompressRespBody(boolean compressRespBody) {
		this.compressRespBody = compressRespBody;
	}
}
