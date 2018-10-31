package com.chopsticks.core.rocketmq.modern.handler;

import com.chopsticks.core.modern.handler.ModernNoticeContext;
import com.chopsticks.core.rocketmq.handler.BaseNoticeContext;

public class DefaultModerNoticeContext extends BaseNoticeContext implements ModernNoticeContext {
	
	public DefaultModerNoticeContext(BaseNoticeContext ctx) {
		super(ctx);
	}
	
	public DefaultModerNoticeContext(String id, String originId, int reconsumeTimes) {
		super(id, originId, reconsumeTimes);
	}

}
