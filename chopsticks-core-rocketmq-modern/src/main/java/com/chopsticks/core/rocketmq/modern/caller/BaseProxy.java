package com.chopsticks.core.rocketmq.modern.caller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

import com.alibaba.fastjson.annotation.JSONType;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;

public abstract class BaseProxy implements InvocationHandler {
	private static final String DEFAULT_TRACK_NO_PREFIX = "DEFAULT_";
	
	protected String getTopic(Class<?> clazz) {
		JSONType jsonType = FluentIterable.from(clazz.getDeclaredAnnotations())
										  .filter(JSONType.class)
										  .first()
										  .orNull();
		if(jsonType != null && !Strings.isNullOrEmpty(jsonType.typeName())) {
			return jsonType.typeName();
		}
		return clazz.getName();
	}
	
	protected String getMethod(Method method) {
		return method.getName();
	}
	
	protected String getDefaultTrackNo() {
		return DEFAULT_TRACK_NO_PREFIX + UUID.randomUUID().toString();
	}
}
