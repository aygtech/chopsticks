package com.chopsticks.core.rocketmq.handler.impl;

import com.chopsticks.core.rocketmq.handler.BaseNoticeContext;

public class DefaultNoticeContext extends BaseNoticeContext {

	public DefaultNoticeContext(String id, int reconsumeTimes) {
		super(id, reconsumeTimes);
	}

}
