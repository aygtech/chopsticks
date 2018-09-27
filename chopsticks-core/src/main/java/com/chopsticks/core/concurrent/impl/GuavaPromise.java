package com.chopsticks.core.concurrent.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.chopsticks.core.concurrent.Promise;
import com.chopsticks.core.concurrent.PromiseListener;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class GuavaPromise<V> extends AbstractFuture<V> implements Promise<V> {
	
	private static final ThreadPoolExecutor DEFAULT_PROMISE_LISTENER_EXECUTOR = new ThreadPoolExecutor(
																						0
																						, Integer.MAX_VALUE
																						, 60L
																						, TimeUnit.SECONDS
																						, new SynchronousQueue<Runnable>()
																						, new ThreadFactoryBuilder()
																								.setNameFormat("DefaultPromiseListenerExecutor-%d")
																								.setDaemon(true)
																								.build());
	
	@Override
	public boolean set(V value) {
		return super.set(value);
	}
	@Override
	public boolean setException(Throwable throwable) {
		return super.setException(throwable);
	}
	
	
	@Override
	public void addListener(PromiseListener<? super V> listener) {
		addListener(listener, DEFAULT_PROMISE_LISTENER_EXECUTOR);
	}
	

	@Override
	public void addListener(PromiseListener<? super V> listener, Executor executor) {
		Futures.addCallback(this, new GuavaPromiseListener<V>(listener), executor);
	}
}
