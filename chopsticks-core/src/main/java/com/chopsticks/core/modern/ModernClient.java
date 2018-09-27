package com.chopsticks.core.modern;

import java.util.Map;

import com.chopsticks.core.modern.caller.ExtBean;

public interface ModernClient{
	
	public void register(Map<Class<?>, Object> beans);
	
	public <T> T getBean(Class<T> clazz);
	
	public ExtBean getExtBean(Class<?> clazz);
	
	public void start();
	
	public void shutdown();
	
}
