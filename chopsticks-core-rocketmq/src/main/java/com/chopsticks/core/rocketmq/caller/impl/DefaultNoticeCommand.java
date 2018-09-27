package com.chopsticks.core.rocketmq.caller.impl;

import com.chopsticks.core.rocketmq.caller.BaseNoticeCommand;

public class DefaultNoticeCommand extends BaseNoticeCommand {
	
	public DefaultNoticeCommand(String topic, String tag, byte[] body) {
		super(topic, tag, body);
	}

}
