package com.chopsticks.core.caller;

import java.util.concurrent.TimeUnit;

import com.chopsticks.common.concurrent.Promise;

public interface Caller {

	public InvokeResult invoke(InvokeCommand cmd);

	public InvokeResult invoke(InvokeCommand cmd, long timeout, TimeUnit timeoutUnit);

	public Promise<? extends InvokeResult> asyncInvoke(InvokeCommand cmd);

	public Promise<? extends InvokeResult> asyncInvoke(InvokeCommand cmd, long timeout, TimeUnit timeoutUnit);
	
	public NoticeResult notice(NoticeCommand cmd);
	
	public NoticeResult notice(NoticeCommand cmd, Object orderKey);
	
	public NoticeResult notice(NoticeCommand cmd, long delay, TimeUnit delayTimeUnit);
	
	public Promise<? extends NoticeResult> asyncNotice(NoticeCommand cmd);
	
	public Promise<? extends NoticeResult> asyncNotice(NoticeCommand cmd, Object orderKey);
	
	public Promise<? extends NoticeResult> asyncNotice(NoticeCommand cmd, long delay, TimeUnit delayTimeUnit);
	
	public void start();
	
	public void shutdown();

}
