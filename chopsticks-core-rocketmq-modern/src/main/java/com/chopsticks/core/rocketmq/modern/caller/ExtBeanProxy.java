package com.chopsticks.core.rocketmq.modern.caller;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.chopsticks.core.modern.caller.InvokeCommand;
import com.chopsticks.core.modern.caller.NoticeCommand;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;
import com.chopsticks.core.rocketmq.caller.BaseNoticeResult;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeCommand;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeResult;
import com.chopsticks.core.rocketmq.modern.Const;
import com.google.common.base.Charsets;

public class ExtBeanProxy extends BaseProxy {
	private String clazzName;
	private DefaultClient client;

	public ExtBeanProxy(String clazzName, DefaultClient client) {
		this.clazzName = clazzName;
		this.client = client;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(method.getName().equals("invoke")) {
			return invoke(method, args);
		}else if(method.getName().equals("notice")) {
			return notice(method, args);
		}else {
			throw new RuntimeException("unsupport method");
		}
	}

	private Object notice(Method method, Object[] args) {
		NoticeCommand cmd = (NoticeCommand)args[0];
		byte[] body;
		if(cmd.getParams() == null) {
			body = Const.EMPTY_PARAMS.getBytes(Charsets.UTF_8);
		}else {
			body = JSON.toJSONString(cmd.getParams(), SerializerFeature.WriteClassName).getBytes(Charsets.UTF_8);
		}
		BaseNoticeResult baseResult;
		if(args.length == 1) {
			baseResult = client.notice(new DefaultNoticeCommand(clazzName, cmd.getMethod(), body));
		}else if(args.length == 2) {
			Object orderKey = (String)args[1];
			baseResult = client.notice(new DefaultNoticeCommand(clazzName, cmd.getMethod(), body), orderKey);
		}else if(args.length == 3){
			Long delay = (Long)args[1];
			TimeUnit delayTimeUnit = (TimeUnit)args[2];
			baseResult = client.notice(new DefaultNoticeCommand(clazzName, cmd.getMethod(), body), delay, delayTimeUnit);
		}else {
			throw new RuntimeException("unsupport method");
		}
		DefaultNoticeResult ret = new DefaultNoticeResult(baseResult.getId());
		return ret;
	}

	private Object invoke(Method method, Object[] args) {
		InvokeCommand cmd = (InvokeCommand)args[0];
		byte[] body;
		if(cmd.getParams() == null) {
			body = Const.EMPTY_PARAMS.getBytes(Charsets.UTF_8);
		}else {
			body = JSON.toJSONString(cmd.getParams(), SerializerFeature.WriteClassName).getBytes(Charsets.UTF_8);
		}
		BaseInvokeResult baseResult;
		if(args.length == 1) {
			baseResult = client.invoke(new DefaultInvokeCommand(clazzName, cmd.getMethod(), body));
		}else if(args.length == 3){
			Long delay = (Long)args[1];
			TimeUnit delayTimeUnit = (TimeUnit)args[2];
			baseResult = client.invoke(new DefaultInvokeCommand(clazzName, cmd.getMethod(), body), delay, delayTimeUnit);
		}else {
			throw new RuntimeException("unsupport method");
		}
		return baseResult;
	}
	
}
