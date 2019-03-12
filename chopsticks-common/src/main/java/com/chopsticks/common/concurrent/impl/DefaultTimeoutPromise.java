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
	
	private ListenableFuture<V> timeoutFuture;
	
	private static final ScheduledExecutorService DEFAULT_PROMISE_TIMEOUT_SCHEDULE_EXECUTOR = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
																																		.setNameFormat("DefaultPromiseTimeoutScheduleExecutor-%d")
																																		.setDaemon(true)
																																		.build());
	
	public DefaultTimeoutPromise() {
		super();
	}
	
	public DefaultTimeoutPromise(long timeout, TimeUnit timeoutUnit) {
		super();
		timeoutFuture = Futures.withTimeout(this, timeout, timeoutUnit, DEFAULT_PROMISE_TIMEOUT_SCHEDULE_EXECUTOR);
	}

	@Override
	public void addListener(PromiseListener<? super V> listener, Executor executor) {
		if(timeoutFuture != null) {
			Futures.addCallback(timeoutFuture, new DefaultPromiseListener<V>(listener), executor);
		}else {
			super.addListener(listener, executor);
		}
	}
	
}
