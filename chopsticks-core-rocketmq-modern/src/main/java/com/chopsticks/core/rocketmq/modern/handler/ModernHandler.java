package com.chopsticks.core.rocketmq.modern.handler;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.common.concurrent.Promise;
import com.chopsticks.common.concurrent.PromiseListener;
import com.chopsticks.common.concurrent.impl.DefaultPromise;
import com.chopsticks.common.utils.Reflect;
import com.chopsticks.common.utils.Reflect.ReflectException;
import com.chopsticks.core.exception.CoreException;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.handler.InvokeContext;
import com.chopsticks.core.handler.InvokeParams;
import com.chopsticks.core.handler.NoticeContext;
import com.chopsticks.core.handler.NoticeParams;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.chopsticks.core.rocketmq.handler.BaseInvokeContext;
import com.chopsticks.core.rocketmq.handler.BaseNoticeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultHandlerResult;
import com.chopsticks.core.rocketmq.modern.Const;
import com.chopsticks.core.rocketmq.modern.DefaultModernClient;
import com.chopsticks.core.rocketmq.modern.exception.ModernCoreException;
import com.google.common.base.Charsets;

public class ModernHandler extends BaseHandler{
	
	private static final Logger log = LoggerFactory.getLogger(ModernHandler.class);
	
	
	private Object obj;
	private DefaultModernClient client;
	
	public ModernHandler(Object obj, String topic, String tag, DefaultModernClient client) {
		super(topic, tag);
		this.obj = obj;
		this.client = client;
	}

	@Override
	public HandlerResult invoke(InvokeParams params, InvokeContext ctx) {
		Object[] args = null;
		if(params.getBody() != null && params.getBody().length > 0) {
			String body = new String(params.getBody(), Charsets.UTF_8);
			if(!Const.EMPTY_PARAMS.equals(body)) {
				try {
					args = JSON.parseArray(body).toArray();
				}catch (Throwable e) {
					throw new ModernCoreException(String.format("bean : %s, method %s , params : %s build error", obj, params.getMethod(), body), e).setCode(ModernCoreException.MODERN_INVOKE_METHOD_PARAMS_BUILD_ERROR);
				}
			}
		}
		Method invokeMethod = null;
		try {
			invokeMethod = Reflect.getMethod(obj, params.getMethod(), args);
		}catch (Throwable e) {
			throw new ModernCoreException(String.format("bean : %s, method %s , params : %s, not found.", obj, params.getMethod(), args), e).setCode(ModernCoreException.MODERN_INVOKE_METHOD_NOT_FOUND);
		}
		Object methodRet;
		BaseInvokeContext mqCtx = (BaseInvokeContext) ctx;
		String oldThreadName = Thread.currentThread().getName();
		Promise<?> invokeExecutePromise = null;
		try {
			ModernContextHolder.setReqTime(mqCtx.getReqTime());
			ModernContextHolder.setExtParams(mqCtx.getExtParams());
			ModernContextHolder.setTraceNos(mqCtx.getTraceNos());
			methodRet = client.getModernClientProxy().invokeExecuteProxy(obj, params.getMethod(), args);
			invokeExecutePromise = ModernContextHolder.getInvokeExecutePromise();
//			if(invokeExecutePromise != null) {
//				Class<?> returnType = (Class<?>)((ParameterizedType)invokeExecutePromise.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
//				if(invokeMethod.getReturnType() != returnType) {
//					throw new ModernCoreException().setCode(ModernCoreException.INVOKE_RETURN_TYPE_NOT_MATCH);
//				}
//			}
		}catch (CoreException e) {
			throw e;
		}catch (Throwable e) {
			while(e instanceof ReflectException || e instanceof InvocationTargetException) {
				e = e.getCause();
			}
			throw new ModernCoreException(String.format("invoke execute exception : %s", e.getMessage())
						, e).setCode(ModernCoreException.MODERN_INVOKE_EXECUTE_ERROR);
		}finally {
			Thread.currentThread().setName(oldThreadName);
			ModernContextHolder.remove();
		}
		
		if(obj instanceof Observer) {
			return null;
		}
		byte[] respBody = null;
		if(methodRet != null && methodRet != obj) {
			respBody = JSON.toJSONString(methodRet).getBytes(Charsets.UTF_8);
		}
		DefaultHandlerResult ret = new DefaultHandlerResult(respBody);
		if(invokeExecutePromise != null) {
			final DefaultPromise<HandlerResult> promise = new DefaultPromise<HandlerResult>();
			final Class<?> returnType = invokeMethod.getReturnType();
			invokeExecutePromise.addListener(new PromiseListener<Object>() {
				@Override
				public void onFailure(Throwable t) {
					while(t instanceof ReflectException || t instanceof InvocationTargetException) {
						t = t.getCause();
					}
					if(!(t instanceof CoreException)) {
						t = new ModernCoreException(String.format("invoke execute exception : %s", t.getMessage())
									, t).setCode(ModernCoreException.MODERN_INVOKE_EXECUTE_ERROR);
					}
					promise.setException(t);
				}
				public void onSuccess(Object result) {
					byte[] respBody = null;
					if(result != null && result != obj) {
						// TODO 未实现完全，泛型都无法判断准确性
						if(!returnType.isAssignableFrom(result.getClass())) {
							promise.setException(new ModernCoreException(String.format("promise value %s not match %s", result.getClass(), returnType)).setCode(ModernCoreException.INVOKE_RETURN_TYPE_NOT_MATCH));
							return;
						}
						respBody = JSON.toJSONString(result).getBytes(Charsets.UTF_8);
					}
					promise.set(new DefaultHandlerResult(respBody));
				};
			});
			ret.setPromise(promise);
		}
		return ret;
	}
	
	@Override
	public void notice(NoticeParams params, NoticeContext ctx) {
		
		Object[] args = null;
		if(params.getBody() != null && params.getBody().length > 0) {
			String body = new String(params.getBody(), Charsets.UTF_8);
			if(!Const.EMPTY_PARAMS.equals(body)) {
				try {
					args = JSON.parseArray(body).toArray();
				}catch (Throwable e) {
					throw new ModernCoreException(String.format("bean : %s, method %s , params : %s build error", obj, params.getMethod(), body), e).setCode(ModernCoreException.MODERN_NOTICE_METHOD_PARAMS_BUILD_ERROR);
				}
			}
		}
		try {
			Reflect.getMethod(obj, params.getMethod(), args);
		}catch (Throwable e) {
			log.error("noticeId : {}, bean : {}, method {} , params : {}, not found.", ctx.getId(), obj, params.getMethod(), args);
			return;
		}
		BaseNoticeContext mqCtx = (BaseNoticeContext) ctx;
		String oldThreadName = Thread.currentThread().getName();
		try {
			DefaultModerNoticeContext baseCtx = new DefaultModerNoticeContext(mqCtx);
			ModernContextHolder.setNoticeContext(baseCtx);
			ModernContextHolder.setReqTime(mqCtx.getReqTime());
			ModernContextHolder.setExtParams(baseCtx.getExtParams());
			ModernContextHolder.setTraceNos(mqCtx.getTraceNos());
			Thread.currentThread().setName(String.format("%s_%s", baseCtx.getId(), oldThreadName));
			client.getModernClientProxy().noticeExecuteProxy(obj, params.getMethod(), args);
//			Reflect.on(obj).call(params.getMethod(), args).get();
		}catch (CoreException e) {
			throw e;
		}catch (Throwable e) {
			while(e instanceof ReflectException || e instanceof InvocationTargetException) {
				e = e.getCause();
			}
			throw new ModernCoreException(String.format("%s execute exception : %s, id : %s, retryCount : %s, maxRetryCount : %s, bean : %s, method : %s"
														, mqCtx.isOrderedNotice() ? "ordered notice" : (mqCtx.isDelayNotice() ? "delay notice" : "notice")
														, e.getMessage()
														, mqCtx.getId()
														, mqCtx.getRetryCount()
														, mqCtx.isMaxRetryCount()
														, obj
														, params.getMethod())
					, e).setCode(ModernCoreException.MODERN_NOTICE_EXECUTE_ERROR);
		}finally {
			Thread.currentThread().setName(oldThreadName);
			ModernContextHolder.remove();
		}
		
	}
}
