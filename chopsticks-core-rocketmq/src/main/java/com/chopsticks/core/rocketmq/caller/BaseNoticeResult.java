package com.chopsticks.core.rocketmq.caller;

import com.chopsticks.core.caller.NoticeResult;

public abstract class BaseNoticeResult extends BaseResult implements NoticeResult {
	
	private String id;
	private String originId;
	
	public BaseNoticeResult(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}
	public String getOriginId() {
		return originId;
	}
	public void setOriginId(String originId) {
		this.originId = originId;
	}
	public void setId(String id) {
		this.id = id;
	}
}
