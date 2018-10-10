package com.chopsticks.core.modern.service;

import com.chopsticks.core.modern.entity.User;
import com.chopsticks.core.modern.handler.ModernNoticeContext;
import com.chopsticks.core.modern.handler.ModernNoticeContextAware;

public class UserServiceImpl implements UserService, ModernNoticeContextAware {
	
	private ThreadLocal<ModernNoticeContext> context;
	
	@Override
	public void saveUser(User user) {
		if(this.context.get() != null) {
			System.out.println("notice : " + this.context.get().getId() + ", saveUser : " + user );
		}else {
			System.out.println("saveUser : " + user);
		}
	}

	@Override
	public void setNoticeContext(ThreadLocal<ModernNoticeContext> context) {
		this.context = context;
	}
}
