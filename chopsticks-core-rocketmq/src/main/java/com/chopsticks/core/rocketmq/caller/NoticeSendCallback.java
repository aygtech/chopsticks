package com.chopsticks.core.rocketmq.caller;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.concurrent.impl.GuavaPromise;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeResult;

class NoticeSendCallback implements SendCallback {
	
	private static final Logger log = LoggerFactory.getLogger(NoticeSendCallback.class);
	
	private GuavaPromise<BaseNoticeResult> noticePromise;
	
	private boolean done;
	
	NoticeSendCallback(GuavaPromise<BaseNoticeResult> noticePromise) {
		this.noticePromise = noticePromise;
	}
	
	@Override
	public void onSuccess(SendResult sendResult) {
		setDone(true);
		if(noticePromise.isCancelled()) {
			log.error("promise is cancel, id : {}, status : {}", sendResult.getMsgId(), sendResult.getSendStatus());
			return;
		}
		if(sendResult.getSendStatus() == SendStatus.SEND_OK) {
			DefaultNoticeResult ret = new DefaultNoticeResult(sendResult.getMsgId());
			ret.setOriginId(sendResult.getOffsetMsgId());
			noticePromise.set(ret);
		}else {
			noticePromise.setException(new RuntimeException(sendResult.getSendStatus().name()));
		}
	}
	
	@Override
	public void onException(Throwable e) {
		setDone(true);
		noticePromise.setException(e);
	}
	
	private void setDone(boolean done) {
		this.done = done;
	}
	public boolean isDone() {
		return done;
	}
}
