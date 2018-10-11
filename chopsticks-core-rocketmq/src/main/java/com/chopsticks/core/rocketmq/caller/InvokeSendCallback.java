package com.chopsticks.core.rocketmq.caller;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;

import com.chopsticks.core.concurrent.impl.GuavaTimeoutPromise;

class InvokeSendCallback implements SendCallback {
	
	private GuavaTimeoutPromise<BaseInvokeResult> promise;
	
	InvokeSendCallback(GuavaTimeoutPromise<BaseInvokeResult> promise) {
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
