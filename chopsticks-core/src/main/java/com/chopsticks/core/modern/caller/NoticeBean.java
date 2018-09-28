package com.chopsticks.core.modern.caller;

import java.util.concurrent.TimeUnit;

import com.chopsticks.core.caller.NoticeResult;

public interface NoticeBean {
	
	public NoticeResult notice(NoticeCommand cmd);
	public NoticeResult notice(NoticeCommand cmd, Object orderKey);
	public NoticeResult notice(NoticeCommand cmd, Long delay, TimeUnit delayTimeUnit);
}
