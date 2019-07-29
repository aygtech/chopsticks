package com.chopsticks.t.schedule.engine.ga;

import java.util.Set;

public interface Population extends Selection{
	
	public boolean add(Individual individual);
	
	public boolean add(Set<Individual> individuals);
	
}
