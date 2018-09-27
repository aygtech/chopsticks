package com.chopsticks.core.modern.caller;

import java.util.concurrent.TimeUnit;

public interface ExtBean {
	
	public ExtNoticeResult notice(ExtNoticeCommand cmd);
	public ExtNoticeResult notice(ExtNoticeCommand cmd, Object orderKey);
	public ExtNoticeResult notice(ExtNoticeCommand cmd, Long delay, TimeUnit delayTimeUnit);
}
