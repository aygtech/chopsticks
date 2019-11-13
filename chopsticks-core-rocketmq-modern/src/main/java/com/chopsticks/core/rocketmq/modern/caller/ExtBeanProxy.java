package com.chopsticks.core.rocketmq.modern.caller;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.chopsticks.common.concurrent.Promise;
import com.chopsticks.common.concurrent.PromiseListener;
import com.chopsticks.core.modern.caller.ModernInvokeCommand;
import com.chopsticks.core.modern.caller.ModernNoticeCommand;
import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;
import com.chopsticks.core.rocketmq.caller.BaseNoticeResult;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeCommand;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.modern.DefaultModernClient;
import com.chopsticks.core.rocketmq.modern.caller.impl.DefaultModernNoticeCommand;
import com.chopsticks.core.rocketmq.modern.handler.ModernContextHolder;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ExtBeanProxy extends BaseProxy {
	private String clazzName;

	public ExtBeanProxy(String clazzName, DefaultModernClient client) {
		super(client);
		this.clazzName = clazzName;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return innerInvoke(proxy, method, args);
	}
	
	private Promise<BaseNoticeResult> asyncNotice(Method method, Object[] args){
		ModernNoticeCommand cmd = (ModernNoticeCommand)args[0];
		byte[] body = buildBody(cmd.getParams());
		
		final DefaultNoticeCommand noticeCmd = new DefaultNoticeCommand(clazzName, cmd.getMethod(), body);
		if(cmd instanceof BaseModernCommand) {
			if(cmd instanceof DefaultModernNoticeCommand) {
				noticeCmd.setTransaction(((DefaultModernNoticeCommand)cmd).isTransaction());
			}
			Map<String, String> extParams = Maps.newHashMap(getExtParams());
			extParams.putAll(((BaseModernCommand)cmd).getExtParams());
			noticeCmd.setExtParams(extParams);
			if(((BaseModernCommand) cmd).getTraceNos().isEmpty()) {
				if(ModernContextHolder.getTraceNos() == null || ModernContextHolder.getTraceNos().isEmpty()) {
					noticeCmd.setTraceNos(Sets.newHashSet(getDefaultTraceNo()));
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
		baseResult.addListener(new PromiseListener<BaseNoticeResult>() {
			@Override
			public void onSuccess(BaseNoticeResult result) {
				result.getTraceNos().addAll(noticeCmd.getTraceNos());
			}
			@Override
			public void onFailure(Throwable t) {}
		});
		return baseResult;
	}
	
	private Promise<BaseInvokeResult> asyncInvoke(Method method, Object[] args) {
		ModernInvokeCommand cmd = (ModernInvokeCommand)args[0];
		byte[] body = buildBody(cmd.getParams());
		final DefaultInvokeCommand invokeCmd = new DefaultInvokeCommand(clazzName, cmd.getMethod(), body);
		if(cmd instanceof BaseModernCommand) {
			Map<String, String> extParams = Maps.newHashMap(getExtParams());
			extParams.putAll(((BaseModernCommand)cmd).getExtParams());
			invokeCmd.setExtParams(extParams);
			if(((BaseModernCommand) cmd).getTraceNos().isEmpty()) {
				if(ModernContextHolder.getTraceNos() == null || ModernContextHolder.getTraceNos().isEmpty()) {
					invokeCmd.setTraceNos(Sets.newHashSet(getDefaultTraceNo()));
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
		baseResult.addListener(new PromiseListener<BaseInvokeResult>() {
			@Override
			public void onSuccess(BaseInvokeResult result) {
				result.getTraceNos().addAll(invokeCmd.getTraceNos());
			}
			@Override
			public void onFailure(Throwable t) {}
		});
		return baseResult;
	}

	@Override
	public Object innerInvoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(method.getName().toLowerCase().contains("asyncInvoke".toLowerCase())) {
			return asyncInvoke(method, args);
		}else if(method.getName().toLowerCase().contains("invoke".toLowerCase())) {
			return asyncInvoke(method, args).get();
		}else if(method.getName().toLowerCase().contains("asyncNotice".toLowerCase())) {
			return asyncNotice(method, args);
		}else if(method.getName().toLowerCase().contains("notice".toLowerCase())) {
			return asyncNotice(method, args).get();
		}else {
			throw new RuntimeException("unsupport method");
		}
	}
}
