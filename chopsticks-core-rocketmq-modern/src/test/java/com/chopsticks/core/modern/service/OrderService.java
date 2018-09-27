package com.chopsticks.core.modern.service;

import java.util.List;
import java.util.Map;

import com.chopsticks.core.modern.entity.Order;

public interface OrderService {
	
	void saveOrder(Order order);

	Order getById(long id);

	List<Order> getAll();

	void saveAll(List<Order> orders);
	
	Map<Long, Order> getAllByMap();
	
	void saveAllByArr(Order[] orders);
}
