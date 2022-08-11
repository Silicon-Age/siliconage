package com.siliconage.web.form;

public class EmailField<T extends EmailField<?>> extends TextField<T> {
	private static final String HTML_INPUT_TYPE = "email";
	
	public static final int DEFAULT_SIZE = 25;
	
	public EmailField(String argName, String argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider, DEFAULT_SIZE);
	}
	
	public EmailField(String argName, String argValue) {
		super(argName, argValue, DEFAULT_SIZE);
	}
	
	@Override
	public String getInputType() {
		return HTML_INPUT_TYPE;
	}
}
