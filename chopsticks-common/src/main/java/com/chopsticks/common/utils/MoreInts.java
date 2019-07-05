package com.chopsticks.common.utils;

public class MoreInts {
	public static boolean startWith(int[] a, int[] b) {
		if(b.length <= a.length) {
			for(int i = 0; i < b.length; i++) {
				if(b[i] != a[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}
