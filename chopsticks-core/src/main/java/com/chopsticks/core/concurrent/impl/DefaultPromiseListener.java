package com.chopsticks.core.concurrent.impl;

import com.chopsticks.core.concurrent.PromiseListener;

class DefaultPromiseListener<V> implements com.google.common.util.concurrent.FutureCallback<V> {

	private PromiseListener<? super V> callback;
	
	DefaultPromiseListener(PromiseListener<? super V> callback) {
		this.callback = callback;
	}
	
	@Override
	public void onSuccess(V result) {
		callback.onSuccess(result);
	}
	
	@Override
	public void onFailure(Throwable t) {
		callback.onFailure(t);
	}
}
