package com.chopsticks.t.schedule.engine.ga.impl;

import com.chopsticks.t.schedule.engine.ga.Individual;

public class ArrayIndividual implements Individual{
	
	private int[] genes;
	
	private int max;
	private int x;
	private int y;

	@Override
	public int[] genes() {
		return this.genes;
	}

	@Override
	public double getFitness() {
		return 0;
	}

}
