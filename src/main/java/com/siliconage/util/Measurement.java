package com.siliconage.util;

/**
 * @author topquark
 */
public class Measurement {
	private int myCount = 0;
	private double myFirstMoment = 0.0D;
	private double mySecondMoment = 0.0D;
	private double myMax = 0.0D;
	private double myMin = 0.0D;
	
	/* package */ Measurement() {
		super();
	}
	
	public int getCount() {
		return myCount;
	}
	
	public double getTotal() {
		return myFirstMoment;
	}
	
	public double getMean() {
		if (getCount() == 0) {
			throw new IllegalStateException("Cannot call getMean() when getCount() == 0");
		}
		return getFirstMoment() / getCount();
	}
	
	public double getMax() {
		if (getCount() == 0) {
			throw new IllegalStateException("Cannot call getMax() when getCount() == 0");
		}
		return myMax;
	}
	
	public double getMin() {
		if (getCount() == 0) {
			throw new IllegalStateException("Cannot call getMin() when getCount() == 0");
		}
		return myMin;
	}
	
	public double getVariance() {
		if (getCount() == 0) {
			throw new IllegalStateException("Cannot call getVariance() when getCount() == 0");
		}
		return (getSecondMoment() - getFirstMoment() * getFirstMoment())/getCount();
	}
	
	public double getStandardDeviation() {
		return Math.sqrt(getVariance()); // getVariance() will throw an exception if getCount() == 0
	}
	
	public double getFirstMoment() {
		return myFirstMoment;
	}
	
	public double getSecondMoment() {
		return mySecondMoment;
	}
	
	public void tally(double argMeasurement) {
		myCount++;
		if (myCount == 1) {
			myMax = argMeasurement;
			myMin = argMeasurement;
		} else {
			if (argMeasurement > myMax) {
				myMax = argMeasurement;
			} else if (argMeasurement < myMin) {
				myMin = argMeasurement;
			}
		}
		myFirstMoment += argMeasurement;
		mySecondMoment += argMeasurement * argMeasurement;
	}
}
