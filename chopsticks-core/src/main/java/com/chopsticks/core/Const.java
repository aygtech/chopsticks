package com.chopsticks.core;

import com.chopsticks.core.utils.Reflect;

public class Const {
	public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
	
	public static final Object getOriginObject(Object obj) {
		if(Reflect.on(obj).fields().keySet().contains("CGLIB$CALLBACK_0")) {
			return Reflect.on(obj).field("CGLIB$CALLBACK_0").field("advised").field("targetSource").field("target").get();
		}else {
			return obj;
		}
	}
}
