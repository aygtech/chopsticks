package com.chopsticks.core.rocketmq.caller;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.concurrent.PromiseListener;
import com.chopsticks.core.concurrent.impl.GuavaTimeoutPromise;
import com.chopsticks.core.utils.TimeUtils;

class CallerInvokeTimoutPromiseListener implements PromiseListener<BaseInvokeResult> {
	
	private static final Logger log = LoggerFactory.getLogger(CallerInvokeTimoutPromiseListener.class);
	
	private Map<String, GuavaTimeoutPromise<BaseInvokeResult>> callerInvokePromiseMap;
	private InvokeRequest req;
	
	public CallerInvokeTimoutPromiseListener(Map<String, GuavaTimeoutPromise<BaseInvokeResult>> callerInvokePromiseMap
									, InvokeRequest req) {
		this.callerInvokePromiseMap = callerInvokePromiseMap;
		this.req = req;
	}
	
	@Override
	public void onSuccess(BaseInvokeResult result) {
	}
	
	@Override
	public void onFailure(Throwable t) {
		if(t instanceof TimeoutException || t instanceof CancellationException) {
			GuavaTimeoutPromise<BaseInvokeResult> promise = callerInvokePromiseMap.remove(req.getReqId());
			log.error("timeout remove promise, reqId : {}, reqTime : {}, deadline : {}, promise : {}"
					, req.getReqId()
					, TimeUtils.yyyyMMddHHmmssSSS(req.getReqTime())
					, TimeUtils.yyyyMMddHHmmssSSS(req.getDeadline())
					, promise);
		}
	}

}
