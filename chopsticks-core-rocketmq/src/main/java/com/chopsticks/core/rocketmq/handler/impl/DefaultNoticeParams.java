package com.chopsticks.core.rocketmq.handler.impl;

import com.chopsticks.core.rocketmq.handler.BaseNoticeParams;

public class DefaultNoticeParams extends BaseNoticeParams{

	public DefaultNoticeParams(String topic, String tag, byte[] body) {
		super(topic, tag, body);
	}
	
}
