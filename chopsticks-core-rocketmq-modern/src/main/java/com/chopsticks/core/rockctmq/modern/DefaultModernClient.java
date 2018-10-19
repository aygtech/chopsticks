package com.chopsticks.core.rockctmq.modern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.Set;

import com.chopsticks.core.modern.ModernClient;
import com.chopsticks.core.modern.caller.ExtBean;
import com.chopsticks.core.modern.caller.NoticeBean;
import com.chopsticks.core.rockctmq.modern.caller.BaseExtBean;
import com.chopsticks.core.rockctmq.modern.caller.BaseNoticeBean;
import com.chopsticks.core.rockctmq.modern.caller.BaseProxy;
import com.chopsticks.core.rockctmq.modern.caller.BeanProxy;
import com.chopsticks.core.rockctmq.modern.caller.ExtBeanProxy;
import com.chopsticks.core.rockctmq.modern.caller.NoticeBeanProxy;
import com.chopsticks.core.rockctmq.modern.handler.ModernHandler;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;

public class DefaultModernClient extends DefaultClient implements ModernClient {
	
	private static final Cache<Class<?>, Object> BEAN_CACHE = CacheBuilder.newBuilder().build();
	private static final Cache<Class<?>, NoticeBean> NOTICE_BEAN_CACHE = CacheBuilder.newBuilder().build();
	private static final Cache<String, ExtBean> EXT_BEAN_CACHE = CacheBuilder.newBuilder().build();
	
	private Map<Class<?>, Object> handlers;
	
	public DefaultModernClient(String groupName) {
		super(groupName);
	}
	
	@Override
	public void register(Map<Class<?>, Object> handlers) {
		this.handlers = handlers;
	}
	
	@Override
	public synchronized void start() {
		if(handlers != null) {
			Set<BaseHandler> clientHandlers = Sets.newHashSet();
			for(Entry<Class<?>, Object> entry : handlers.entrySet()) {
				clientHandlers.add(new ModernHandler(entry.getValue()
													, entry.getKey().getName()
													, com.chopsticks.core.rocketmq.Const.ALL_TAGS));
				
			}
			super.register(clientHandlers);
		}
		super.start();
	}
	
	@Override
	public <T> T getBean(final Class<T> clazz) {
		checkNotNull(clazz);
		checkArgument(clazz.isInterface(), "clazz must be interface");
		try {
			final DefaultClient self = this;
			Object bean = BEAN_CACHE.get(clazz, new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {clazz}, new BeanProxy(clazz, self));
				}
			});
			return clazz.cast(bean);
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}


	@Override
	public NoticeBean getNoticeBean(final Class<?> clazz) {
		checkNotNull(clazz);
		checkArgument(clazz.isInterface(), "clazz must be interface");
		try {
			final DefaultModernClient self = this;
			return NOTICE_BEAN_CACHE.get(clazz, new Callable<NoticeBean>() {
				@Override
				public NoticeBean call() throws Exception {
					return BaseNoticeBean.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {BaseNoticeBean.class, clazz}, self.getNoticeBeanProxy(clazz, self)));
				}
			});
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public ExtBean getExtBean(final String clazzName) {
		checkNotNull(clazzName);
		try {
			final DefaultModernClient self = this;
			return EXT_BEAN_CACHE.get(clazzName, new Callable<ExtBean>() {
				@Override
				public ExtBean call() throws Exception {
					return BaseExtBean.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {BaseExtBean.class}, new ExtBeanProxy(clazzName, self)));
				}
			});
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	protected BaseProxy getNoticeBeanProxy(Class<?> clazz, DefaultClient client) {
		return new NoticeBeanProxy(clazz, client);
	}
}
