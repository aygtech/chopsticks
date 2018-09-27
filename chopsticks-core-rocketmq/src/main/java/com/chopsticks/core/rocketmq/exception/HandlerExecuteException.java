package com.chopsticks.core.rocketmq.exception;

public class HandlerExecuteException extends RuntimeException{
	
	private static final long serialVersionUID = 1L;

	public HandlerExecuteException(String errMsg, Throwable e) {
		super(errMsg, e);
	}
}
