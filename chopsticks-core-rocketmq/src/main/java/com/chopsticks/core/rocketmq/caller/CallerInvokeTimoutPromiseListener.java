package com.chopsticks.core.rocketmq.caller;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.concurrent.PromiseListener;
import com.chopsticks.core.concurrent.impl.GuavaTimeoutPromise;

class CallerInvokeTimoutPromiseListener implements PromiseListener<BaseInvokeResult> {
	
	private static final Logger log = LoggerFactory.getLogger(CallerInvokeTimoutPromiseListener.class);
	
	private Map<String, GuavaTimeoutPromise<BaseInvokeResult>> callerInvokePromiseMap;
	private String reqId;
	
	public CallerInvokeTimoutPromiseListener(Map<String, GuavaTimeoutPromise<BaseInvokeResult>> callerInvokePromiseMap
									, String reqId) {
		this.callerInvokePromiseMap = callerInvokePromiseMap;
		this.reqId = reqId;
	}
	
	@Override
	public void onSuccess(BaseInvokeResult result) {
	}
	
	@Override
	public void onFailure(Throwable t) {
		if(t instanceof TimeoutException || t instanceof CancellationException) {
			GuavaTimeoutPromise<BaseInvokeResult> promise = callerInvokePromiseMap.remove(reqId);
			log.trace("timeout remove promise, reqId : {}, promise : {}", reqId, promise);
		}
	}

}
