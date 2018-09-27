package com.chopsticks.core.modern.service;

import java.util.List;
import java.util.Map;

import com.chopsticks.core.modern.entity.Order;
import com.chopsticks.core.modern.entity.OrderItem;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class OrderServiceImpl implements OrderService{
	@Override
	public List<Order> getAll() {
		List<Order> orders = Lists.newArrayList();
		for(int i = 0; i < 10; i++) {
			Order order = new Order();
			order.setId(i);
			orders.add(order);
		}
		System.out.println("getAll : " + orders);
		return orders;
	}
	@Override
	public Order getById(long id) {
		Order order = new Order();
		order.setId(id);
		OrderItem item = new OrderItem();
		item.setName("itmeName");
		order.setItems(Lists.newArrayList(item));
		System.out.println("getById : " + order);
		return order;
	}
	@Override
	public void saveOrder(Order order) {
		System.out.println("saveOrder : " + order);
	}
	
	@Override
	public void saveAll(List<Order> orders) {
		System.out.println("saveAll : " + orders);
	}
	@Override
	public Map<Long, Order> getAllByMap() {
		Map<Long, Order> orders = Maps.newHashMap();
		for(long i = 0; i < 10; i++) {
			Order order = new Order();
			order.setId(i);
			orders.put(i, order);
		}
		System.out.println("getAll : " + orders);
		return orders;
	}
	@Override
	public void saveAllByArr(Order[] orders) {
		System.out.println("saveAll : " + orders);
	}
}
