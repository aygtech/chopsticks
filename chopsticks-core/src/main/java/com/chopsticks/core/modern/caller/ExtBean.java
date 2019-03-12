package com.chopsticks.core.modern.caller;

import java.util.concurrent.TimeUnit;

import com.chopsticks.common.concurrent.Promise;
import com.chopsticks.core.caller.InvokeResult;
import com.chopsticks.core.caller.NoticeResult;

public interface ExtBean {
	
	/**
	 * 阻塞的同步调用
	 * @param cmd
	 * @return
	 */
	public InvokeResult invoke(ModernInvokeCommand cmd);
	/**
	 * 非阻塞同步调用
	 * @param cmd
	 * @return
	 */
	public Promise<InvokeResult> asyncInvoke(ModernInvokeCommand cmd);
	/**
	 * 可设置超时的阻塞同步调用
	 * @param cmd
	 * @param timeout
	 * @param timeoutUnit
	 * @return
	 */
	public InvokeResult invoke(ModernInvokeCommand cmd, long timeout, TimeUnit timeoutUnit);
	/**
	 * 可设置超时的非阻塞同步调用
	 * @param cmd
	 * @param timeout
	 * @param timeoutUnit
	 * @return
	 */
	public Promise<InvokeResult> asyncInvoke(ModernInvokeCommand cmd, long timeout, TimeUnit timeoutUnit);
	/**
	 * 阻塞的异步调用
	 * @param cmd
	 * @return
	 */
	public NoticeResult notice(ModernNoticeCommand cmd);
	/**
	 * 非阻塞的异步调用
	 * @param cmd
	 * @return
	 */
	public Promise<NoticeResult> asyncNotice(ModernNoticeCommand cmd);
	/**
	 * 阻塞的顺序异步调用
	 * @param cmd
	 * @param orderKey
	 * @return
	 */
	public NoticeResult notice(ModernNoticeCommand cmd, Object orderKey);
	/**
	 * 非阻塞的顺序异步调用
	 * @param cmd
	 * @param orderKey
	 * @return
	 */
	public Promise<NoticeResult> asyncNotice(ModernNoticeCommand cmd, Object orderKey);
	
	/**
	 * 阻塞的延迟异步调用
	 * @param cmd
	 * @param delay
	 * @param delayTimeUnit
	 * @return
	 */
	public NoticeResult notice(ModernNoticeCommand cmd, long delay, TimeUnit delayTimeUnit);
	/**
	 * 非阻塞的延迟异步调用
	 * @param cmd
	 * @param delay
	 * @param delayTimeUnit
	 * @return
	 */
	public Promise<NoticeResult> asyncNotice(ModernNoticeCommand cmd, Long delay, TimeUnit delayTimeUnit);
}
