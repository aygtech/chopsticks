package com.chopsticks.core.rocketmq.modern.caller;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.chopsticks.common.utils.Reflect;
import com.chopsticks.core.modern.caller.ModernNoticeCommand;
import com.chopsticks.core.rocketmq.caller.BaseNoticeResult;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.modern.DefaultModernClient;
import com.chopsticks.core.rocketmq.modern.caller.impl.DefaultModernNoticeCommand;
import com.chopsticks.core.rocketmq.modern.handler.ModernContextHolder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class NoticeBeanProxy extends BaseProxy{
	
	private Class<?> clazz;
	
	public NoticeBeanProxy(Class<?> clazz, DefaultModernClient client) {
		super(client);
		this.clazz = clazz;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return innerInvoke(proxy, method, args);
	}
	
	protected Class<?> getClazz() {
		return clazz;
	}

	@Override
	public Object innerInvoke(Object proxy, Method method, Object[] args) throws Throwable {
		Preconditions.checkNotNull(args);
		ModernNoticeCommand cmd = ((ModernNoticeCommand)args[0]);
		byte[] body = buildBody(cmd.getParams());
		
		// check method exist
		Reflect.getMethod(proxy, cmd.getMethod(), cmd.getParams());
		
		DefaultNoticeCommand noticeCmd = new DefaultNoticeCommand(getTopic(clazz), cmd.getMethod(), body);
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
		BaseNoticeResult baseResult = null;
		if(args.length == 1) {
			baseResult = client.notice(noticeCmd);
		}else if(args.length == 2) {
			Object orderKey = args[1];
			baseResult = client.notice(noticeCmd, orderKey);
		}else if(args.length == 3){
			Long delay = (Long)args[1];
			TimeUnit delayTimeUnit = (TimeUnit)args[2];
			baseResult = client.notice(noticeCmd, delay, delayTimeUnit);
		}else {
			throw new RuntimeException("unsupport method");
		}
		baseResult.getTraceNos().addAll(noticeCmd.getTraceNos());
		return baseResult;
	}
	
}
