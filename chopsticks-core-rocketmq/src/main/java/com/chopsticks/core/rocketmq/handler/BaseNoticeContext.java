package com.chopsticks.core.rocketmq.handler;

import com.chopsticks.core.handler.NoticeContext;

public abstract class BaseNoticeContext implements NoticeContext{
	
	private String id;
	
	private int reconsumeTimes;
	
	public BaseNoticeContext(String id, int reconsumeTimes) {
		this.id = id;
		this.reconsumeTimes = reconsumeTimes;
	}
	public int getReconsumeTimes() {
		return reconsumeTimes;
	}
	@Override
	public String getId() {
		return id;
	}
}
