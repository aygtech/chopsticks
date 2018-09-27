package com.chopsticks.core.handler;

public interface Handler{
	
	public String getMethod();
	
	public HandlerResult invoke(InvokeParams params, InvokeContext ctx);
	
	public void notice(NoticeParams params, NoticeContext ctx);
}
