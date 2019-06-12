package com.chopsticks.core.rocketmq.caller;

public class OrderedNoticeRequest extends BaseRequest{
	private Object orderKey;

	public Object getOrderKey() {
		return orderKey;
	}

	public void setOrderKey(Object orderKey) {
		this.orderKey = orderKey;
	}
	
}
