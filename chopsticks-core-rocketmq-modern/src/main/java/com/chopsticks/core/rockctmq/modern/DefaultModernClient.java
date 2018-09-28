package com.chopsticks.core.rockctmq.modern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.chopsticks.core.modern.ModernClient;
import com.chopsticks.core.modern.caller.ExtBean;
import com.chopsticks.core.modern.caller.NoticeBean;
import com.chopsticks.core.rockctmq.modern.caller.BaseExtBean;
import com.chopsticks.core.rockctmq.modern.caller.BaseNoticeBean;
import com.chopsticks.core.rockctmq.modern.caller.BeanProxy;
import com.chopsticks.core.rockctmq.modern.caller.ExtBeanProxy;
import com.chopsticks.core.rockctmq.modern.caller.NoticeBeanProxy;
import com.chopsticks.core.rockctmq.modern.handler.ModernHandler;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.google.common.collect.Sets;

public class DefaultModernClient implements ModernClient {
	
	private String groupName;
	private String namesrvAddr;
	private DefaultClient client;
	private Map<Class<?>, Object> handlers;
	
	private volatile boolean started;
	
	public DefaultModernClient(String groupName) {
		this.groupName = groupName;
	}
	
	public void setNamesrvAddr(String namesrvAddr) {
		this.namesrvAddr = namesrvAddr;
	}
	
	@Override
	public void register(Map<Class<?>, Object> handlers) {
		this.handlers = handlers;
	}
	
	@Override
	public synchronized void start() {
		if(!started) {
			client = new DefaultClient(groupName);
			client.setNamesrvAddr(namesrvAddr);
			if(handlers != null) {
				Set<BaseHandler> clientHandlers = Sets.newHashSet();
				for(Entry<Class<?>, Object> entry : handlers.entrySet()) {
					clientHandlers.add(new ModernHandler(entry.getValue()
														, entry.getKey().getName()
														, com.chopsticks.core.rocketmq.Const.ALL_TAGS));
					
				}
				client.register(clientHandlers);
			}
			client.start();
			started = true;
		}
	}
	

	@Override
	public synchronized void shutdown() {
		if(client != null) {
			client.shutdown();
			started = false;
		}
	}
	
	@Override
	public <T> T getBean(final Class<T> clazz) {
		checkNotNull(clazz);
		checkArgument(clazz.isInterface(), "clazz must be interface");
		return clazz.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {clazz}, new BeanProxy(clazz, client)));
	}


	@Override
	public NoticeBean getNoticeBean(Class<?> clazz) {
		checkNotNull(clazz);
		checkArgument(clazz.isInterface(), "clazz must be interface");
		return BaseNoticeBean.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {BaseNoticeBean.class, clazz}, new NoticeBeanProxy(clazz, client)));
	}
	
	@Override
	public ExtBean getExtBean(String clazzName) {
		return BaseExtBean.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {BaseExtBean.class}, new ExtBeanProxy(clazzName, client)));
	}
	
}
