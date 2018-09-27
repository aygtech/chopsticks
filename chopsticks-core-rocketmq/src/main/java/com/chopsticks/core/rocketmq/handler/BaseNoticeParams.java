package com.chopsticks.core.rocketmq.handler;

import com.chopsticks.core.handler.NoticeParams;

public abstract class BaseNoticeParams extends BaseHandlerParams implements NoticeParams {
	
	public BaseNoticeParams(String topic, String tag, byte[] body) {
		super(topic, tag, body);
	}
}
