package com.chopsticks.core.rockctmq.modern.caller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;

public abstract class BaseProxy implements InvocationHandler {
	
	protected String getTopic(Class<?> clazz) {
		Endpoint endPoint = FluentIterable.from(clazz.getDeclaredAnnotations())
										  .filter(Endpoint.class)
										  .first()
										  .orNull();
		if(endPoint != null && !Strings.isNullOrEmpty(endPoint.value())) {
			return endPoint.value();
		}
		return clazz.getName();
	}
	
	protected String getMethod(Method method) {
		Endpoint endPoint = FluentIterable.from(method.getDeclaredAnnotations())
										  .filter(Endpoint.class)
										  .first()
										  .orNull();
		if(endPoint != null && !Strings.isNullOrEmpty(endPoint.value())) {
			return endPoint.value();
		}
		return method.getName();
	}
}
