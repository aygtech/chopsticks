package com.chopsticks.core.rockctmq.modern.handler;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.core.exception.HandlerExecuteException;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.handler.InvokeContext;
import com.chopsticks.core.handler.InvokeParams;
import com.chopsticks.core.handler.NoticeContext;
import com.chopsticks.core.handler.NoticeParams;
import com.chopsticks.core.modern.handler.ModernNoticeContext;
import com.chopsticks.core.modern.handler.ModernNoticeContextAware;
import com.chopsticks.core.rockctmq.modern.Const;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.chopsticks.core.rocketmq.handler.BaseNoticeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultHandlerResult;
import com.chopsticks.core.utils.Reflect;
import com.google.common.base.Charsets;

public class ModernHandler extends BaseHandler{
	
	private static final Logger log = LoggerFactory.getLogger(ModernHandler.class);
	
	private Object obj;
	
	private static final ThreadLocal<ModernNoticeContext> modernCtx = new ThreadLocal<ModernNoticeContext>();
	
	public ModernHandler(Object obj, String topic, String tag) {
		super(topic, tag);
		this.obj = obj;
		if(obj instanceof ModernNoticeContextAware) {
			((ModernNoticeContextAware)obj).setNoticeContext(modernCtx);
		}
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
		Method method;
		try {
			method = Reflect.getMethod(obj, params.getMethod(), args);
		}catch (Throwable e) {
			if(obj.getClass().isAnnotationPresent(Watch.class)) {
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
		
		if(obj.getClass().isAnnotationPresent(Watch.class) 
		|| method.isAnnotationPresent(Watch.class)) {
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
				modernCtx.set(new DefaultModerNoticeContext(mqCtx.getId(), mqCtx.getOriginId(), mqCtx.getReconsumeTimes()));
			}
			Reflect.on(obj).call(params.getMethod(), args).get();
		}catch (Throwable e) {
			throw new HandlerExecuteException(String.format("notice execute exception : %s, id : %s, retry count : %s, bean : %s, method : %s"
														, e.getMessage()
														, mqCtx.getId()
														, mqCtx.getReconsumeTimes()
														, obj
														, params.getMethod())
					, e);
		}finally {
			modernCtx.remove();
		}
		
	}
	
}
