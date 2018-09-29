package com.chopsticks.core.rockctmq.modern.caller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.google.common.base.Strings;

public abstract class BaseProxy implements InvocationHandler {
	
	protected String getTopic(Class<?> clazz) {
		Endpoint endPoint = clazz.getDeclaredAnnotation(Endpoint.class);
		if(endPoint != null && !Strings.isNullOrEmpty(endPoint.value())) {
			return endPoint.value();
		}
		return clazz.getName();
	}
	
	protected String getMethod(Method method) {
		Endpoint endPoint = method.getDeclaredAnnotation(Endpoint.class);
		if(endPoint != null && !Strings.isNullOrEmpty(endPoint.value())) {
			return endPoint.value();
		}
		return method.getName();
	}
}
