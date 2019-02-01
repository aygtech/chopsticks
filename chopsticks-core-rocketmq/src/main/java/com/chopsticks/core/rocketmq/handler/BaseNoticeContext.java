package com.chopsticks.core.rocketmq.handler;

import com.chopsticks.core.handler.NoticeContext;

public abstract class BaseNoticeContext extends BaseContext implements NoticeContext{
	
	private String id;
	private String originId;
	private int retryCount;
	private boolean maxRetryCount;
	private boolean orderedNotice;
	private boolean delayNotice;
	
	public BaseNoticeContext(BaseNoticeContext ctx) {
		this(ctx.getId(), ctx.getOriginId(), ctx.getRetryCount(), ctx.isMaxRetryCount(), ctx.isOrderedNotice(), ctx.isDelayNotice());
		setExtParams(ctx.getExtParams());
		setReqTime(ctx.getReqTime());
		setTraceNos(ctx.getTraceNos());
	}
	
	public BaseNoticeContext(String id, String originId, int retryCount, boolean maxRetryCount, boolean orderedNotice, boolean delayNotice) {
		super();
		this.id = id;
		this.originId = originId;
		this.retryCount = retryCount;
		this.maxRetryCount = maxRetryCount;
		this.orderedNotice = orderedNotice;
		this.delayNotice = delayNotice;
	}

	public int getRetryCount() {
		return retryCount;
	}
	@Override
	public String getId() {
		return id;
	}
	public String getOriginId() {
		return originId;
	}
	public boolean isOrderedNotice() {
		return orderedNotice;
	}
	public boolean isDelayNotice() {
		return delayNotice;
	}
	public boolean isMaxRetryCount() {
		return maxRetryCount;
	}
}
