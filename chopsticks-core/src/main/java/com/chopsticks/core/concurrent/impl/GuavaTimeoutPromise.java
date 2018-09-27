package com.chopsticks.core.concurrent.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.chopsticks.core.concurrent.PromiseListener;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class GuavaTimeoutPromise<V> extends GuavaPromise<V> {
	
	private ListenableFuture<V> timeoutFuture;
	
	private static final ScheduledExecutorService DEFAULT_PROMISE_TIMEOUT_SCHEDULE_EXECUTOR = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
																																		.setNameFormat("DefaultPromiseTimeoutScheduleExecutor-%d")
																																		.setDaemon(true)
																																		.build());
	
	public GuavaTimeoutPromise(long timeout, TimeUnit timeoutUnit) {
		super();
		timeoutFuture = Futures.withTimeout(this, timeout, timeoutUnit, DEFAULT_PROMISE_TIMEOUT_SCHEDULE_EXECUTOR);
	}


	@Override
	public void addListener(PromiseListener<? super V> listener, Executor executor) {
		Futures.addCallback(timeoutFuture, new GuavaPromiseListener<V>(listener), executor);
	}
}
