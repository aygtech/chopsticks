package com.chopsticks.core.rocketmq.caller;

import com.chopsticks.core.caller.NoticeCommand;

public abstract class BaseNoticeCommand extends BaseCommand implements NoticeCommand{

	public BaseNoticeCommand(String topic, String tag, byte[] body) {
		super(topic, tag, body);
	}
	
}
