package com.chopsticks.core.rocketmq.modern;

import com.chopsticks.common.utils.Reflect;

public class ModernClientProxy {
	
	
	public <T> T invokeExecuteProxy(Object obj, String method, Object... args) throws Throwable{
		return Reflect.on(obj).call(method, args).get();
	}
	public void noticeExecuteProxy(Object obj, String method, Object... args) throws Throwable{
		Reflect.on(obj).call(method, args).get();
	}
}
