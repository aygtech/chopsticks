package com.chopsticks.core.rocketmq.modern.exception;

import com.chopsticks.core.rocketmq.exception.DefaultCoreException;

public class ModernCoreException extends DefaultCoreException {

	private static final long serialVersionUID = 1L;
	
	public static final int MODERN_INVOKE_METHOD_NOT_FOUND = 20000;
	public static final int MODERN_NOTICE_EXECUTE_ERROR = 20001;
	public static final int MODERN_INVOKE_EXECUTE_ERROR = 20002;
	public static final int UNSUPPORT_ARRAY_ARGUMENTS = 20003;
	public static final int UNSUPPORT_ARRAY_RESULT = 20004;
	public static final int INVOKE_RETURN_TYPE_NOT_MATCH = 20005;
	

	public ModernCoreException() {
		super();
	}

	public ModernCoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModernCoreException(String message) {
		super(message);
	}

	public ModernCoreException(Throwable cause) {
		super(cause);
	}

	public ModernCoreException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
}
