package com.chopsticks.core.utils;

import java.text.SimpleDateFormat;

public class TimeUtils {
	
	private static final ThreadLocal<SimpleDateFormat> yyyyMMddHHmmssSSS = new ThreadLocal<SimpleDateFormat>() {
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		} 
	};
	
	public static String yyyyMMddHHmmssSSS(long time) {
		return yyyyMMddHHmmssSSS.get().format(time);
	}
}
