package com.chopsticks.t.schedule.engine.ga;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;

import com.chopsticks.common.utils.MoreInts;

public class Ga {
	
	public static int[][] crossover(int[] a, int[] b, float varLenRatio, int x, int y) {
		int[][] ret;
		int minLen = Math.min(a.length, b.length);
		int croIdx = ThreadLocalRandom.current().nextInt(minLen);
		int varLen = Math.round(varLenRatio * minLen);
		int end = croIdx + varLen;
		int[] aPart = Arrays.copyOfRange(a, croIdx, end > a.length ? a.length : end);
		int[] bPart = Arrays.copyOfRange(b, croIdx, end > b.length ? b.length : end);
		if(aPart.length == bPart.length && Arrays.equals(aPart, bPart)) {
			ret = new int[][] {a.clone(), b.clone()};
		}else {
			ret = new int[2][];
			if(bPart.length > aPart.length && MoreInts.startWith(bPart, aPart)) {
				ret[0] = b.clone();
			}else {
				ret[0] = crossover1(b, aPart, croIdx, x, y);
			}
			if(aPart.length > bPart.length && MoreInts.startWith(aPart, bPart)) {
				ret[1] = a.clone();
			}else {
				ret[1] = crossover1(a, bPart, croIdx, x, y);
			}
		}
		return ret;
	}
	private static int[] crossover1(int[] src, int[] part, int croIdx, int x, int y) {
		int[] tmp = new int[src.length + part.length];
		System.arraycopy(src, 0, tmp, 0, croIdx);
		System.arraycopy(part, 0, tmp, croIdx, part.length);
		System.arraycopy(src, croIdx, tmp, croIdx + part.length,  src.length - croIdx);
		return filterByCrossover(tmp, x, y);
	}
	
	private static int[] filterByCrossover(int[] src, int x, int y) {
		int[] tmp = src.clone();
		BitSet exist = new BitSet();
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
					}
					tmp[tmp.length - 1] = 0;
					size--;
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
		int last = tmp[tmp.length - 1];
		if(last == x || last == y) {
			tmp[tmp.length - 1] = 0;
			size--;
			// 最后是个空 y, 并且也只有这一个 y, 不重置 yNextIdx, 尽量减少 y
		}
		if(tmp[0] != x && tmp[0] != y) {
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
	public static int[] mutation(int src[], float varLenRatio, int x, int y) {
		int varLen = Math.round(varLenRatio * src.length);
		int aMutIdx = ThreadLocalRandom.current().nextInt(src.length);
		int bMutIdx = ThreadLocalRandom.current().nextInt(src.length);
		if(aMutIdx == bMutIdx) {
			return src.clone();
		}else {
			return mutation1(src, aMutIdx, bMutIdx, varLen, x, y);
		}
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
					prev = cur;
					if(cur == y) {
						yNextIdx = size + 1;
					}
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
		int last = tmp[tmp.length - 1];
		if(last == x || last == y) {
			tmp[tmp.length - 1] = 0;
			size--;
			// 最后是个空 y, 并且也只有这一个 y, 不重置 yNextIdx, 尽量减少 y
		}
		if(tmp[0] != x && tmp[0] != y) {
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
	
}
