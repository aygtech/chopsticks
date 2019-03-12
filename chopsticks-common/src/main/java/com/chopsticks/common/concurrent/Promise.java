package com.chopsticks.common.concurrent;

import java.util.concurrent.Executor;

public interface Promise<V> extends java.util.concurrent.Future<V> {
	
	public void addListener(PromiseListener<? super V> listener);
	
	public void addListener(PromiseListener<? super V> listener, Executor executor);
	
}
