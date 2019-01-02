package com.chopsticks.core.exception;

public class CoreException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	private int code;

	public CoreException() {
		super();
	}

	public CoreException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public CoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public CoreException(String message) {
		super(message);
	}

	public CoreException(Throwable cause) {
		super(cause);
	}

	public int getCode() {
		return code;
	}

	public CoreException setCode(int code) {
		this.code = code;
		return this;
	}
	
	@Override
	public String getMessage() {
		return String.format("code : %s, msg : %s", getCode(), super.getMessage());
	}
}
