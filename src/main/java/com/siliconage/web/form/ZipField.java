package com.siliconage.web.form;

public class ZipField<T extends ZipField<?>> extends TextField<T> {
	private static final int MAX_LENGTH = 10;
	private static final String PLACEHOLDER = "US: #####(-####)";
	private static final int DEFAULT_SIZE = PLACEHOLDER.length() + 1;
	
	public ZipField(String argName, String argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider, DEFAULT_SIZE);
		
		maxlength(MAX_LENGTH);
		placeholder(PLACEHOLDER);
	}
	
	public ZipField(String argName, String argValue) {
		this(argName, argValue, new SingleValueProvider(argValue));
	}
}
