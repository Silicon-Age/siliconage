package com.siliconage.web.form;

public class PasswordField<T extends PasswordField<?>> extends TextField<T> {
	private static final String HTML_INPUT_TYPE = "password";
	
	public PasswordField(String argName, String argSavedValue, FormValueProvider argEnteredValueProvider, int argSize) { // An unusual constructor to use; see below.
		super(argName, argSavedValue, argEnteredValueProvider, argSize);
	}
	
	public PasswordField(String argName, String argValue, int argSize) { // An unusual constructor to use; see below.
		super(argName, argValue, argSize);
	}
	
	public PasswordField(String argName, int argSize) { // The common constructor. Who would want to pre-fill a password field? But HTML allows it, so we will too.
		this(argName, "", argSize);
	}
	
	@Override
	public String getInputType() {
		return HTML_INPUT_TYPE;
	}
}
