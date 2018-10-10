package com.chopsticks.core.modern.handler;

public interface ModernNoticeContextAware {
	
	public void setNoticeContext(ThreadLocal<ModernNoticeContext> context);
}
