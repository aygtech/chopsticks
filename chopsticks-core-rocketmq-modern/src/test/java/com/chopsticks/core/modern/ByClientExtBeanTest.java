package com.chopsticks.core.modern;

import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.chopsticks.core.caller.InvokeResult;
import com.chopsticks.core.modern.caller.ExtBean;
import com.chopsticks.core.rockctmq.modern.DefaultModernClient;
import com.chopsticks.core.rockctmq.modern.caller.impl.DefaultModernInvokeCommand;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

public class ByClientExtBeanTest {
	private static final String groupName = "testClientGroupName";
	public static void main(String[] args) {
		ModernClient client = new DefaultModernClient(groupName);
		((DefaultModernClient)client).setNamesrvAddr("localhost:9876");
		try {
			client.start();
			ExtBean extOrderService = client.getExtBean("com.chopsticks.core.modern.service.OrderService");
			Map<String, Object> order = Maps.newHashMap();
			order.put("id", 111L);
			order.put("@type", "com.chopsticks.core.modern.entity.Order");
			
			extOrderService.invoke(new DefaultModernInvokeCommand("saveOrder", order));
			System.out.println("saveOrder end");
			InvokeResult ret = extOrderService.invoke(new DefaultModernInvokeCommand("getAll"));
			String getAllStr = new String(ret.getBody(), Charsets.UTF_8);
			System.out.println("getAll : " + getAllStr);
			JSONArray jsonArray = JSON.parseArray(getAllStr);
			for(int i = 0; i < jsonArray.size(); i++) {
				jsonArray.getJSONObject(i).put("@type", "com.chopsticks.core.modern.entity.Order");
			}
			ret = extOrderService.invoke(new DefaultModernInvokeCommand("saveAll", jsonArray));
			System.out.println("saveAll end");
		}finally {
			client.shutdown();
		}
	}
}
