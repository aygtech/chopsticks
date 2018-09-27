package com.chopsticks.core.modern;

import java.util.Map;

import com.chopsticks.core.modern.service.OrderService;
import com.chopsticks.core.modern.service.OrderServiceImpl;
import com.chopsticks.core.modern.service.UserService;
import com.chopsticks.core.modern.service.UserServiceImpl;
import com.chopsticks.core.rockctmq.modern.DefaultModernClient;
import com.google.common.collect.Maps;

public class ByServerTest {
	
	private static final String groupName = "testServerGroupName";
	
	public static void main(String[] args) throws Throwable{
		ModernClient client = new DefaultModernClient(groupName);
		((DefaultModernClient)client).setNamesrvAddr("localhost:9876");
		
		Map<Class<?>, Object> services = Maps.newHashMap();
		services.put(OrderService.class, new OrderServiceImpl());
		services.put(UserService.class, new UserServiceImpl());
		client.register(services);
		try {
			client.start();
			System.out.println("start success, press any key shutdown");
			System.in.read();
		}finally {
			client.shutdown();
		}
	}
}
