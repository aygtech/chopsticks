package com.chopsticks.core.rocketmq.caller;

import com.chopsticks.core.caller.NoticeResult;

public abstract class BaseNoticeResult extends BaseResult implements NoticeResult {
	
	private String id;
	
	public BaseNoticeResult(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}
}
