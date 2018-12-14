package com.chopsticks.core.rocketmq.handler;

import com.chopsticks.core.handler.NoticeContext;

public abstract class BaseNoticeContext implements NoticeContext{
	
	private String id;
	private String originId;
	private int reconsumeTimes;
	private boolean orderedNotice;
	private boolean delayNotice;
	
	public BaseNoticeContext(BaseNoticeContext ctx) {
		this(ctx.getId(), ctx.getOriginId(), ctx.getReconsumeTimes(), ctx.isOrderedNotice(), ctx.isDelayNotice());
	}
	
	public BaseNoticeContext(String id, String originId, int reconsumeTimes) {
		this(id, originId, reconsumeTimes, false, false);
	}
	public BaseNoticeContext(String id, String originId, int reconsumeTimes, boolean orderedNotice) {
		this(id, originId, reconsumeTimes, orderedNotice, false);
	}
	public BaseNoticeContext(String id, String originId, int reconsumeTimes, boolean orderedNotice,
			boolean delayNotice) {
		super();
		this.id = id;
		this.originId = originId;
		this.reconsumeTimes = reconsumeTimes;
		this.orderedNotice = orderedNotice;
		this.delayNotice = delayNotice;
	}

	public int getReconsumeTimes() {
		return reconsumeTimes;
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
}
