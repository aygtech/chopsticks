package com.chopsticks.core.rocketmq.caller;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.chopsticks.core.caller.InvokeResult;

public abstract class BaseInvokeResult extends BaseResult implements InvokeResult {
	
	private byte[] body;
	
	public BaseInvokeResult(byte[] body) {
		this.body = body;
	}

	@Override
	public byte[] getBody() {
		return body;
	}
	
	public Object parseJson() {
		return JSON.parse(getBody());
	}
	public <T> T parseJsonObject(Class<T> clazz) {
		return JSON.parseObject(getBody(), clazz);
	}
	public <T> List<T> parseJsonArray(Class<T> clazz) {
		return JSON.parseArray(new String(getBody()), clazz);
	}

}
