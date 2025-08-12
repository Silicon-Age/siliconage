package com.opal;

/**
 * @author topquark
 */
public class IllegalNullArgumentException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	
	public IllegalNullArgumentException() {
		super();
	}
	
	public IllegalNullArgumentException(String argMessage) {
		super(argMessage);
	}
}
