package com.chopsticks.core.rocketmq.caller;

import java.util.List;

import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

class OrderedMessageQueueSelector implements MessageQueueSelector {

	@Override
	public MessageQueue select(List<MessageQueue> mqs, Message msg, Object orderKey) {
		int idx = Math.abs(orderKey.hashCode());
		return mqs.get(idx % mqs.size());
	}

}
