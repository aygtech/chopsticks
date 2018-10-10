package com.chopsticks.core.rocketmq.handler;

import com.chopsticks.core.handler.NoticeContext;

public abstract class BaseNoticeContext implements NoticeContext{
	
	private String id;
	private String originId;
	
	private int reconsumeTimes;
	
	public BaseNoticeContext(String id, String originId, int reconsumeTimes) {
		this.id = id;
		this.originId = originId;
		this.reconsumeTimes = reconsumeTimes;
	}
	
	public int getReconsumeTimes() {
		return reconsumeTimes;
	}
	@Override
	public String getId() {
		return id;
	}
	public String getOriginId() {
		return originId;
	}
}
