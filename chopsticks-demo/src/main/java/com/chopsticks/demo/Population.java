package com.chopsticks.demo;

import com.google.common.collect.MinMaxPriorityQueue;

public class Population {
	
	private MinMaxPriorityQueue<Individual> queue;
	private double sum;
	
	public Population(int maximumSize, Iterable<? extends Individual> initialContents) {
		queue = MinMaxPriorityQueue.expectedSize(maximumSize).maximumSize(maximumSize).create(initialContents);
		Individual[] tmp = queue.toArray(new Individual[0]);
		for(int i = 0; i < tmp.length; i++) {
			sum += tmp[0].fitness();
		}
	}
	
	public boolean add(Individual individual) {
		return queue.add(individual);
	}
	
	public Individual select() {
		return null;
	}
}
