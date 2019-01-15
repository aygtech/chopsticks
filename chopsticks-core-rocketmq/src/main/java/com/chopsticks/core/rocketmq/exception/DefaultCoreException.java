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
	public static final int CANNOT_FIND_NOTICE_HANDLER = 10010;
	public static final int CANNOT_FIND_ORDERED_NOTICE_HANDLER = 10011;
	public static final int CANNOT_FIND_DELAY_NOTICE_HANDLER = 10012;
	public static final int CANNOT_FIND_INVOKE_HANDLER = 10013;
	public static final int INVOKE_BEFORE_PROCESS_TIMEOUT = 10014;
	public static final int INVOKE_PROCESS_TIMEOUT = 10015;
	public static final int INVOKE_REC_ERROR = 10016;
	public static final int INVOKE_REQUEST_NULL = 10017;
	public static final int INVOKE_EXECUTE_ERROR = 10018;
	public static final int TEST_INVOKE_CLIENT_NAME_SERVER_CONNECTION_ERROR = 10019;
	public static final int TEST_INVOKE_CLIENT_NO_NAME_SERVER_ERROR = 10020;
	public static final int TEST_CALLER_NAME_SERVER_CONNECTION_ERROR = 10021;
	public static final int TEST_CALLER_NO_NAME_SERVER_ERROR = 10022;
	public static final int INVOKE_EXECUTOR_NOT_FOUND = 10023;
	public static final int INVOKE_CONSUMER_START_TIMEOUT = 10024;

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
