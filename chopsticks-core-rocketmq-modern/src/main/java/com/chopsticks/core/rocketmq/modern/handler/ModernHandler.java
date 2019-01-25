package com.chopsticks.core.rocketmq.modern.handler;


import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
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
import com.chopsticks.core.rocketmq.modern.exception.ModernCoreException;
import com.chopsticks.core.utils.Reflect;
import com.chopsticks.core.utils.Reflect.ReflectException;
import com.google.common.base.Charsets;

public class ModernHandler extends BaseHandler{
	
	private static final Logger log = LoggerFactory.getLogger(ModernHandler.class);
	
	
	private Object obj;
	
	public ModernHandler(Object obj, String topic, String tag) {
		super(topic, tag);
		this.obj = obj;
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
			throw new ModernCoreException(String.format("bean : %s, method %s , params : %s, not found.", obj, params.getMethod(), args), e).setCode(ModernCoreException.MODERN_INVOKE_METHOD_NOT_FOUND);
		}
		Object ret;
		BaseInvokeContext mqCtx = (BaseInvokeContext) ctx;
		try {
			ModernContextHolder.setReqTime(mqCtx.getReqTime());
			ModernContextHolder.setExtParams(mqCtx.getExtParams());
			ModernContextHolder.setTraceNos(mqCtx.getTraceNos());
			ret = Reflect.on(obj).call(params.getMethod(), args).get();
		}catch (CoreException e) {
			throw e;
		}catch (Throwable e) {
			while(e instanceof ReflectException || e instanceof InvocationTargetException) {
				e = e.getCause();
			}
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new ModernCoreException(String.format("invoke execute exception : %s", e.getMessage())
						, e).setCode(ModernCoreException.MODERN_INVOKE_EXECUTE_ERROR);
			}
		}finally {
			ModernContextHolder.remove();
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
			log.error("noticeId : {}, bean : {}, method {} , params : {}, not found.", ctx.getId(), obj, params.getMethod(), args);
			return;
		}
		BaseNoticeContext mqCtx = (BaseNoticeContext) ctx;
		
		try {
			DefaultModerNoticeContext baseCtx = new DefaultModerNoticeContext(mqCtx);
			ModernContextHolder.setNoticeContext(baseCtx);
			ModernContextHolder.setReqTime(mqCtx.getReqTime());
			ModernContextHolder.setExtParams(baseCtx.getExtParams());
			ModernContextHolder.setTraceNos(mqCtx.getTraceNos());
			Reflect.on(obj).call(params.getMethod(), args).get();
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
			ModernContextHolder.remove();
		}
		
	}
}
