package com.chopsticks.core.caller;

public interface Command {
	public String getMethod();
	public byte[] getBody();
}
