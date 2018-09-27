package com.chopsticks.core.modern.service;

import com.chopsticks.core.modern.entity.User;

public class UserServiceImpl implements UserService {

	@Override
	public void saveUser(User user) {
		System.out.println("saveUser : " + user);
	}
}
