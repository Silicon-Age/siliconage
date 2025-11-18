package com.opal;

public class OpalIntegerField <U extends UserFacing> extends OpalSpecificField<U, Integer> {
	
	private final int myMinimumValue;
	private final int myMaximumValue;

	private final static int DEFAULT_MINIMUM_VALUE = Integer.MIN_VALUE;
	private final static int DEFAULT_MAXIMUM_VALUE = Integer.MAX_VALUE;
	
	public OpalIntegerField(OpalBaseField<U, Integer> argBaseField, int argMinimumValue, int argMaximumValue) {
		super(argBaseField);
		
		myMinimumValue = argMinimumValue; // FIXME: Validate
		myMaximumValue = argMaximumValue;	
	}

	// FIXME: One-arg ctor for just maximum length?
	
	public OpalIntegerField(OpalBaseField<U, Integer> argBaseField) {
		this(argBaseField, DEFAULT_MINIMUM_VALUE, DEFAULT_MAXIMUM_VALUE);		
	}

	public int getMinimumValue() {
		return myMinimumValue;
	}
	
	public int getMaximumValue() {
		return myMaximumValue;
	}

	public DatabaseQuery query(int argSearchValue) {
		return query(Integer.valueOf(argSearchValue));
	}
	
}
