package com.siliconage.web.form;

import com.siliconage.web.HTMLUtility;

public class RadioField<T extends RadioField<?>> extends CheckableField<T> {
	// private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(CheckableField.class.getName());
	private static final String HTML_INPUT_TYPE = "radio";
	
	public RadioField(String argName, String argValue, boolean argChecked, FormValueProvider argEnteredValueProvider) {
		super(argName, argValue != null ? argValue : HTMLUtility.DEFAULT_TRUE_STRING, argChecked, argEnteredValueProvider);
	}
	
	public RadioField(String argName, String argValue, FormValueProvider argEnteredValueProvider) {
		this(argName, argValue, false, argEnteredValueProvider);
	}
	
	public RadioField(String argName, boolean argChecked, FormValueProvider argEnteredValueProvider) {
		this(argName, null, argChecked, argEnteredValueProvider);
	}
	
	public RadioField(String argName, FormValueProvider argEnteredValueProvider) {
		this(argName, null, false, argEnteredValueProvider);
	}
	
	@Override
	protected String getInputType() {
		return HTML_INPUT_TYPE;
	}
}
