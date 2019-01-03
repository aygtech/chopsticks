package com.chopsticks.core.modern.caller;

import java.util.concurrent.TimeUnit;

import com.chopsticks.core.caller.InvokeResult;
import com.chopsticks.core.caller.NoticeResult;

public interface ExtBean {

	public InvokeResult invoke(ModernInvokeCommand cmd);

	public InvokeResult invoke(ModernInvokeCommand cmd, long timeout, TimeUnit timeoutUnit);

	public NoticeResult notice(ModernNoticeCommand cmd);

	public NoticeResult notice(ModernNoticeCommand cmd, Object orderKey);

	public NoticeResult notice(ModernNoticeCommand cmd, Long delay, TimeUnit delayTimeUnit);
}
