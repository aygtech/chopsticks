package com.chopsticks.core.modern;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.chopsticks.core.modern.caller.ExtBean;
import com.chopsticks.core.modern.entity.Order;
import com.chopsticks.core.modern.service.OrderService;
import com.chopsticks.core.rockctmq.modern.DefaultModernClient;
import com.chopsticks.core.rockctmq.modern.caller.impl.DefaultExtNoticeCommand;
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.utils.Reflect;
import com.google.common.collect.Maps;

public class ByClientDelayNoticeTest {
	
	
	private static final TreeMap<Long, Integer> DELAY_LEVEL = Maps.newTreeMap();
	static {
		DELAY_LEVEL.put(TimeUnit.SECONDS.toMillis(1), 1);
		DELAY_LEVEL.put(TimeUnit.SECONDS.toMillis(5), 2);
		DELAY_LEVEL.put(TimeUnit.SECONDS.toMillis(10), 3);
		DELAY_LEVEL.put(TimeUnit.SECONDS.toMillis(30), 4);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(1), 5);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(2), 6);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(3), 7);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(4), 8);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(5), 9);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(6), 10);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(7), 11);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(8), 12);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(9), 13);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(10), 14);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(20), 15);
		DELAY_LEVEL.put(TimeUnit.MINUTES.toMillis(30), 16);
		DELAY_LEVEL.put(TimeUnit.HOURS.toMillis(1), 17);
		DELAY_LEVEL.put(TimeUnit.HOURS.toMillis(2), 18);
	}
	
	private static final String groupName = "testClientGroupName";
	
	public static void main(String[] args) {
		
		ModernClient client = new DefaultModernClient(groupName);
		((DefaultModernClient)client).setNamesrvAddr("localhost:9876");
		
		Reflect.on(Const.class).call("setDelayLevel", DELAY_LEVEL);
		try {
			client.start();
			ExtBean orderServiceExt = client.getExtBean(OrderService.class);
			
			Order order = new Order();
			order.setId(1L);
			
			System.out.println("saveOrder : " + orderServiceExt.notice(new DefaultExtNoticeCommand("saveOrder", order), 8L, TimeUnit.SECONDS).getId());
			
		}finally {
			client.shutdown();
		}
	}
	
	
}
