package com.chopsticks.t.schedule.engine.ga.entity;

public class Chromosome {
	private int[] gene;
	private double fitness;
	public int[] getGene() {
		return gene;
	}
	public void setGene(int[] gene) {
		this.gene = gene;
	}
	public double getFitness() {
		return fitness;
	}
	public void setFitness(double fitness) {
		this.fitness = fitness;
	}
}
