package com.chopsticks.core.rocketmq.modern.caller;

import java.util.Map;
import java.util.Set;

import com.chopsticks.core.modern.caller.ModernCommand;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class BaseModernCommand implements ModernCommand {
	private String method;
	private Object[] params;
	private Set<String> traceNos = Sets.newHashSet();
	private Map<String, String> extParams = Maps.newHashMap();
	
	public BaseModernCommand(String method, Object... params) {
		this.method = method;
		this.params = params;
		if(params != null) {
			for(Object param : params) {
				Preconditions.checkNotNull(param);
			}
		}
	}
	public void setMethod(String method) {
		this.method = method;
	}
	@Override
	public String getMethod() {
		return method;
	}
	public void setParams(Object[] params) {
		this.params = params;
		if(params != null) {
			for(Object param : params) {
				Preconditions.checkNotNull(param);
			}
		}
	}
	@Override
	public Object[] getParams() {
		return params;
	}
	public void setTraceNos(Set<String> traceNos) {
		this.traceNos = traceNos;
	}
	public Set<String> getTraceNos() {
		return traceNos;
	}
	public void setExtParams(Map<String, String> extParams) {
		this.extParams = extParams;
	}
	public Map<String, String> getExtParams() {
		return extParams;
	}
	
	/**
	 * 添加单次调用扩展参数
	 * @param key 扩展键
	 * @param value 扩展值
	 * @return 当前对象
	 */
	public <T extends BaseModernCommand> T addExtParam(String key, String value) {
		getExtParams().put(key, value);
		@SuppressWarnings("unchecked")
		T ret = (T) this;
		return ret;
	}
	/**
	 * 添加轨迹标识
	 * @param traceNo 轨迹标识
	 * @return 当前对象
	 */
	public <T extends BaseModernCommand> T addTraceNo(String traceNo) {
		getTraceNos().add(traceNo);
		@SuppressWarnings("unchecked")
		T ret = (T) this;
		return ret;
	}
}
