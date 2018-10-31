package com.chopsticks.core.rocketmq.caller;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import com.chopsticks.core.concurrent.impl.GuavaTimeoutPromise;

public abstract class InvokeSender{
	
	protected DefaultMQProducer producer;
	
	public InvokeSender(DefaultMQProducer producer) {
		this.producer = producer;
	}
	
	public abstract void send(Message message, GuavaTimeoutPromise<BaseInvokeResult> promise);
	
	public void shutdown() {}
}
