package com.chopsticks.core.rocketmq.caller;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.common.concurrent.PromiseListener;

class CallerNoticeTimeoutPromiseListener implements PromiseListener<BaseNoticeResult>{
	
	private static final Logger log = LoggerFactory.getLogger(CallerNoticeTimeoutPromiseListener.class);
	
	private NoticeSendCallback callback;
	
	public CallerNoticeTimeoutPromiseListener(NoticeSendCallback callback) {
		this.callback = callback;
	}
	
	@Override
	public void onSuccess(BaseNoticeResult result) {
		
	}

	@Override
	public void onFailure(Throwable t) {
		if(t instanceof TimeoutException || t instanceof CancellationException) {
			if(!callback.isDone()) {
				log.error("notice timeout, callback not done");
			}
		}
	}

}
