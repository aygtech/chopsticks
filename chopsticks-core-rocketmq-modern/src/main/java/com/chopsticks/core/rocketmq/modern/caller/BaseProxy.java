package com.chopsticks.core.rocketmq.modern.caller;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.chopsticks.core.rocketmq.modern.Const;
import com.chopsticks.core.rocketmq.modern.exception.ModernCoreException;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;

public abstract class BaseProxy implements InvocationHandler {
	private static final String DEFAULT_TRACK_NO_PREFIX = "DEFAULT_";
	private Map<String, String> extParams = Maps.newHashMap();
	
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

	public static String getDefaultTrackNoPrefix() {
		return DEFAULT_TRACK_NO_PREFIX;
	}
	
	public Map<String, String> getExtParams() {
		return extParams;
	}
	
	protected byte[] buildBody(Object[] args) {
		byte[] body;
		if (args == null) {
			body = Const.EMPTY_PARAMS.getBytes(Charsets.UTF_8);
		} else {
			for(Object arg : args) {
				if(arg.getClass().isArray()) {
					throw new ModernCoreException("unsupport array arguments").setCode(ModernCoreException.UNSUPPORT_ARRAY_ARGUMENTS);
				}
			}
			body = JSON.toJSONString(args, SerializerFeature.WriteClassName).getBytes(Charsets.UTF_8);
		}
		return body;
	}

	public BaseProxy setExtParams(Map<String, String> extParams) {
		this.extParams = extParams;
		return this;
	}
}
