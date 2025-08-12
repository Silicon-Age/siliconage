package com.opal;

import org.apache.commons.lang3.Validate;

/**
 * @author topquark
 */
public class ValidationException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private final FieldValidator myFieldValidator;
	private final Object myValue;
	
	public ValidationException(String argS, FieldValidator argFV, Object argValue) {
		super(argS);
		Validate.notNull(argFV);
		myFieldValidator = argFV;
		
		Validate.notNull(argValue);
		myValue = argValue;
	}
	
	public FieldValidator getFieldValidator() {
		return myFieldValidator;
	}
	
	public Object getValue() {
		return myValue;
	}
}
