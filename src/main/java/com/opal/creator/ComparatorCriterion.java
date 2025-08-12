package com.opal.creator;

public abstract class ComparatorCriterion {
	private final boolean myInverted;
	
	protected ComparatorCriterion() {
		this(false);
	}
	
	protected ComparatorCriterion(boolean argInverted) {
		super();
		
		myInverted = argInverted;
	}
	
	public boolean isInverted() {
		return myInverted;
	}
	
	public abstract String getNameElement();
	
	public abstract String generateComparisonCode(MappedClass argMC);
}
