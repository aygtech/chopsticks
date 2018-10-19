package com.chopsticks.core.utils;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class SyncSystemMillis {
	
	private static final ScheduledExecutorService syncService = new ScheduledThreadPoolExecutor(1
																							, new ThreadFactoryBuilder()
																									.setNameFormat("SyncSystemMillis-%d")
																									.setDaemon(true)
																									.build());
	
	private volatile long now;
	private AtomicBoolean seted = new AtomicBoolean(false);
	
	public SyncSystemMillis(final long updateMillis) {
		now = System.currentTimeMillis();
		syncService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				now += updateMillis;
			}
		}, 0L, updateMillis, TimeUnit.MILLISECONDS);
	}
	
	public long getNow() {
		return now;
	}
	
	public void setNow(long now) {
		if(seted.compareAndSet(false, true)) {
			this.now = now;
		}
	}
}
