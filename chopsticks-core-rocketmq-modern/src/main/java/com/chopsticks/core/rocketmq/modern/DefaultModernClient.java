package com.chopsticks.core.rocketmq.modern;

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
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.chopsticks.core.rocketmq.modern.caller.BaseExtBean;
import com.chopsticks.core.rocketmq.modern.caller.BaseNoticeBean;
import com.chopsticks.core.rocketmq.modern.caller.BaseProxy;
import com.chopsticks.core.rocketmq.modern.caller.BeanProxy;
import com.chopsticks.core.rocketmq.modern.caller.ExtBeanProxy;
import com.chopsticks.core.rocketmq.modern.caller.NoticeBeanProxy;
import com.chopsticks.core.rocketmq.modern.handler.ModernHandler;
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
	public <T extends NoticeBean> T getNoticeBean(final Class<?> clazz) {
		checkNotNull(clazz);
		checkArgument(clazz.isInterface(), "clazz must be interface");
		try {
			final DefaultModernClient self = this;
			@SuppressWarnings("unchecked")
			T ret = (T) NOTICE_BEAN_CACHE.get(clazz, new Callable<NoticeBean>() {
				@Override
				public NoticeBean call() throws Exception {
					
					return getNoticeBeanClazz().cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {getNoticeBeanClazz(), clazz}, getNoticeBeanProxy(clazz, self)));
				}
			});
			return ret;
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	protected Class<? extends NoticeBean> getNoticeBeanClazz() {
		return BaseNoticeBean.class;
	}
	
	@Override
	public <T extends ExtBean> T getExtBean(final String clazzName) {
		checkNotNull(clazzName);
		try {
			final DefaultModernClient self = this;
			@SuppressWarnings("unchecked")
			T ret = (T) EXT_BEAN_CACHE.get(clazzName, new Callable<ExtBean>() {
				@Override
				public ExtBean call() throws Exception {
					return getExtBeanClazz().cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {getExtBeanClazz()}, new ExtBeanProxy(clazzName, self)));
				}
			});
			return ret;
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	protected Class<? extends ExtBean> getExtBeanClazz(){
		return BaseExtBean.class;
	}
	
	protected BaseProxy getNoticeBeanProxy(Class<?> clazz, DefaultClient client) {
		return new NoticeBeanProxy(clazz, client);
	}
}