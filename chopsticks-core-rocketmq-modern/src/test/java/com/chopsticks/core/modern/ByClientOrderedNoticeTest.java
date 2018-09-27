package com.chopsticks.core.modern;

import java.util.UUID;

import com.chopsticks.core.modern.caller.ExtBean;
import com.chopsticks.core.modern.entity.Order;
import com.chopsticks.core.modern.service.OrderService;
import com.chopsticks.core.rockctmq.modern.DefaultModernClient;
import com.chopsticks.core.rockctmq.modern.caller.impl.DefaultExtNoticeCommand;

public class ByClientOrderedNoticeTest {
	
	private static final String groupName = "testClientGroupName";
	
	public static void main(String[] args) {
		
		ModernClient client = new DefaultModernClient(groupName);
		((DefaultModernClient)client).setNamesrvAddr("localhost:9876");
		
		try {
			client.start();
			ExtBean orderServiceExt = client.getExtBean(OrderService.class);
			
			Order order = new Order();
			order.setId(1L);
			
			Object orderKey = UUID.randomUUID().toString();
			for(int i = 0; i < 100; i++) {
				System.out.println("saveOrder : " + orderServiceExt.notice(new DefaultExtNoticeCommand("saveOrder", order), orderKey).getId());
				System.out.println("getById : " + orderServiceExt.notice(new DefaultExtNoticeCommand("getById", 5L), orderKey).getId());
			}
		}finally {
			client.shutdown();
		}
	}
}
