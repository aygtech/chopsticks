package com.chopsticks.core.rocketmq.exception;

import com.chopsticks.core.exception.CoreException;

public class DefaultCoreException extends CoreException {

	private static final long serialVersionUID = 1L;
	
	public static final int DELAY_NOTICE_EXECUTE_FORWORD_SEND_NOT_OK = 10000;
	public static final int NOTICE_EXECUTE_FORWORD_SEND_NOT_OK = -10000;
	public static final int TEST_DELAY_NOTICE_CLIENT_BROKER_CONNECTION_ERROR = 10001;
	public static final int TEST_DELAY_NOTICE_CLIENT_NAME_SERVER_CONNECTION_ERROR = 10002;
	public static final int TEST_DELAY_NOTICE_CLIENT_ERROR = 10003;
	public static final int TEST_NOTICE_CLIENT_BROKER_CONNECTION_ERROR = 10004;
	public static final int TEST_NOTICE_CLIENT_NAME_SERVER_CONNECTION_ERROR = 10005;
	public static final int TEST_NOTICE_CLIENT_ERROR = 10006;
	public static final int TEST_ORDERED_NOTICE_CLIENT_BROKER_CONNECTION_ERROR = 10007;
	public static final int TEST_ORDERED_NOTICE_CLIENT_NAME_SERVER_CONNECTION_ERROR = 10008;
	public static final int TEST_ORDERED_NOTICE_CLIENT_ERROR = 10009;

	public DefaultCoreException() {
		super();
	}

	public DefaultCoreException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DefaultCoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public DefaultCoreException(String message) {
		super(message);
	}

	public DefaultCoreException(Throwable cause) {
		super(cause);
	}
	
}
