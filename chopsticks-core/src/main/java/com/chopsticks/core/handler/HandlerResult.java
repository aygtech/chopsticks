package com.chopsticks.core.handler;

import com.chopsticks.common.concurrent.Promise;

public interface HandlerResult{
	
	public byte[] getBody();
	
	public Promise<HandlerResult> getPromise();

}
