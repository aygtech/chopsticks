package com.chopsticks.core.rocketmq.modern.caller;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.chopsticks.core.modern.caller.ModernInvokeCommand;
import com.chopsticks.core.modern.caller.ModernNoticeCommand;
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
		ModernNoticeCommand cmd = (ModernNoticeCommand)args[0];
		byte[] body;
		if(cmd.getParams() == null) {
			body = Const.EMPTY_PARAMS.getBytes(Charsets.UTF_8);
		}else {
			body = JSON.toJSONString(cmd.getParams(), SerializerFeature.WriteClassName).getBytes(Charsets.UTF_8);
		}
		BaseNoticeResult baseResult;
		DefaultNoticeCommand noticeCmd = new DefaultNoticeCommand(clazzName, cmd.getMethod(), body);
		if(cmd instanceof BaseModernCommand) {
			noticeCmd.setTraceNos(((BaseModernCommand)cmd).getTraceNos());
			noticeCmd.setExtParams(((BaseModernCommand)cmd).getExtParams());
		}
		if(args.length == 1) {
			baseResult = client.notice(noticeCmd);
		}else if(args.length == 2) {
			Object orderKey = (String)args[1];
			baseResult = client.notice(noticeCmd, orderKey);
		}else if(args.length == 3){
			Long delay = (Long)args[1];
			TimeUnit delayTimeUnit = (TimeUnit)args[2];
			baseResult = client.notice(noticeCmd, delay, delayTimeUnit);
		}else {
			throw new RuntimeException("unsupport method");
		}
		DefaultNoticeResult ret = new DefaultNoticeResult(baseResult.getId());
		return ret;
	}

	private Object invoke(Method method, Object[] args) {
		ModernInvokeCommand cmd = (ModernInvokeCommand)args[0];
		byte[] body;
		if(cmd.getParams() == null) {
			body = Const.EMPTY_PARAMS.getBytes(Charsets.UTF_8);
		}else {
			body = JSON.toJSONString(cmd.getParams(), SerializerFeature.WriteClassName).getBytes(Charsets.UTF_8);
		}
		BaseInvokeResult baseResult;
		DefaultInvokeCommand invokeCmd = new DefaultInvokeCommand(clazzName, cmd.getMethod(), body);
		if(cmd instanceof BaseModernCommand) {
			invokeCmd.setTraceNos(((BaseModernCommand)cmd).getTraceNos());
			invokeCmd.setExtParams(((BaseModernCommand)cmd).getExtParams());
		}
		if(args.length == 1) {
			baseResult = client.invoke(invokeCmd);
		}else if(args.length == 3){
			Long delay = (Long)args[1];
			TimeUnit delayTimeUnit = (TimeUnit)args[2];
			baseResult = client.invoke(invokeCmd, delay, delayTimeUnit);
		}else {
			throw new RuntimeException("unsupport method");
		}
		return baseResult;
	}
	
}
