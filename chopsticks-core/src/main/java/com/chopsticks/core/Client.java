package com.chopsticks.core;

import java.util.Set;

import com.chopsticks.core.caller.Caller;
import com.chopsticks.core.handler.Handler;

public interface Client extends Caller{
	
	public void register(Set<? extends Handler> handlers);
	
}
