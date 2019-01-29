package com.chopsticks.core.modern.caller;

import java.util.concurrent.TimeUnit;

import com.chopsticks.core.caller.NoticeResult;

public interface NoticeBean {
	
	public NoticeResult notice(ModernNoticeCommand cmd);
	public NoticeResult notice(ModernNoticeCommand cmd, Object orderKey);
	public NoticeResult notice(ModernNoticeCommand cmd, long delay, TimeUnit delayTimeUnit);
}
