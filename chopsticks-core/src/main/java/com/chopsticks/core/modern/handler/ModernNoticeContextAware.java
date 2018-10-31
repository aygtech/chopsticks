package com.chopsticks.core.modern.handler;

public interface ModernNoticeContextAware<T extends ModernNoticeContextHolder<?>> {
	
	public void setNoticeContextHolder(T holder);
}
