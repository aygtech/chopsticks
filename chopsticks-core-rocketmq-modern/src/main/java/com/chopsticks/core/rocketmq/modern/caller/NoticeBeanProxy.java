package com.chopsticks.core.rocketmq.modern.caller;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.chopsticks.core.modern.caller.ModernNoticeCommand;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.caller.BaseNoticeResult;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.modern.handler.ModernContextHolder;
import com.chopsticks.core.utils.Reflect;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
		ModernNoticeCommand cmd = ((ModernNoticeCommand)args[0]);
		updateCmd(cmd);
		byte[] body = buildBody(cmd.getParams());
		
		// check method exist
		Reflect.getMethod(proxy, cmd.getMethod(), cmd.getParams());
		BaseNoticeResult baseResult;
		DefaultNoticeCommand noticeCmd = new DefaultNoticeCommand(getTopic(clazz), cmd.getMethod(), body);
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
		baseResult.setTraceNos(noticeCmd.getTraceNos());
		return baseResult;
	}
	
	protected void updateCmd(ModernNoticeCommand cmd) {
		
	}

	protected Class<?> getClazz() {
		return clazz;
	}
	
}
