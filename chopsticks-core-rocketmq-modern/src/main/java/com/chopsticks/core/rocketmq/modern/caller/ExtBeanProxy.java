package com.chopsticks.core.rocketmq.modern.caller;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.chopsticks.core.concurrent.Promise;
import com.chopsticks.core.modern.caller.ModernInvokeCommand;
import com.chopsticks.core.modern.caller.ModernNoticeCommand;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;
import com.chopsticks.core.rocketmq.caller.BaseNoticeResult;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeCommand;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.modern.Const;
import com.chopsticks.core.rocketmq.modern.handler.ModernContextHolder;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ExtBeanProxy extends BaseProxy {
	private String clazzName;
	private DefaultClient client;

	public ExtBeanProxy(String clazzName, DefaultClient client) {
		this.clazzName = clazzName;
		this.client = client;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(method.getName().equals("asyncInvoke")) {
			return asyncInvoke(method, args);
		}else if(method.getName().equals("invoke")) {
			return asyncInvoke(method, args).get();
		}else if(method.getName().equals("asyncNotice")) {
			return asyncNotice(method, args);
		}else if(method.getName().equals("notice")) {
			return asyncNotice(method, args).get();
		}else {
			throw new RuntimeException("unsupport method");
		}
	}
	
	private Promise<BaseNoticeResult> asyncNotice(Method method, Object[] args){
		ModernNoticeCommand cmd = (ModernNoticeCommand)args[0];
		byte[] body;
		if(cmd.getParams() == null) {
			body = Const.EMPTY_PARAMS.getBytes(Charsets.UTF_8);
		}else {
			body = JSON.toJSONString(cmd.getParams(), SerializerFeature.WriteClassName).getBytes(Charsets.UTF_8);
		}
		
		DefaultNoticeCommand noticeCmd = new DefaultNoticeCommand(clazzName, cmd.getMethod(), body);
		if(cmd instanceof BaseModernCommand) {
			Map<String, String> extParams = Maps.newHashMap(getExtParams());
			extParams.putAll(((BaseModernCommand)cmd).getExtParams());
			noticeCmd.setExtParams(extParams);
			if(((BaseModernCommand) cmd).getTraceNos().isEmpty()) {
				if(ModernContextHolder.getTraceNos() == null || ModernContextHolder.getTraceNos().isEmpty()) {
					noticeCmd.setTraceNos(Sets.newHashSet(getDefaultTrackNo()));
				}else {
					noticeCmd.setTraceNos(ModernContextHolder.getTraceNos());
				}
			}else {
				noticeCmd.setTraceNos(((BaseModernCommand) cmd).getTraceNos());
			}
		}
		Promise<BaseNoticeResult> baseResult = null;
		if(args.length == 1) {
			baseResult = client.asyncNotice(noticeCmd);
		}else if(args.length == 2) {
			Object orderKey = (String)args[1];
			baseResult = client.asyncNotice(noticeCmd, orderKey);
		}else if(args.length == 3){
			Long delay = (Long)args[1];
			TimeUnit delayTimeUnit = (TimeUnit)args[2];
			baseResult = client.asyncNotice(noticeCmd, delay, delayTimeUnit);
		}else {
			throw new RuntimeException("unsupport method");
		}
		return baseResult;
	}
	
	private Promise<BaseInvokeResult> asyncInvoke(Method method, Object[] args) {
		ModernInvokeCommand cmd = (ModernInvokeCommand)args[0];
		byte[] body;
		if(cmd.getParams() == null) {
			body = Const.EMPTY_PARAMS.getBytes(Charsets.UTF_8);
		}else {
			body = JSON.toJSONString(cmd.getParams(), SerializerFeature.WriteClassName).getBytes(Charsets.UTF_8);
		}
		DefaultInvokeCommand invokeCmd = new DefaultInvokeCommand(clazzName, cmd.getMethod(), body);
		if(cmd instanceof BaseModernCommand) {
			Map<String, String> extParams = Maps.newHashMap(getExtParams());
			extParams.putAll(((BaseModernCommand)cmd).getExtParams());
			invokeCmd.setExtParams(extParams);
			if(((BaseModernCommand) cmd).getTraceNos().isEmpty()) {
				if(ModernContextHolder.getTraceNos() == null || ModernContextHolder.getTraceNos().isEmpty()) {
					invokeCmd.setTraceNos(Sets.newHashSet(getDefaultTrackNo()));
				}else {
					invokeCmd.setTraceNos(ModernContextHolder.getTraceNos());
				}
			}else {
				invokeCmd.setTraceNos(((BaseModernCommand) cmd).getTraceNos());
			}
		}
		Promise<BaseInvokeResult> baseResult;
		if(args.length == 1) {
			baseResult = client.asyncInvoke(invokeCmd);
		}else if(args.length == 3){
			Long delay = (Long)args[1];
			TimeUnit delayTimeUnit = (TimeUnit)args[2];
			baseResult = client.asyncInvoke(invokeCmd, delay, delayTimeUnit);
		}else {
			throw new RuntimeException("unsupport method");
		}
		return baseResult;
	}
}
