package com.chopsticks.core.rocketmq.caller;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.concurrent.PromiseListener;
import com.chopsticks.core.concurrent.impl.GuavaPromise;

class CallerTimoutPromiseListener implements PromiseListener<BaseInvokeResult> {
	
	private static final Logger log = LoggerFactory.getLogger(CallerTimoutPromiseListener.class);
	
	private Map<String, GuavaPromise<BaseInvokeResult>> callerInvokePromiseMap;
	private String reqId;
	
	public CallerTimoutPromiseListener(Map<String, GuavaPromise<BaseInvokeResult>> callerInvokePromiseMap
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
			GuavaPromise<BaseInvokeResult> promise = callerInvokePromiseMap.remove(reqId);
			log.error("timeout remove promise, reqId : {}, promise : {}", reqId, promise);
		}
	}

}
