package com.chopsticks.core.concurrent;

public interface PromiseListener<V> {
	
	public void onSuccess(V result);

	public void onFailure(Throwable t);
}
