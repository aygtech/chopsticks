package com.chopsticks.core.rocketmq.caller;

import com.chopsticks.core.caller.NoticeCommand;

public abstract class BaseNoticeCommand extends BaseCommand implements NoticeCommand{
	
	private boolean transaction;

	public boolean isTransaction() {
		return transaction;
	}

	public BaseNoticeCommand setTransaction(boolean transaction) {
		this.transaction = transaction;
		return this;
	}


	public BaseNoticeCommand(String topic, String tag, byte[] body) {
		super(topic, tag, body);
	}
	
}
