package com.chopsticks.core.modern;

import java.util.List;
import java.util.Map;

import com.chopsticks.core.modern.entity.Order;
import com.chopsticks.core.modern.entity.User;
import com.chopsticks.core.modern.service.OrderService;
import com.chopsticks.core.modern.service.UserService;
import com.chopsticks.core.rockctmq.modern.DefaultModernClient;

public class ByClientTest {
	
	private static final String groupName = "testClientGroupName";
	
	public static void main(String[] args) {
		
		DefaultModernClient client = new DefaultModernClient(groupName);
		client.setNamesrvAddr("localhost:9876");
		try {
			client.start();
			OrderService orderService = client.getBean(OrderService.class);
			UserService userService = client.getBean(UserService.class);
			
			Order order = new Order();
			order.setId(1L);
			
			orderService.saveOrder(order);
			System.out.println("saveOrder end");
			System.out.println("getById : " + orderService.getById(5L));
			List<Order> orders = orderService.getAll();
			System.out.println("getAll : " + orders);
			orderService.saveAll(orders);
			System.out.println("saveAll end");
			Map<Long, Order> ordersByMap = orderService.getAllByMap();
			System.out.println("getAllByMap : " + ordersByMap);
//			orderService.saveAllByArr(orders.toArray(new Order[0]));
//			System.out.println("saveAllByArr end");
			
			
			User user = new User();
			user.setAge(11);
			user.setName("asdasdasdsadsad");
			user.setId(1111L);
			userService.saveUser(user);
			System.out.println("saveUser end");
 		}finally {
			client.shutdown();
		}
		
	}
	
	
}
