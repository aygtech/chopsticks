package com.chopsticks.core.modern.service;

import com.chopsticks.core.modern.entity.User;
import com.chopsticks.core.rocketmq.modern.handler.ModernContextHolder;

public class UserServiceImpl implements UserService {
	
	
	@Override
	public void saveUser(User user) {
		if(ModernContextHolder.getNoticeContext() != null) {
			System.out.println("notice : " + ModernContextHolder.getNoticeContext().getId() + ", saveUser : " + user.getId());
		}else {
			System.out.println("saveUser : " + user);
		}
	}

}
