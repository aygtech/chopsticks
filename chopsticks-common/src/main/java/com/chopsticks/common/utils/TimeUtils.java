package com.chopsticks.common.utils;

import java.text.SimpleDateFormat;

public class TimeUtils {
	
	private static final ThreadLocal<SimpleDateFormat> yyyyMMddHHmmssSSS = new ThreadLocal<SimpleDateFormat>() {
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		} 
	};
	
	private static final ThreadLocal<SimpleDateFormat> MMddHH = new ThreadLocal<SimpleDateFormat>() {
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("MMddHH");
		} 
	};
	
	public static String yyyyMMddHHmmssSSS(long time) {
		return yyyyMMddHHmmssSSS.get().format(time);
	}
	
	public static String MMddHH(long time) {
		return MMddHH.get().format(time);
	}
}
