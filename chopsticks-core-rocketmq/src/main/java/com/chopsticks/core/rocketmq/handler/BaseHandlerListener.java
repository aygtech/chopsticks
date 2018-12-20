package com.chopsticks.core.rocketmq.handler;

import java.util.Map;

import com.chopsticks.core.rocketmq.Const;

abstract class BaseHandlerListener{
	
	
	/**
	 *  <topic + tag, baseHandler>
	 */
	private Map<String, BaseHandler> topicTagHandlers;
	private String groupName;
	
	BaseHandlerListener(Map<String, BaseHandler> topicTagHandlers, String groupName) {
		this.topicTagHandlers = topicTagHandlers;
		this.groupName = groupName;
	}
	
	protected BaseHandler getHandler(String topic, String tag) {
		BaseHandler handler = topicTagHandlers.get(topic + tag);
		if(handler == null) {
			handler = topicTagHandlers.get(topic + Const.ALL_TAGS);
		}
		return handler;
	}
	
	protected String getGroupName() {
		return groupName;
	}
}
