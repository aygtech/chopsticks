package com.chopsticks.core.rockctmq.modern.handler;

import com.chopsticks.core.modern.handler.ModernNoticeContext;
import com.chopsticks.core.rocketmq.handler.BaseNoticeContext;

public class DefaultModerNoticeContext extends BaseNoticeContext implements ModernNoticeContext {

	public DefaultModerNoticeContext(String id, String originId, int reconsumeTimes) {
		super(id, originId, reconsumeTimes);
	}
	

}
