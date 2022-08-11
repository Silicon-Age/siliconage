package com.siliconage.util;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * @author topquark
 */
public class MeasurementTally<K> {
	private final HashMap<K, Measurement> myTally = new HashMap<>();
	
	public MeasurementTally() {
		super();
	}
	
	public void clear() {
		myTally.clear();
	}
	
	public Iterator<K> keyIterator() {
		return myTally.keySet().iterator();
	}
	
	public void tally(K argKey, double argMeasurement) {
		Measurement lclM = myTally.get(argKey);
		if (lclM == null) {
			myTally.put(argKey, lclM = new Measurement());
		}
		lclM.tally(argMeasurement);
	}
	
	public Measurement getMeasurement(K argKey) {
		return myTally.get(argKey);
	}
	
	public Set<K> keySet() {
		return myTally.keySet();
	}
}
