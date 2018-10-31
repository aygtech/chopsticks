package com.chopsticks.core.rocketmq.caller.impl;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;

import com.chopsticks.core.concurrent.impl.GuavaTimeoutPromise;
import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;

class SingleInvokeSendCallback implements SendCallback {
	
	private GuavaTimeoutPromise<BaseInvokeResult> promise;
	
	SingleInvokeSendCallback(GuavaTimeoutPromise<BaseInvokeResult> promise) {
		this.promise = promise;
	}

	@Override
	public void onSuccess(SendResult sendResult) {
		if(sendResult.getSendStatus() != SendStatus.SEND_OK) {
			promise.setException(new RuntimeException(sendResult.getSendStatus().name()));
		}
	}

	@Override
	public void onException(Throwable e) {
		promise.setException(e);
	}

}
