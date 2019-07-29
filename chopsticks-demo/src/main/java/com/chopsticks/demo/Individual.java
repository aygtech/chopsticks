package com.chopsticks.demo;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

public abstract class Individual implements Comparable<Individual>{
	
	private static final int X = -1;
	private static final int Y = -2;
	
	private int[] gene;
	
	public int[] getGene() {
		return gene;
	}

	public void setGene(int[] gene) {
		this.gene = gene;
	}

	public abstract double fitness();
	
	@Override
	public int compareTo(Individual o) {
		if(fitness() >= o.fitness()){
			return -1;
		}else {
			return 1;
		}
	}
	public abstract Individual newInstance();
	public abstract Individual code();
	public abstract Individual decode();
	
	public Individual reproduction(Individual individual, float crossoverLenRadio, double mutationRadio, float mutationLenRadio) throws Throwable{
		int[] tmpGene = crossover(gene, individual.gene, crossoverLenRadio);
		if(mutationRadio > ThreadLocalRandom.current().nextDouble()) {
			tmpGene = mutation(tmpGene, mutationLenRadio);
		}
		Individual ret = newInstance();
		ret.setGene(tmpGene);
		return ret;
	}
	private static int[] crossover(int[] a, int[] b, float crossoverLenRadio) {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		int idx = r.nextInt(Math.min(a.length, b.length));
		if(idx == b.length - 1) {
			return b;
		}
		int crossoverLen = Math.max(Math.round(a.length * crossoverLenRadio), 1);
		int[] part = Arrays.copyOfRange(a, idx, Math.min(idx + crossoverLen, a.length));
		int[] ret = new int[b.length + part.length];
		System.arraycopy(b, 0, ret, 0, idx);
		System.arraycopy(part, 0, ret, idx, part.length);
		System.arraycopy(b.length, idx, ret, idx + part.length, b.length - idx);
		ret = filterByCrossover(ret);
		return ret;
	}
	private static int[] filterByCrossover(int[] ret) {
		int emptyVal = 0, emptyIdx = -1;
		int prev = emptyVal, delIdx = emptyIdx, cur;
		int y1Begin = emptyIdx, y1Len = emptyVal;
		int y2Begin = emptyIdx, y2Len = emptyVal;
		BitSet cache = new BitSet(ret.length);
		int i = emptyVal, len = ret.length;
		for(; i < len; i++) {
			cur = ret[i];
			switch (cur) {
			case X: 
			case Y:
				if(prev == X || prev == Y) { 
					if(delIdx == emptyIdx) { 
						// x, 1, x, x ==> x, 1, x, "x"
						delIdx = i;
					}else {	
						// x, 1, x, 1, x ==> x, 1, x, "1", "x"
					}
				}else {	
					if(delIdx != emptyIdx){	
						System.arraycopy(ret, i, ret, delIdx, len - i);
						len = len - (i - delIdx);
						i = delIdx; 
						delIdx = emptyIdx;
					}
					if(cur == Y) {
						if(y1Begin == emptyIdx) {
							y1Begin = i;
							y1Len++;
						}else if(y2Begin == emptyIdx) {
							y2Begin = i;
							y2Len++;
						}
					}
					if(cur == X) {
						y1Len = y1Len > emptyVal ? -y1Len : y1Len;
						y2Len = y2Len > emptyVal ? -y2Len : y2Len;
					}
					prev = cur;
				}
				break;
			default:
				if(cache.get(cur)) {
					if(delIdx == emptyIdx) { 
						delIdx = i;
					}else {	
						// ignore
					}
				}else {
					if(delIdx != emptyIdx){	
						System.arraycopy(ret, i, ret, delIdx, len - i);
						len = len - (i - delIdx);
						i = delIdx; 
						delIdx = emptyIdx;
					}
					cache.set(cur);
					prev = cur;
					if(y2Len > emptyVal) {
						y2Len++;
					}else if(y1Len > emptyVal) {
						y1Len++;
					}
				}
				break;
			}
		}
		if(delIdx != emptyIdx){	
			System.arraycopy(ret, i, ret, delIdx, len - i);
			len = len - (i - delIdx);
			i = delIdx; 
			delIdx = emptyIdx;
		}
		int last = ret[len - 1];
		if(last == X || last == Y){
			len--;
		}
		if(y2Len != emptyVal) {
			y2Len = y2Len > emptyVal ? y2Len : -y2Len;
			y1Len = y1Len > emptyVal ? y1Len : -y1Len;
			ret = merge(ret, len, y1Begin, y1Len, y2Begin, y2Len);
			len--;
		}
		return Arrays.copyOf(ret, len);
	}
	private static int[] merge(int[] ret, int len, int y1Begin, int y1Len, int y2Begin, int y2Len) {
		int y1End = y1Begin + y1Len;
		int y2End = y2Begin + y2Len;
		int[] part = Arrays.copyOfRange(ret, y2Begin + 1, y2End);
		if(y2End < len) {
			System.arraycopy(ret, y2End, ret, y2Begin, len - y2End);
		}
		System.arraycopy(ret, y1End, ret, y1End + part.length, len - (y1End + part.length));
		System.arraycopy(part, 0, ret, y1Begin + y1Len, part.length);
		return ret;
	}

	private static int[] mutation(int[] ret, float mutationLenRadio) {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		int idx1 = r.nextInt(ret.length);
		int idx2 = r.nextInt(ret.length);
		if(idx1 == idx2) {
			return ret;
		}
		if(idx1 > idx2) {
			idx1 ^=  idx2;
			idx2 ^=  idx1;
			idx1 ^=  idx2;
		}
		int mutationLen = Math.max(Math.round(ret.length * mutationLenRadio), 1);
		int x, y;
		for(int i = 0; i < mutationLen; i++) {
			x = (idx2 + i) % ret.length;
			y = (idx1 + i) % ret.length;
			ret[x] = ret[x] ^ ret[y];
			ret[y] = ret[x] ^ ret[y];
			ret[x] = ret[x] ^ ret[y];
		}
		ret = filterByMutation(ret);
		return ret;
	}
	
	private static int[] filterByMutation(int[] ret) {
		int emptyVal = 0, emptyIdx = -1;
		int prev = emptyVal, delIdx = emptyIdx, cur;
		int i = emptyVal, len = ret.length;
		for(; i < len; i++) {
			cur = ret[i];
			switch (cur) {
			case X:
			case Y:
				if(prev == X || prev ==Y) {
					if(delIdx == emptyIdx) {
						delIdx = i;
					}else {
						
					}
				}else {
					if(delIdx != emptyIdx) {
						System.arraycopy(ret, i, ret, delIdx, len - i);
						len = len - (i - delIdx);
						i = delIdx; 
						delIdx = emptyIdx;
					}
					prev = cur;
				}
				break;
			default:
				prev = cur;
				break;
			}
		}
		if(delIdx != emptyIdx){	
			System.arraycopy(ret, i, ret, delIdx, len - i);
			len = len - (i - delIdx);
			i = delIdx; 
			delIdx = emptyIdx;
		}
		int first = ret[0];
		if(first != X && first != Y) {
			if(len == ret.length) {
				ret = Arrays.copyOf(ret, len + 1);
			}
			System.arraycopy(ret, 0, ret, 1, len);
			len++;
			ret[0] = X;
		}
		return ret;
	}

	public static void main(String[] args) {
		Stopwatch watch = Stopwatch.createStarted();
		int[] src = {-1, 1, 1, 2, 3, 4, -2, 1, 5, -1, 9, 1, 2, -2, -1, 6, 7, 5, -1, -1, 3, 5};
		int[] ret = null;
		int len = 1000000;
		for(int i = 0; i < len; i++) {
			ret = Individual.filterByCrossover(src.clone());
		}
		System.out.println(Arrays.toString(src));
		System.out.println(Arrays.toString(ret));
		long sum = watch.elapsed(TimeUnit.MICROSECONDS);
		System.out.println(1.0d * sum / len);
		
		watch = Stopwatch.createStarted();
		src = new int[]{8, -1, 1, 2, 3, 4, -2, 5, -1, 9, -1, 6, 7};
		ret = null;
		for(int i = 0; i < len; i++) {
			ret = Individual.filterByMutation(src.clone());
		}
		System.out.println(Arrays.toString(src));
		System.out.println(Arrays.toString(ret));
		sum = watch.elapsed(TimeUnit.MICROSECONDS);
		System.out.println(1.0d * sum / len);
	}
	
}
