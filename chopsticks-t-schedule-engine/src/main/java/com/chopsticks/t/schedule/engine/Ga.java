package com.chopsticks.t.schedule.engine;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.primitives.Ints;

public class Ga {
	
	public static int[][] crossover(int[] a, int[] b, float varLenRatio, int max, int x, int y) {
		int[][] ret;
		int varLen = Math.round(varLenRatio * max);
		int minLen = Math.min(a.length, b.length);
		int croIdx = ThreadLocalRandom.current().nextInt(minLen);
		int end = croIdx + varLen;
		int[] aPart = Arrays.copyOfRange(a, croIdx, end > a.length ? a.length : end);
		int[] bPart = Arrays.copyOfRange(b, croIdx, end > b.length ? b.length : end);
		if(aPart.length == bPart.length && Arrays.equals(aPart, bPart)) {
			ret = new int[][] {a.clone(), b.clone()};
		}else {
			ret = new int[2][];
			if(bPart.length > aPart.length && Ints.indexOf(bPart, aPart) != -1) {
				ret[0] = b.clone();
			}else {
				ret[0] = crossover1(b, aPart, croIdx, max, x, y);
			}
			if(aPart.length > bPart.length && Ints.indexOf(aPart, bPart) != -1) {
				ret[1] = a.clone();
			}else {
				ret[1] = crossover1(a, bPart, croIdx, max, x, y);
			}
		}
		return ret;
	}
	public static int[] mutation(int src[], float varLenRatio, int max, int x, int y) {
		int len = Math.round(varLenRatio * max);
		int aMutIdx = ThreadLocalRandom.current().nextInt(src.length);
		int bMutIdx = ThreadLocalRandom.current().nextInt(src.length);
		if(aMutIdx == bMutIdx) {
			return src.clone();
		}else {
			return mutation1(src, aMutIdx, bMutIdx, len, x, y);
		}
	}
	private static int[] crossover1(int[] src, int[] part, int croIdx, int max, int x, int y) {
		int[] tmp = new int[src.length + part.length];
		System.arraycopy(src, 0, tmp, 0, croIdx);
		System.arraycopy(part, 0, tmp, croIdx, part.length);
		System.arraycopy(src, croIdx, tmp, croIdx + part.length,  src.length - croIdx);
		return filterByCrossover(tmp, max, x, y);
	}
	private static int[] filterByCrossover(int[] src, int max, int x, int y) {
		int[] tmp = src.clone();
		BitSet exist = new BitSet(max);
		int cur = 0;
		int prev = 0;
		int size = 0;
		int yNextIdx = 0;
		for(; size < tmp.length; size++) {
			cur = tmp[size];
			if(cur == x || cur == y) {
				if(prev == x || prev == y) {
					if(size != tmp.length - 1) {
						System.arraycopy(tmp, size + 1, tmp, size, tmp.length - size - 1);
						size--;
					}
					tmp[tmp.length - 1] = 0;
				}else {
					if(cur == y) {
						if(Math.abs(yNextIdx) > 0) {
							yNextIdx = Math.abs(yNextIdx);
							System.arraycopy(tmp, size + 1, tmp, size, tmp.length - size - 1);
							size--;
							tmp[tmp.length - 1] = 0;
						}else {
							prev = cur;
							yNextIdx = size + 1;
						}
					}else{
						yNextIdx = yNextIdx > 0 ? -yNextIdx: yNextIdx;
						prev = cur;
					}
				}
			}else if(cur == 0){
				if(prev == x || prev == y) {
					tmp[size - 1] = 0;
					size--;
				}
				break;
			}else{
				if(exist.get(cur)) {
					if(size != tmp.length - 1) {
						System.arraycopy(tmp, size + 1, tmp, size, tmp.length - size - 1);
						size--;
					}
					tmp[tmp.length - 1] = 0;
				}else {
					exist.set(cur);
					if(yNextIdx > 0) {
						if(yNextIdx == size) {
							prev = cur;
							yNextIdx++;
						}else {
							System.arraycopy(tmp, yNextIdx, tmp, yNextIdx + 1, size - yNextIdx);
							tmp[yNextIdx] = cur;
							yNextIdx++;
						}
					}else {
						prev = cur;
					}
				}
			}
		}
		if(tmp[0] != x && tmp[0] != x) {
			int add;
			if(yNextIdx != 0) {
				add = x;
			}else {
				add = y;
			}
			if(size == tmp.length) {
				tmp = Arrays.copyOf(tmp, size + 1);
			}
			System.arraycopy(tmp, 0, tmp, 1, size);
			tmp[0] = add;
			size++;
		}
		return size == tmp.length ? tmp : Arrays.copyOf(tmp, size);
	}
	
	public static int[] mutation1(int[] src, int aMutIdx, int bMutIdx, int len, int x, int y) {
		int[] tmp = src.clone();
		int c;
		for(int i = 0, aIdx = aMutIdx + i, bIdx = bMutIdx + i
			; i < len && aIdx < src.length && bIdx < src.length
			; i++, aIdx = aMutIdx + i, bIdx = bMutIdx + i) {
			c = tmp[aIdx];
			tmp[aIdx] = tmp[bIdx];
			tmp[bIdx] = c;
		}
		return filterByMutation(tmp, x, y);
	}
	private static int[] filterByMutation(int[] src, int x, int y) {
		int[] tmp = src.clone();
		int cur = 0;
		int prev = 0;
		int size = 0;
		for(; size < tmp.length; size++) {
			cur = tmp[size];
			if(cur == x || cur == y) {
				if(prev == x || prev == y) {
					if(size != tmp.length - 1) {
						System.arraycopy(tmp, size + 1, tmp, size, tmp.length - size - 1);
						size--;
					}
					tmp[tmp.length - 1] = 0;
				}else {
					prev = cur;
				}
			}else if(cur == 0){
				if(prev == x || prev == y) {
					tmp[size - 1] = 0;
					size--;
				}
				break;
			}else{
				prev = cur;
			}
		}
		return size == tmp.length ? tmp : Arrays.copyOf(tmp, size);
	}
}
