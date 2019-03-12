package com.chopsticks.common.concurrent;

public interface PromiseListener<V> {
	
	public void onSuccess(V result);

	public void onFailure(Throwable t);
}
