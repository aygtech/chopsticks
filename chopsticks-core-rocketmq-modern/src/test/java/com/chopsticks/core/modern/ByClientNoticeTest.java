package com.chopsticks.core.modern;

import com.chopsticks.core.modern.caller.NoticeBean;
import com.chopsticks.core.modern.entity.Order;
import com.chopsticks.core.modern.entity.User;
import com.chopsticks.core.modern.service.OrderService;
import com.chopsticks.core.modern.service.UserService;
import com.chopsticks.core.rocketmq.modern.DefaultModernClient;
import com.chopsticks.core.rocketmq.modern.caller.impl.DefaultModernNoticeCommand;

public class ByClientNoticeTest {
	
	private static final String groupName = "testClientGroupName";
	
	public static void main(String[] args) {
		
		ModernClient client = new DefaultModernClient(groupName);
		((DefaultModernClient)client).setNamesrvAddr("localhost:9876");
		try {
			client.start();
			NoticeBean noticeOrderService = client.getNoticeBean(OrderService.class);
			NoticeBean noticeUserService = client.getNoticeBean(UserService.class);
			Order order = new Order();
			order.setId(1L);
			
			for(int i = 0; i < 5; i++) {
				System.out.println("saveOrder : " + noticeOrderService.notice(new DefaultModernNoticeCommand("saveOrder", order)).getId());
				System.out.println("getById : " + noticeOrderService.notice(new DefaultModernNoticeCommand("getById", 5L)).getId());
			}
			User user = new User();
			user.setId(11L);
			System.out.println("saveUser : " + noticeUserService.notice(new DefaultModernNoticeCommand("saveUser", user)).getId());
		}finally {
			client.shutdown();
		}
	}
	
	
}
