package com.chopsticks.core.rocketmq.modern.handler;

import com.chopsticks.core.modern.handler.ModernNoticeContext;

public class ModernContextHolder {
	
	private static final ThreadLocal<ModernNoticeContext> NOTICE_CONTEXT = new ThreadLocal<ModernNoticeContext>();
	
	public static void setNoticeContext(ModernNoticeContext ctx) {
		NOTICE_CONTEXT.set(ctx);
	}
	
	public static ModernNoticeContext getNoticeContext() {
		return NOTICE_CONTEXT.get();
	}
	
	public static void remove() {
		NOTICE_CONTEXT.remove();
	}
}
