package com.chopsticks.core.rocketmq;

import com.chopsticks.core.handler.InvokeContext;
import com.chopsticks.core.handler.InvokeParams;
import com.chopsticks.core.handler.NoticeContext;
import com.chopsticks.core.handler.NoticeParams;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.chopsticks.core.rocketmq.handler.BaseHandlerResult;
import com.chopsticks.core.rocketmq.handler.impl.DefaultHandlerResult;

public class EmptyHandler extends BaseHandler{
	

	public EmptyHandler(String topic, String tag) {
		super(topic, tag);
	}

	@Override
	public BaseHandlerResult invoke(InvokeParams params, InvokeContext ctx) {
		return new DefaultHandlerResult();
	}

	@Override
	public void notice(NoticeParams params, NoticeContext ctx) {
	}
}
