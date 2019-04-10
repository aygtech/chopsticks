package com.chopsticks.common.concurrent.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.chopsticks.common.concurrent.PromiseListener;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class DefaultTimeoutPromise<V> extends DefaultPromise<V> {
	
	private static final long DEFAULT_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30L);
	
	private ListenableFuture<V> timeoutFuture;
	
	private static final ScheduledExecutorService DEFAULT_PROMISE_TIMEOUT_SCHEDULE_EXECUTOR = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
																																		.setNameFormat("DefaultPromiseTimeoutScheduleExecutor-%d")
																																		.setDaemon(true)
																																		.build());
	
	public DefaultTimeoutPromise() {
		super();
		timeoutFuture = Futures.withTimeout(this, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, DEFAULT_PROMISE_TIMEOUT_SCHEDULE_EXECUTOR);
	}
	
	public DefaultTimeoutPromise(long timeout, TimeUnit timeoutUnit) {
		super();
		timeoutFuture = Futures.withTimeout(this, timeout, timeoutUnit, DEFAULT_PROMISE_TIMEOUT_SCHEDULE_EXECUTOR);
	}
	
	@Override
	public void addListener(PromiseListener<? super V> listener) {
		this.addListener(listener, DEFAULT_PROMISE_LISTENER_EXECUTOR);
	}
	
	@Override
	public void addListener(PromiseListener<? super V> listener, Executor executor) {
		Futures.addCallback(timeoutFuture, new DefaultPromiseListener<V>(listener), executor);
	}
	
}
