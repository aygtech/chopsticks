package com.chopsticks.core.rocketmq.caller;

public interface TransactionChecker {
	
	public TransactionState check(BaseNoticeResult noticeResult);
}
