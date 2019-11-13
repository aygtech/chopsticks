package com.chopsticks.core.rocketmq.caller;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageExt;

import com.chopsticks.core.caller.NoticeResult;

public abstract class BaseNoticeResult extends BaseResult implements NoticeResult {
	
	private String id;
	private String originId;
	private String transactionId;
	private SendResult sendResult;
	private MessageExt messageExt;
	
	public BaseNoticeResult(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOriginId() {
		return originId;
	}
	public void setOriginId(String originId) {
		this.originId = originId;
	}
	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public SendResult getSendResult() {
		return sendResult;
	}

	public void setSendResult(SendResult sendResult) {
		this.sendResult = sendResult;
	}

	public MessageExt getMessageExt() {
		return messageExt;
	}

	public void setMessageExt(MessageExt messageExt) {
		this.messageExt = messageExt;
	}
	
}
