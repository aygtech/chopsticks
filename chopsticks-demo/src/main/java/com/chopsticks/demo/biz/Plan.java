package com.chopsticks.demo.biz;

import com.chopsticks.demo.Individual;

public class Plan extends Individual {

	@Override
	public double fitness() {
		return 0;
	}

	@Override
	public Individual newInstance() {
		return null;
	}

	@Override
	public Individual code() {
		return null;
	}

	@Override
	public Individual decode() {
		return null;
	}

}
