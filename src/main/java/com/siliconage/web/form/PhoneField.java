package com.siliconage.web.form;

public class PhoneField<T extends PhoneField<?>> extends TextField<T> {
	private static final String HTML_INPUT_TYPE = "tel";
	private static final int DEFAULT_SIZE = 14;
	private static final int DEFAULT_SIZE_WITH_EXTENSION = 23;
	private static final int MAX_LENGTH = 25;
	private static final String PLACEHOLDER = "###-###-####";
	private static final String PLACEHOLDER_WITH_EXTENSION = PLACEHOLDER + " (x####)";
	
	public PhoneField(String argName, String argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider, DEFAULT_SIZE);
		
		maxlength(MAX_LENGTH);
		placeholder(PLACEHOLDER);
	}
	
	public PhoneField(String argName, String argValue) {
		this(argName, argValue, new SingleValueProvider(argValue));
	}
	
	@Override
	public String getInputType() {
		return HTML_INPUT_TYPE;
	}
	
	public T withPossibleExtension() {
		placeholder(PLACEHOLDER_WITH_EXTENSION);
		size(DEFAULT_SIZE_WITH_EXTENSION);
		return castThis();
	}
}
