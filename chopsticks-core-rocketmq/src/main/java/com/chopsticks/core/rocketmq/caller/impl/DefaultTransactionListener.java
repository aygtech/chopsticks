package com.chopsticks.core.rocketmq.caller.impl;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.rocketmq.caller.TransactionChecker;
import com.chopsticks.core.rocketmq.caller.TransactionState;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.google.common.base.Preconditions;

public class DefaultTransactionListener implements TransactionListener {
	
	private static final Logger log = LoggerFactory.getLogger(DefaultTransactionListener.class);
	private TransactionChecker checker;
	
	public DefaultTransactionListener(TransactionChecker checker) {
		this.checker = Preconditions.checkNotNull(checker);
	}

	@Override
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		return LocalTransactionState.UNKNOW;
	}

	@Override
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		// return msgid not src id
		DefaultNoticeResult ret = new DefaultNoticeResult(msg.getTransactionId());
		ret.setTransactionId(msg.getTransactionId());
		ret.setMessageExt(msg);
		TransactionState state = null;
		try {
			state = checker.check(ret);
		}catch (Throwable e) {
			log.error("user transaction check error", new DefaultCoreException(e).setCode(DefaultCoreException.USER_TRANSACTION_CHECK_ERROR));
			state = TransactionState.UNKNOW;
		}
		switch(state) {
			case COMMIT : 
				return LocalTransactionState.COMMIT_MESSAGE;
			case ROLLBACK : 
				return LocalTransactionState.ROLLBACK_MESSAGE;
			case UNKNOW : 
				return LocalTransactionState.UNKNOW;
			default : 
				log.warn("noticeId : {}, state : {} undefined", msg.getMsgId(), state);
				return LocalTransactionState.UNKNOW;
		}
	}

	

}
