package com.chopsticks.core.rocketmq.caller;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import com.chopsticks.common.concurrent.impl.DefaultTimeoutPromise;

public abstract class BaseInvokeSender{
	
	protected DefaultMQProducer producer;
	
	public BaseInvokeSender(DefaultMQProducer producer) {
		this.producer = producer;
	}
	
	public abstract void send(Message message, DefaultTimeoutPromise<BaseInvokeResult> promise);
	
	public void shutdown() {}
}
