package com.chopsticks.core.rocketmq.handler;

import java.util.Map;

import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.DefaultClient;

abstract class BaseHandlerListener{
	
	
	/**
	 *  <topic + tag, baseHandler>
	 */
	private Map<String, BaseHandler> topicTagHandlers;
	private DefaultClient client;
	
	BaseHandlerListener(Map<String, BaseHandler> topicTagHandlers, DefaultClient client) {
		this.topicTagHandlers = topicTagHandlers;
		this.client = client;
	}
	
	protected BaseHandler getHandler(String topic, String tag) {
		BaseHandler handler = topicTagHandlers.get(topic + tag);
		if(handler == null) {
			handler = topicTagHandlers.get(topic + Const.ALL_TAGS);
		}
		return handler;
	}
	
	protected DefaultClient getClient() {
		return client;
	}
}
