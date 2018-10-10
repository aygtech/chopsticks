package com.chopsticks.core.rocketmq.handler.impl;

import com.chopsticks.core.rocketmq.handler.BaseNoticeContext;

public class DefaultNoticeContext extends BaseNoticeContext {

	public DefaultNoticeContext(String id, String originId, int reconsumeTimes) {
		super(id, originId, reconsumeTimes);
	}

}
