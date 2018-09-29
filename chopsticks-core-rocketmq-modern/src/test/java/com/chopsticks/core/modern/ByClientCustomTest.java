package com.chopsticks.core.modern;

import java.util.List;

import com.alibaba.fastjson.annotation.JSONType;
import com.chopsticks.core.rockctmq.modern.DefaultModernClient;
import com.chopsticks.core.rockctmq.modern.caller.Endpoint;

public class ByClientCustomTest {
	private static final String groupName = "testClientGroupName";

	public static void main(String[] args) {

		ModernClient client = new DefaultModernClient(groupName);
		((DefaultModernClient) client).setNamesrvAddr("localhost:9876");
		try {
			client.start();
			CustomOrderService orderService = client.getBean(CustomOrderService.class);
			CustomOrder order = new CustomOrder();
			order.setId(1L);
			orderService.customSaveOrder(order);
			System.out.println("saveOrder end");
			System.out.println("getAll : " + orderService.customGetAll());
		} finally {
			client.shutdown();
		}
	}
	
	@Endpoint("com.chopsticks.core.modern.service.OrderService")
	public static interface CustomOrderService{
		@Endpoint("saveOrder")
		public void customSaveOrder(CustomOrder order);
		@Endpoint("getAll")
		public List<CustomOrder> customGetAll();
	}
	@JSONType(typeName = "com.chopsticks.core.modern.entity.Order")
	public static class CustomOrder {
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}
}
