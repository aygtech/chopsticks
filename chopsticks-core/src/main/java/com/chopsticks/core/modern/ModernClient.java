package com.chopsticks.core.modern;

import java.util.Map;

import com.chopsticks.core.modern.caller.ExtBean;
import com.chopsticks.core.modern.caller.NoticeBean;

public interface ModernClient{
	
	public void register(Map<Class<?>, Object> beans);
	
	public <T> T getBean(Class<T> clazz);
	
	public NoticeBean getNoticeBean(Class<?> clazz);
	
	public ExtBean getExtBean(String clazzName);
	
	public void start();
	
	public void shutdown();
	
}
