package com.chopsticks.core.rocketmq.caller;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;

import com.chopsticks.core.concurrent.impl.GuavaPromise;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeResult;

class NoticeSendCallback implements SendCallback {
	
	private GuavaPromise<BaseNoticeResult> noticePromise;
	
	NoticeSendCallback(GuavaPromise<BaseNoticeResult> noticePromise) {
		this.noticePromise = noticePromise;
	}
	
	@Override
	public void onSuccess(SendResult sendResult) {
		if(sendResult.getSendStatus() == SendStatus.SEND_OK) {
			noticePromise.set(new DefaultNoticeResult(sendResult.getMsgId()));
		}else {
			noticePromise.setException(new RuntimeException(sendResult.getSendStatus().name()));
		}
	}
	
	@Override
	public void onException(Throwable e) {
		noticePromise.setException(e);
	}

}
