package com.chopsticks.core.rockctmq.modern.caller;

import com.chopsticks.core.modern.caller.ExtNoticeResult;

public abstract class BaseExtNoticeResult implements ExtNoticeResult{
	
	private String id;
	
	public BaseExtNoticeResult(String id) {
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}
}
