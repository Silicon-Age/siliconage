package com.opal;

import org.apache.commons.lang3.Validate;

/**
 * @author topquark and jonah
 */
public class ArgumentTooLongException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	
	private final Integer myAttemptedLength, myPermittedLength;
	
	public ArgumentTooLongException() {
		super();
		
		myAttemptedLength = null;
		myPermittedLength = null;
	}
	
	public ArgumentTooLongException(String argMessage) {
		super(argMessage);
		
		myAttemptedLength = null;
		myPermittedLength = null;
	}
	
	@SuppressWarnings("boxing")
	public ArgumentTooLongException(int argAttemptedLength, int argPermittedLength) {
		super();
		
		Validate.isTrue(argAttemptedLength > 0);
		Validate.isTrue(argPermittedLength >= 0);
		Validate.isTrue(argAttemptedLength > argPermittedLength);
		
		myAttemptedLength = argAttemptedLength;
		myPermittedLength = argPermittedLength;
	}
	
	@SuppressWarnings("boxing")
	public ArgumentTooLongException(String argMessage, int argAttemptedLength, int argPermittedLength) {
		super(argMessage);
		
		Validate.isTrue(argAttemptedLength > 0);
		Validate.isTrue(argPermittedLength >= 0);
		Validate.isTrue(argAttemptedLength > argPermittedLength);
		
		myAttemptedLength = argAttemptedLength;
		myPermittedLength = argPermittedLength;
	}
	
	public Integer getAttemptedLength() {
		return myAttemptedLength;
	}
	
	public Integer getPermittedLength() {
		return myPermittedLength;
	}
	
	public Integer getOverage() {
		if (getAttemptedLength() == null || getPermittedLength() == null) {
			return null;
		} else {
			return Integer.valueOf(getAttemptedLength().intValue() - getPermittedLength().intValue());
		}
	}
}
