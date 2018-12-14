package com.chopsticks.core.modern.service;

import com.chopsticks.core.modern.entity.User;
import com.chopsticks.core.rocketmq.modern.handler.DefaultModernNoticeContextAware;
import com.chopsticks.core.rocketmq.modern.handler.DefaultModernNoticeContextHolder;

public class UserServiceImpl implements UserService, DefaultModernNoticeContextAware {
	
	private DefaultModernNoticeContextHolder holder;
	
	@Override
	public void saveUser(User user) {
		if(this.holder.get() != null) {
			System.out.println("notice : " + this.holder.get().getId() + ", saveUser : " + user.getId());
		}else {
			System.out.println("saveUser : " + user);
		}
	}

	@Override
	public void setNoticeContextHolder(DefaultModernNoticeContextHolder holder) {
		this.holder = holder;
	}

}
