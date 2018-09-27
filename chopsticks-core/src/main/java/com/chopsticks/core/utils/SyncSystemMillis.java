package com.chopsticks.core.utils;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class SyncSystemMillis {
	
	private static final ScheduledExecutorService syncService = new ScheduledThreadPoolExecutor(1
																							, new ThreadFactoryBuilder()
																									.setNameFormat("SyncSystemMillis-%d")
																									.setDaemon(true)
																									.build());
	
	private volatile long now;
	
	public SyncSystemMillis(final long updateMillis) {
		now = System.currentTimeMillis();
		syncService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				now += updateMillis;
			}
		}, 0, updateMillis, TimeUnit.MILLISECONDS);
	}
	
	public long getNow() {
		return now;
	}
	
	public void setNow(long now) {
		this.now = now;
	}
}
