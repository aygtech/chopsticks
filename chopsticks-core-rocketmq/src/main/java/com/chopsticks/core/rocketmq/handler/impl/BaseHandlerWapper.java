package com.chopsticks.core.rocketmq.handler.impl;

import com.chopsticks.core.handler.Handler;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.handler.InvokeContext;
import com.chopsticks.core.handler.InvokeParams;
import com.chopsticks.core.handler.NoticeContext;
import com.chopsticks.core.handler.NoticeParams;
import com.chopsticks.core.rocketmq.handler.BaseHandler;

public class BaseHandlerWapper extends BaseHandler {
	
	private Handler handler;

	public BaseHandlerWapper(Handler handler, String topic, String tag) {
		super(topic, tag);
		this.handler = handler;
	}

	@Override
	public HandlerResult invoke(InvokeParams params, InvokeContext ctx) {
		return handler.invoke(params, ctx);
	}

	@Override
	public void notice(NoticeParams params, NoticeContext ctx) {
		handler.notice(params, ctx);
	}

	@Override
	public boolean isSupportInvoke() {
		return handler.isSupportInvoke();
	}

	@Override
	public boolean isSupportNotice() {
		return handler.isSupportNotice();
	}

	@Override
	public boolean isSupportDelayNotice() {
		return handler.isSupportDelayNotice();
	}

	@Override
	public boolean isSupportOrderedNotice() {
		return handler.isSupportOrderedNotice();
	}

}
