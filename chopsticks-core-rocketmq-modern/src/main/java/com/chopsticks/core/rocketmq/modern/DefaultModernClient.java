package com.chopsticks.core.rocketmq.modern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.common.utils.TimeUtils;
import com.chopsticks.core.exception.CoreException;
import com.chopsticks.core.modern.ModernClient;
import com.chopsticks.core.modern.caller.ExtBean;
import com.chopsticks.core.modern.caller.NoticeBean;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.chopsticks.core.rocketmq.modern.caller.BaseExtBean;
import com.chopsticks.core.rocketmq.modern.caller.BaseNoticeBean;
import com.chopsticks.core.rocketmq.modern.caller.BaseProxy;
import com.chopsticks.core.rocketmq.modern.caller.BeanProxy;
import com.chopsticks.core.rocketmq.modern.caller.ExtBeanProxy;
import com.chopsticks.core.rocketmq.modern.caller.NoticeBeanProxy;
import com.chopsticks.core.rocketmq.modern.exception.ModernCoreException;
import com.chopsticks.core.rocketmq.modern.handler.ModernHandler;
import com.chopsticks.core.rocketmq.modern.handler.Picker;
import com.chopsticks.core.rocketmq.modern.handler.UnSupportDelayNotice;
import com.chopsticks.core.rocketmq.modern.handler.UnSupportInvoke;
import com.chopsticks.core.rocketmq.modern.handler.UnSupportNotice;
import com.chopsticks.core.rocketmq.modern.handler.UnSupportOrderedNotice;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;

/**
 * 默认封装客户端实现
 * @author zilong.li
 *
 */
public class DefaultModernClient extends DefaultClient implements ModernClient {
	
	private static final Logger log = LoggerFactory.getLogger(DefaultModernClient.class);
	
	private final Cache<Class<?>, Object> beanCache = CacheBuilder.newBuilder().build();
	private final Cache<Class<?>, NoticeBean> noticeBeanCache = CacheBuilder.newBuilder().build();
	private final Cache<String, ExtBean> extBeanCache = CacheBuilder.newBuilder().build();
	
	private Map<Class<?>, Object> handlers;
	
	public DefaultModernClient(String groupName) {
		super(groupName);
	}
	
	@Override
	public void register(Map<Class<?>, Object> handlers) {
		for(Entry<Class<?>, Object> entry : handlers.entrySet()) {
			if(!entry.getKey().isInterface()) {
				throw new ModernCoreException("key must be interface");
			}
			checkNotNull(entry.getValue(), "key : %s, value is null", entry.getKey());
		}
		this.handlers = handlers;
	}
	
	@Override
	public synchronized void shutdown() {
		log.info("Client {} begin shutdown", getGroupName());
		super.shutdown();
		log.info("Client {} end shutdown", getGroupName());
	}
	
	@Override
	public synchronized void start() {
		log.info("Client {} begin start", getGroupName());
		log.info("InvokeBeginExectableTime : {}, Invokable : {}, InvokeExecutable : {}, InvokeExecutableNum : {}, InvokeMaxExecutableTime : {}, NoticeBeginExecutableTime : {}, NoticeExecutable : {}, NoticeExecutableNum : {}, NoticeExcecutableRetryCount : {}, NoticeMaxExecutableTime : {}, DelayNoticeBeginExecutableTime : {}, DelayNoticeExecutable : {}, DelayNoticeExecutableNum : {}, DelayNoticeExecutableRetryCount : {}, DelayNoticeMaxExecutableTime : {}, OrderedNoticeBeginExecutableTime : {}, OrderedNoticeExecutable : {}, OrderedNoticeExecutableNum : {}, OrderedNoticeExecutableRetryCount : {}, OrderedNoticeMaxExecutableTime : {}"
				, TimeUtils.yyyyMMddHHmmssSSS(getInvokeBeginExectableTime())
				, isInvokable()
				, isInvokeExecutable()
				, getInvokeExecutableNum()
				, getInvokeMaxExecutableTime()
				, TimeUtils.yyyyMMddHHmmssSSS(getNoticeBeginExecutableTime())
				, isNoticeExecutable()
				, getNoticeExecutableNum()
				, getNoticeExcecutableRetryCount()
				, getNoticeMaxExecutableTime()
				, TimeUtils.yyyyMMddHHmmssSSS(getDelayNoticeBeginExecutableTime())
				, isDelayNoticeExecutable()
				, getDelayNoticeExecutableNum()
				, getDelayNoticeExecutableRetryCount()
				, getDelayNoticeMaxExecutableTime()
				, TimeUtils.yyyyMMddHHmmssSSS(getOrderedNoticeBeginExecutableTime())
				, isOrderedNoticeExecutable()
				, getOrderedNoticeExecutableNum()
				, getOrderedNoticeExecutableRetryCount()
				, getOrderedNoticeMaxExecutableTime());
		try {
			if(handlers != null) {
				Set<BaseHandler> clientHandlers = Sets.newHashSet();
				for(Entry<Class<?>, Object> entry : handlers.entrySet()) {
					Set<String> methods = getMethods(entry);
					Set<String> unSupportInvoke = getUnSupportInvoke(entry);
					Set<String> unSupportNotice = getUnSupportNotice(entry);
					Set<String> unSupportDelayNotice = getUnSupportDelayNotice(entry);
					Set<String> unSupportOrderedNotice = getUnSupportOrderedNotice(entry);
					
					log.info("interface : {}, impl : {}, method : {}, unSupportInvoke : {}, unSupportNotice : {}, unSupportDelayNotice : {}, unSupportOrderedNotice : {}"
								, entry.getKey()
								, entry.getValue()
								, methods
								, unSupportInvoke
								, unSupportNotice
								, unSupportDelayNotice
								, unSupportOrderedNotice);
					for(String method : methods) {
						BaseHandler handler = new ModernHandler(entry.getValue(), entry.getKey().getName(), method);
						if(unSupportInvoke.contains(method)) {
							handler.setSupportInvoke(false);
						}
						if(unSupportNotice.contains(method)) {
							handler.setSupportNotice(false);
						}
						if(unSupportDelayNotice.contains(method)) {
							handler.setSupportDelayNotice(false);
						}
						if(unSupportOrderedNotice.contains(method)) {
							handler.setSupportOrderedNotice(false);
						}
						clientHandlers.add(handler);
					}
				}
				super.register(clientHandlers);
			}
			super.start();
		}catch (Throwable e) {
			this.shutdown();
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new ModernCoreException(e);
			}
		}
		log.info("Client {} end start", getGroupName());
	}
	
	private Set<String> getUnSupportNotice(Entry<Class<?>, Object> entry) {
		Set<String> unSupportNotice = Sets.newHashSet();
		if(entry.getValue() instanceof UnSupportNotice) {
			unSupportNotice = checkNotNull(((UnSupportNotice)entry.getValue()).unSupportNotice(), "unSupportNotice can not be null");
		}
		return unSupportNotice;
	}
	private Set<String> getUnSupportDelayNotice(Entry<Class<?>, Object> entry) {
		Set<String> unSupportDelayNotice = Sets.newHashSet();
		if(entry.getValue() instanceof UnSupportDelayNotice) {
			unSupportDelayNotice = checkNotNull(((UnSupportDelayNotice)entry.getValue()).unSupportDelayNotice(), "unSupportDelayNotice can not be null");
		}
		return unSupportDelayNotice;
	}
	private Set<String> getUnSupportOrderedNotice(Entry<Class<?>, Object> entry) {
		Set<String> unSupportOrderedNotice = Sets.newHashSet();
		if(entry.getValue() instanceof UnSupportOrderedNotice) {
			unSupportOrderedNotice = checkNotNull(((UnSupportOrderedNotice)entry.getValue()).unSupportOrderedNotice(), "unSupportOrderedNotice can not be null");
		}
		return unSupportOrderedNotice;
	}
	private Set<String> getUnSupportInvoke(Entry<Class<?>, Object> entry) {
		Set<String> unSupportInvoke = Sets.newHashSet();
		if(entry.getValue() instanceof UnSupportInvoke) {
			unSupportInvoke = checkNotNull(((UnSupportInvoke)entry.getValue()).unSupportInvoke(), "unSupportInvoke can not be null");
		}
		return unSupportInvoke;
	}

	private Set<String> getMethods(Entry<Class<?>, Object> entry) {
		Set<String> methods = null;
		Set<String> interfaceMethods = getInterfaceMehtods(entry.getKey());
		if(entry.getValue() instanceof Picker
		&& (methods = ((Picker)entry.getValue()).pick()) != null) {
			if(methods.isEmpty()) {
				throw new RuntimeException(entry.getValue().getClass().getName() + " pick method must return data");
			}else {
				Set<String> tmp = Sets.newHashSet();
				for(String method : methods) {
					tmp.add(method.trim());
				}
				methods = tmp;
				if(methods.isEmpty()) {
					throw new RuntimeException(entry.getValue().getClass().getName() + " pick method must return data"); 
				}
			}
			methods.retainAll(interfaceMethods);
			if(methods.isEmpty()) {
				throw new RuntimeException(entry.getValue().getClass().getName() + " pick method must return data");
			}
		}else {
			methods = interfaceMethods;
		}
		for(Method method : entry.getKey().getMethods()) {
			if(methods.contains(method.getName()) && method.getReturnType().isArray()) {
				throw new ModernCoreException("unsupport array result").setCode(ModernCoreException.UNSUPPORT_ARRAY_RESULT);
			}
		}
		return methods;
	}

	private Set<String> getInterfaceMehtods(Class<?> clazz) {
		Set<String> interfaceMethods = Sets.newHashSet();
		for(Method method : clazz.getMethods()) {
			interfaceMethods.add(method.getName());
		}
		return interfaceMethods;
	}

	@Override
	public <T> T getBean(final Class<T> clazz) {
		checkNotNull(clazz);
		checkArgument(clazz.isInterface(), "clazz must be interface");
		try {
			final DefaultClient self = this;
			Object bean = beanCache.get(clazz, new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {clazz}, getBeanProxy(clazz, self));
				}
			});
			return clazz.cast(bean);
		}catch (Throwable e) {
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new ModernCoreException(e);
			}
		}
	}


	@Override
	public <T extends NoticeBean> T getNoticeBean(final Class<?> clazz) {
		checkNotNull(clazz);
		checkArgument(clazz.isInterface(), "clazz must be interface");
		try {
			final DefaultModernClient self = this;
			@SuppressWarnings("unchecked")
			T ret = (T) noticeBeanCache.get(clazz, new Callable<NoticeBean>() {
				@Override
				public NoticeBean call() throws Exception {
					return getNoticeBeanClazz().cast(Proxy.newProxyInstance(getClass().getClassLoader()
																			, new Class[] {getNoticeBeanClazz(), clazz}
																			, getNoticeBeanProxy(clazz, self))
													);
				}
			});
			return ret;
		}catch (Throwable e) {
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new ModernCoreException(e);
			}
		}
	}
	
	
	
	@Override
	public <T extends ExtBean> T getExtBean(final String clazzName) {
		checkNotNull(clazzName);
		try {
			final DefaultModernClient self = this;
			@SuppressWarnings("unchecked")
			T ret = (T) extBeanCache.get(clazzName, new Callable<ExtBean>() {
				@Override
				public ExtBean call() throws Exception {
					return getExtBeanClazz().cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {getExtBeanClazz()}, getExtBeanProxy(clazzName, self)));
				}
			});
			return ret;
		}catch (Throwable e) {
			if(e instanceof CoreException) {
				throw (CoreException)e;
			}else {
				throw new DefaultCoreException(e);
			}
		}
	}
	
	
	protected BaseProxy getBeanProxy(Class<?> clazz, DefaultClient client){
		return new BeanProxy(clazz, client);
	}
	
	protected Class<? extends ExtBean> getExtBeanClazz(){
		return BaseExtBean.class;
	}
	protected BaseProxy getExtBeanProxy(String clazzName, DefaultClient client) {
		return new ExtBeanProxy(clazzName, client);
	}
	
	protected Class<? extends NoticeBean> getNoticeBeanClazz() {
		return BaseNoticeBean.class;
	}
	protected BaseProxy getNoticeBeanProxy(Class<?> clazz, DefaultClient client) {
		return new NoticeBeanProxy(clazz, client);
	}
}
