package com.chopsticks.core.rocketmq.caller;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import com.chopsticks.common.concurrent.impl.DefaultTimeoutPromise;

public abstract class InvokeSender{
	
	protected DefaultMQProducer producer;
	
	public InvokeSender(DefaultMQProducer producer) {
		this.producer = producer;
	}
	
	public abstract void send(Message message, DefaultTimeoutPromise<BaseInvokeResult> promise);
	
	public void shutdown() {}
}
