package com.chopsticks.t.schedule.engine.utils;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.MinMaxPriorityQueue;

import me.lemire.integercompression.differential.IntegratedIntCompressor;

public class TestEngineUtils {
	@Test
	public void test() {
		MinMaxPriorityQueue<Integer> queue = MinMaxPriorityQueue.maximumSize(5).create();

		queue.add(1);
		queue.add(3);
		queue.add(2);
		queue.add(5);
		queue.add(-1);
		queue.add(-1);
		queue.add(7);
		while(!queue.isEmpty()) {
			System.out.println(queue.poll());
		}

	}
	@Test
	public void testArray() {
		int[] arr = new int[300];
		for(int i = 0; i < arr.length; i++) {
			arr[i] = i + 1;
		}
		for(int i = 0; i < arr.length/10; i++) {
			int idx = ThreadLocalRandom.current().nextInt(arr.length);
			arr[idx] = -1;
		}
		System.out.println(Arrays.toString(arr));
		System.out.println(arr.length);
		IntegratedIntCompressor iic = new IntegratedIntCompressor();
		Stopwatch watch = Stopwatch.createStarted();
		int[] arr2 = iic.compress(arr);
		System.out.println(watch.elapsed(TimeUnit.MILLISECONDS));
		System.out.println(Arrays.toString(arr2));
		System.out.println(arr2.length);
		
	}
}
