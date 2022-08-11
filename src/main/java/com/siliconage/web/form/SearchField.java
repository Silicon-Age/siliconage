package com.siliconage.web.form;

public class SearchField<T extends SearchField<?>> extends TextField<T> {
	private static final String HTML_INPUT_TYPE = "search";
	
	public SearchField(String argName, String argSavedValue, FormValueProvider argEnteredValueProvider, int argSize) {
		super(argName, argSavedValue, argEnteredValueProvider, argSize);
	}
	
	public SearchField(String argName, String argValue, int argSize) {
		super(argName, argValue, argSize);
	}
	
	@Override
	public String getInputType() {
		return HTML_INPUT_TYPE;
	}
}
