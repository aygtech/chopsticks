package com.chopsticks.t.schedule.engine.ga;

public interface Codec {
	
	public Individual code(Object obj);
	
	public Object decode(Individual individual);
}
