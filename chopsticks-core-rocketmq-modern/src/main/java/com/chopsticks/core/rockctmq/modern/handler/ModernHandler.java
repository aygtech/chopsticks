package com.chopsticks.core.rockctmq.modern.handler;

import com.alibaba.fastjson.JSON;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.handler.InvokeContext;
import com.chopsticks.core.handler.InvokeParams;
import com.chopsticks.core.handler.NoticeContext;
import com.chopsticks.core.handler.NoticeParams;
import com.chopsticks.core.rockctmq.modern.Const;
import com.chopsticks.core.rocketmq.exception.HandlerExecuteException;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.chopsticks.core.rocketmq.handler.BaseNoticeContext;
import com.chopsticks.core.rocketmq.handler.BaseNoticeParams;
import com.chopsticks.core.rocketmq.handler.impl.DefaultHandlerResult;
import com.chopsticks.core.rocketmq.handler.impl.DefaultInvokeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultInvokeParams;
import com.chopsticks.core.utils.Reflect;
import com.google.common.base.Charsets;

public class ModernHandler extends BaseHandler{
	
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
		Object ret = Reflect.on(obj).call(params.getMethod(), args).get();
		if(Reflect.getMethod(obj, params.getMethod(), args).isAnnotationPresent(ModernWatch.class)) {
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
		
		BaseNoticeParams mqParams = (BaseNoticeParams) params;
		BaseNoticeContext mqCtx = (BaseNoticeContext) ctx;
		try {
		
			invoke(new DefaultInvokeParams(mqParams.getTopic(), mqParams.getTag(), params.getBody()), new DefaultInvokeContext());
		}catch (Throwable e) {
			throw new HandlerExecuteException(String.format("notice execute exception : %s, retry count : %s, bean : %s, method : %s"
														, e.getMessage()
														, mqCtx.getReconsumeTimes()
														, obj
														, mqParams.getTag())
					, e);
		}
		
	}
	
}
