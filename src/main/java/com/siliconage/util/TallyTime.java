package com.siliconage.util;

import java.io.PrintStream;

import java.util.Map;
import java.util.HashMap;

/**
 * @author topquark
 */
public class TallyTime<K> {
	private final HashMap<K, TimeCount> myHashMap = new HashMap<>(); 
	
	public TallyTime() {
		super();
	}
	
	private static class TimeCount {
		long myTime;
		int myCount;
		public TimeCount(long argTime) {
			myTime = argTime;
			myCount = 1;
		}
		public void addTime(long argTime) {
			myTime += argTime;
			myCount++;
		}
	}
	
	public void tally(K argKey, long argTime) {
		if (argKey == null) {
			return;
		}
		TimeCount lclTC = myHashMap.get(argKey);
		if (lclTC == null) {
			myHashMap.put(argKey, new TimeCount(argTime));
		} else {
			lclTC.addTime(argTime);
		}
	}
	
	public void report(PrintStream argPS) {
		if (argPS == null) {
			return;
		}

		for(Map.Entry<K, TimeCount> lclEntry : myHashMap.entrySet()) {
			final K lclKey = lclEntry.getKey();
			final TimeCount lclCount = lclEntry.getValue();
			argPS.println((lclCount.myTime / lclCount.myCount) + " ms  " + lclCount.myCount + "  -> " + lclKey);
		}
	}
	
	public void clear() {
		myHashMap.clear();
	}
}
