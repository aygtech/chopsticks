package com.chopsticks.core.rocketmq.modern.handler;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.core.exception.HandlerExecuteException;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.handler.InvokeContext;
import com.chopsticks.core.handler.InvokeParams;
import com.chopsticks.core.handler.NoticeContext;
import com.chopsticks.core.handler.NoticeParams;
import com.chopsticks.core.modern.handler.ModernNoticeContextAware;
import com.chopsticks.core.modern.handler.ModernNoticeContextHolder;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.chopsticks.core.rocketmq.handler.BaseNoticeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultHandlerResult;
import com.chopsticks.core.rocketmq.modern.Const;
import com.chopsticks.core.utils.Reflect;
import com.chopsticks.core.utils.Reflect.ReflectException;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ModernHandler extends BaseHandler{
	
	private static final Logger log = LoggerFactory.getLogger(ModernHandler.class);
	
	private Object obj;
	
	private ModernNoticeContextHolder<?> holder;
	
	private Class<?> holderCtxClazz;
	
	private static final Cache<Object, ModernNoticeContextHolder<?>> objHolder = CacheBuilder.newBuilder().build();
	
	public ModernHandler(Object obj, String topic, String tag) {
		super(topic, tag);
		this.obj = obj;
		
		if(obj instanceof ModernNoticeContextAware) {
			Class<?> typeClazz = getTypeClazz(obj);
			Object typeObj = typeClazz == obj.getClass() ? obj : typeClazz;
			for(Type type : typeClazz.getGenericInterfaces()) {
				if(ModernNoticeContextAware.class.isAssignableFrom((Class<?>)type)) {
					if(((Class<?>)type).getGenericInterfaces() != null) {
						type = ((Class<?>)type).getGenericInterfaces()[0];
					}
					final Type holderType = ((ParameterizedType)type).getActualTypeArguments()[0];
					try {
						// 一个 实现 中的 hodler 必须唯一才能保持 引用的 ThreadLocal 变量唯一
						holder = objHolder.get(typeObj, new Callable<ModernNoticeContextHolder<?>>() {
							@Override
							public ModernNoticeContextHolder<?> call() throws Exception {
								return (ModernNoticeContextHolder<?>)((Class<?>)holderType).newInstance();
							}
						});
						Reflect.on(obj).call("setNoticeContextHolder", holder);
						holderCtxClazz = (Class<?>)((ParameterizedType)((Class<?>)holderType).getGenericSuperclass()).getActualTypeArguments()[0];
						if(holderCtxClazz.isInterface()) {
							holderCtxClazz = DefaultModerNoticeContext.class;
						}
						break;
					}catch (Throwable e) {
						Throwables.throwIfUnchecked(e);
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	private Class<?> getTypeClazz(Object obj) {
		obj = com.chopsticks.core.Const.getOriginObject(obj);
		Class<?> typeClazz = obj.getClass();
		do {
			for(Type type : typeClazz.getGenericInterfaces()) {
				if(ModernNoticeContextAware.class.isAssignableFrom((Class<?>)type)) {
					return typeClazz;
				}
			}
			typeClazz = typeClazz.getSuperclass();
		}while(typeClazz != Object.class);
		throw new RuntimeException("unkown typeClazz");
	}

	@Override
	public HandlerResult invoke(InvokeParams params, InvokeContext ctx) {
		Object[] args = null;
		if(params.getBody() != null && params.getBody().length > 0) {
			String body = new String(params.getBody(), Charsets.UTF_8);
			if(!Const.EMPTY_PARAMS.equals(body)) {
				args = JSON.parseArray(body).toArray();
			}
		}
		try {
			Reflect.getMethod(obj, params.getMethod(), args);
		}catch (Throwable e) {
			if(obj instanceof Observer) {
					return null;
			}else {
				throw new HandlerExecuteException(String.format("bean : %s, method %s , params : %s, not found.", obj, params.getMethod(), args), e);
			}
		}
		Object ret;
		try {
			ret = Reflect.on(obj).call(params.getMethod(), args).get();
		}catch (Throwable e) {
			throw new HandlerExecuteException(String.format("invoke execute exception : %s, bean : %s, method : %s, params : %s"
															, e.getMessage()
															, obj
															, params.getMethod()
															, args)
					, e);
		}
		
		if(obj instanceof Observer) {
			return null;
		}
		byte[] respBody = null;
		if(ret != null && ret != obj) {
			respBody = JSON.toJSONString(ret).getBytes(Charsets.UTF_8);
		}
		return new DefaultHandlerResult(respBody);
	}
	
	@Override
	public void notice(NoticeParams params, NoticeContext ctx) {
		
		Object[] args = null;
		if(params.getBody() != null && params.getBody().length > 0) {
			String body = new String(params.getBody(), Charsets.UTF_8);
			if(!Const.EMPTY_PARAMS.equals(body)) {
				args = JSON.parseArray(body).toArray();
			}
		}
		try {
			Reflect.getMethod(obj, params.getMethod(), args);
		}catch (Throwable e) {
			log.error("bean : {}, method {} , params : {}, not found.", obj, params.getMethod(), args);
			return;
		}
		BaseNoticeContext mqCtx = (BaseNoticeContext) ctx;
		
		try {
			if(obj instanceof ModernNoticeContextAware) {
				BaseNoticeContext baseCtx = new DefaultModerNoticeContext(mqCtx);
				Object holderCtx = holderCtxClazz.getDeclaredConstructor(BaseNoticeContext.class).newInstance(baseCtx);
				Reflect.on(holder).call("set", holderCtx);
			}
			Reflect.on(obj).call(params.getMethod(), args).get();
		}catch (Throwable e) {
			while(e instanceof ReflectException || e instanceof InvocationTargetException) {
				e = e.getCause();
			}
			throw new HandlerExecuteException(String.format("%s execute exception : %s, id : %s, retry count : %s, bean : %s, method : %s"
														, mqCtx.isOrderedNotice() ? "ordered notice" : "notice"
														, e.getMessage()
														, mqCtx.getId()
														, mqCtx.getRetryCount()
														, obj
														, params.getMethod())
					, e);
		}finally {
			if(holder != null) {
				holder.remove();
			}
		}
		
	}
	
}
