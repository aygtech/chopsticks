package com.chopsticks.core.rocketmq.modern.handler;

import java.util.Map;

import com.chopsticks.core.modern.handler.ModernNoticeContext;

public class ModernContextHolder {
	
	private static final ThreadLocal<ModernNoticeContext> NOTICE_CONTEXT = new ThreadLocal<ModernNoticeContext>();
	private static final ThreadLocal<Map<String, String>> EXT_PARAMS = new ThreadLocal<Map<String,String>>();
	
	public static void setNoticeContext(ModernNoticeContext ctx) {
		NOTICE_CONTEXT.set(ctx);
	}
	
	public static ModernNoticeContext getNoticeContext() {
		return NOTICE_CONTEXT.get();
	}
	
	public static void remove() {
		NOTICE_CONTEXT.remove();
		EXT_PARAMS.remove();
	}
	public static void setExtParams(Map<String, String> extParams) {
		EXT_PARAMS.set(extParams);
	}
	public static Map<String, String> getExtParams() {
		return EXT_PARAMS.get();
	}
	
}
