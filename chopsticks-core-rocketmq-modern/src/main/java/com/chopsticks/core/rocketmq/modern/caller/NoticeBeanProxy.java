package com.chopsticks.core.rocketmq.modern.caller;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.chopsticks.core.modern.caller.NoticeCommand;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.caller.BaseNoticeResult;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeResult;
import com.chopsticks.core.rocketmq.modern.Const;
import com.chopsticks.core.utils.Reflect;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

public class NoticeBeanProxy extends BaseProxy{
	
	private Class<?> clazz;
	private DefaultClient client;
	
	public NoticeBeanProxy(Class<?> clazz, DefaultClient client) {
		this.clazz = clazz;
		this.client = client;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Preconditions.checkNotNull(args);
		byte[] body;
		NoticeCommand cmd = ((NoticeCommand)args[0]);
		if (cmd.getParams() == null) {
			body = Const.EMPTY_PARAMS.getBytes(Charsets.UTF_8);
		} else {
			body = JSON.toJSONString(cmd.getParams(), SerializerFeature.WriteClassName).getBytes(Charsets.UTF_8);
		}
		// check method exist
		Reflect.getMethod(proxy, cmd.getMethod(), cmd.getParams());
		BaseNoticeResult baseResult;
		if(args.length == 1) {
			baseResult = client.notice(new DefaultNoticeCommand(getTopic(clazz), cmd.getMethod(), body));
		}else if(args.length == 2) {
			Object orderKey = (String)args[1];
			baseResult = client.notice(new DefaultNoticeCommand(getTopic(clazz), cmd.getMethod(), body), orderKey);
		}else if(args.length == 3){
			Long delay = (Long)args[1];
			TimeUnit delayTimeUnit = (TimeUnit)args[2];
			baseResult = client.notice(new DefaultNoticeCommand(getTopic(clazz), cmd.getMethod(), body), delay, delayTimeUnit);
		}else {
			throw new RuntimeException("unsupport method");
		}
		
		DefaultNoticeResult ret = new DefaultNoticeResult(baseResult.getId());
		return ret;
	}

}
