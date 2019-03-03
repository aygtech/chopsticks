package com.chopsticks.core.rocketmq.caller.impl;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import com.chopsticks.core.concurrent.impl.DefaultTimeoutPromise;
import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;
import com.chopsticks.core.rocketmq.caller.InvokeSender;

public class SingleInvokeSender extends InvokeSender{
	
	
	public SingleInvokeSender(DefaultMQProducer producer) {
		super(producer);
	}
	
	@Override
	public void send(Message message, DefaultTimeoutPromise<BaseInvokeResult> promise) {
		try {
			producer.send(message, new SingleInvokeSendCallback(promise));
		}catch (Throwable e) {
			promise.setException(e);
		}
	}
	
}
