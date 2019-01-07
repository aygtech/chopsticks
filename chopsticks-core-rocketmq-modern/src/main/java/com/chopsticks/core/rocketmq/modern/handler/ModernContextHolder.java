package com.chopsticks.core.rocketmq.modern.handler;

import java.util.Map;
import java.util.Set;

import com.chopsticks.core.modern.handler.ModernNoticeContext;

public class ModernContextHolder {
	
	private static final ThreadLocal<ModernNoticeContext> NOTICE_CONTEXT = new ThreadLocal<ModernNoticeContext>();
	private static final ThreadLocal<Map<String, String>> EXT_PARAMS = new ThreadLocal<Map<String,String>>();
	private static final ThreadLocal<Set<String>> TRACE_NOS = new ThreadLocal<Set<String>>();
	
	public static void setNoticeContext(ModernNoticeContext ctx) {
		NOTICE_CONTEXT.set(ctx);
	}
	
	public static ModernNoticeContext getNoticeContext() {
		return NOTICE_CONTEXT.get();
	}
	
	public static void remove() {
		NOTICE_CONTEXT.remove();
		EXT_PARAMS.remove();
		TRACE_NOS.remove();
	}
	public static void setExtParams(Map<String, String> extParams) {
		EXT_PARAMS.set(extParams);
	}
	public static Map<String, String> getExtParams() {
		return EXT_PARAMS.get();
	}
	public static void setTraceNos(Set<String> traceNos) {
		TRACE_NOS.set(traceNos);
	}
	public static Set<String> getTraceNos(){
		return TRACE_NOS.get();
	}
	
}
