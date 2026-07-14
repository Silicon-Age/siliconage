package com.opal;

import java.util.Objects;

/**
 * @author topquark
 */
public class ValidationException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private final FieldValidator myFieldValidator;
	private final Object myValue;
	
	public ValidationException(String argS, FieldValidator argFV, Object argValue) {
		super(argS);
		Objects.requireNonNull(argFV);
		myFieldValidator = argFV;
		
		Objects.requireNonNull(argValue);
		myValue = argValue;
	}
	
	public FieldValidator getFieldValidator() {
		return myFieldValidator;
	}
	
	public Object getValue() {
		return myValue;
	}
}
