package com.opal;

/**
 * @author topquark
 */
public abstract class FieldValidator {
	protected FieldValidator() {
		super();
	}
	
	public void check(Object argValue) throws ValidationException {
		String lclS = generateMessage(argValue);
		if (lclS != null) {
			throw new ValidationException(lclS, this, argValue);
		}
	}
	
	public void assertValid(Object argValue) {
		String lclS = generateMessage(argValue);
		if (lclS != null) {
			throw new IllegalStateException("Could not assert that " + argValue + " was compatible with " + this);
		}
	}
	
	public abstract String generateMessage(Object argValue);
}
