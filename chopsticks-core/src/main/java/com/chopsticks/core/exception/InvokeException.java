package com.chopsticks.core.exception;

public class InvokeException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public InvokeException(String message) {
		super(message);
	}
	
	public InvokeException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
