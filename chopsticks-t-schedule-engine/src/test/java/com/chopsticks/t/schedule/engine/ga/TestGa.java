package com.chopsticks.t.schedule.engine.ga;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TestGa {
	
	@Test
	public void testCrossover1() throws Throwable{
		ThreadLocalRandom random = mock(ThreadLocalRandom.class);
		when(random.nextInt(Mockito.anyInt())).thenReturn(0);
		
		int[] a = {-1, 1, 3, -2, 4, -1, 5};
		int[] b = {-2, 1, 3, 4, 5};
		int[][] ret = Ga.crossover(random, a, b, 0.1f, -1, -2);
		assert Arrays.equals(ret[0], new int[] {-1, 1, 3, 4, 5});
		assert Arrays.equals(ret[1], new int[] {-2, 1, 3, 4, -1, 5});
	}
	
	@Test
	public void testCrossover2() throws Throwable{
		ThreadLocalRandom random = mock(ThreadLocalRandom.class);
		when(random.nextInt(Mockito.anyInt())).thenReturn(4);
		
		int[] a = {-1, 1, 3, -2, 4, -1, 5};
		int[] b = {-2, 1, 3, 4, 5};
		int[][] ret = Ga.crossover(random, a, b, 0.7f, -1, -2);
		
		assert Arrays.equals(ret[0], new int[] {-2, 1, 3, 4, -1, 5});
		assert Arrays.equals(ret[1], new int[] {-1, 1, 3, -2, 5, 4});
	}
	
	@Test
	public void testCrossover3() throws Throwable{
		ThreadLocalRandom random = mock(ThreadLocalRandom.class);
		when(random.nextInt(Mockito.anyInt())).thenReturn(2);
		
		int[] a = {-1, 1, 3, -2, 4, -1, 5};
		int[] b = {-2, 1, 3, 4, 5};
		int[][] ret = Ga.crossover(random, a, b, 0.7f, -1, -2);
		System.out.println(Arrays.toString(ret[0]));
		System.out.println(Arrays.toString(ret[1]));
		
		assert Arrays.equals(ret[0], new int[] {-2, 1, 3, 4, -1, 5});
		assert Arrays.equals(ret[1], new int[] {-1, 1, 3, 4, 5});
	}
}
