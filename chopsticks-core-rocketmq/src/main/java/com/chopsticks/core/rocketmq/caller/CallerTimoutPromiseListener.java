package com.chopsticks.core.rocketmq.caller;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import com.chopsticks.core.concurrent.PromiseListener;
import com.chopsticks.core.concurrent.impl.GuavaPromise;

class CallerTimoutPromiseListener implements PromiseListener<BaseInvokeResult> {
	
	private Map<String, GuavaPromise<BaseInvokeResult>> callerInvokePromiseMap;
	private String msgId;
	
	public CallerTimoutPromiseListener(Map<String, GuavaPromise<BaseInvokeResult>> callerInvokePromiseMap
									, String msgId) {
		this.callerInvokePromiseMap = callerInvokePromiseMap;
		this.msgId = msgId;
	}
	
	@Override
	public void onSuccess(BaseInvokeResult result) {
	}
	
	@Override
	public void onFailure(Throwable t) {
		if(t instanceof TimeoutException || t instanceof CancellationException) {
			callerInvokePromiseMap.remove(msgId);
		}
	}

}
