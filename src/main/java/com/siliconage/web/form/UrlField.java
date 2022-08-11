package com.siliconage.web.form;

public class UrlField<T extends UrlField<?>> extends TextField<T> {
	private static final String HTML_INPUT_TYPE = "url";
	
	public UrlField(String argName, String argSavedValue, FormValueProvider argEnteredValueProvider, int argSize) {
		super(argName, argSavedValue, argEnteredValueProvider, argSize);
	}
	
	public UrlField(String argName, String argValue, int argSize) {
		super(argName, argValue, argSize);
	}
	
	@Override
	public String getInputType() {
		return HTML_INPUT_TYPE;
	}
}
