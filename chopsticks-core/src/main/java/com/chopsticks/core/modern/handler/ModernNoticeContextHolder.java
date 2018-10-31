package com.chopsticks.core.modern.handler;

public class ModernNoticeContextHolder<T extends ModernNoticeContext> {
	
	private ThreadLocal<T> ctx = new ThreadLocal<T>();
	
	public void set(T ctx) {
		this.ctx.set(ctx);
	}
	
	public T get() {
		return this.ctx.get();
	}
	
	public void remove() {
		this.ctx.remove();
	}
	
}
