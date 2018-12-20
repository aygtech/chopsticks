package com.chopsticks.core.rocketmq.modern.handler;

import java.util.Set;

public interface Picker {
	
	public Set</*methodName*/String> pick();
}
