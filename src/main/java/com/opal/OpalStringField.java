package com.opal;

public class OpalStringField <U extends UserFacing> extends OpalSpecificField<U, String> {
	
	private final int myMinimumLength;
	private final int myMaximumLength;

	private final static int DEFAULT_MINIMUM_LENGTH = 0;
	private final static int DEFAULT_MAXIMUM_LENGTH = Integer.MAX_VALUE;
	
	public OpalStringField(OpalBaseField<U, String> argBaseField, int argMinimumLength, int argMaximumLength) {
		super(argBaseField);
		
		myMinimumLength = argMinimumLength; // FIXME: Validate
		myMaximumLength = argMaximumLength;	
	}

	// FIXME: One-arg ctor for just maximum length?
	
	public OpalStringField(OpalBaseField<U, String> argBaseField) {
		this(argBaseField, DEFAULT_MINIMUM_LENGTH, DEFAULT_MAXIMUM_LENGTH);
		
	}

	public int getMinimumLength() {
		return myMinimumLength;
	}
	
	public int getMaximumLength() {
		return myMaximumLength;
	}

}
