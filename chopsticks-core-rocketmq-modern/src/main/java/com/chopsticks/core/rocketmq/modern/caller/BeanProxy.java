package com.chopsticks.core.rocketmq.modern.caller;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.chopsticks.core.caller.InvokeResult;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeCommand;
import com.chopsticks.core.rocketmq.modern.DefaultModernClient;
import com.chopsticks.core.rocketmq.modern.handler.ModernContextHolder;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class BeanProxy extends BaseProxy {

	private Class<?> clazz;

	public BeanProxy(Class<?> clazz, DefaultModernClient client) {
		super(client);
		this.clazz = clazz;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return innerInvoke(proxy, method, args);
	}

	@Override
	public Object innerInvoke(Object proxy, Method method, Object[] args) throws Throwable {
		byte[] body = buildBody(args);
		DefaultInvokeCommand invokeCmd = new DefaultInvokeCommand(getTopic(clazz), getMethod(method), body);
		if(ModernContextHolder.getTraceNos() == null || ModernContextHolder.getTraceNos().isEmpty()) {
			invokeCmd.setTraceNos(Sets.newHashSet(getDefaultTraceNo()));
		}
		Map<String, String> extParams = Maps.newHashMap(getExtParams());
		invokeCmd.setExtParams(extParams);
		
		InvokeResult result = client.invoke(invokeCmd);
		Class<?> returnType = method.getReturnType();
		Object ret = null;
		if (returnType != void.class && result.getBody() != null && result.getBody().length > 0) {
			String strBody;
			try {
				strBody = new String(result.getBody(), Charsets.UTF_8);
			} catch (Throwable e) {
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
			if (Collection.class.isAssignableFrom(returnType)) {
				if (ParameterizedType.class.isAssignableFrom(method.getGenericReturnType().getClass())) {
					Class<?> clazz = (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
					ret = JSON.parseArray(strBody, clazz);
				} else {
					ret = JSON.parseArray(strBody);
				}
			} else if(Map.class.isAssignableFrom(returnType)){
				if (ParameterizedType.class.isAssignableFrom(method.getGenericReturnType().getClass())) {
					ret = JSON.parseObject(strBody, method.getGenericReturnType());
				} else {
					ret = JSON.parseObject(strBody);
				}
			} else {
				ret = JSON.parseObject(strBody, returnType);
			}
		}
		return ret;
	}
	
}
