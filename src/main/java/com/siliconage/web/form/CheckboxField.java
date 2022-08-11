package com.siliconage.web.form;

import com.siliconage.web.HTMLUtility;

public class CheckboxField<T extends CheckboxField<?>> extends CheckableField<T> {
	private static final String HTML_INPUT_TYPE = "checkbox";
	
	public CheckboxField(String argName, String argValue, boolean argChecked, FormValueProvider argEnteredValueProvider) {
		super(
			argName,
			argValue == null ? HTMLUtility.DEFAULT_TRUE_STRING : argValue,
			argChecked,
			argEnteredValueProvider
		);
	}
	
	public CheckboxField(String argName, String argValue, FormValueProvider argEnteredValueProvider) {
		this(argName, argValue, false, argEnteredValueProvider);
	}
	
	public CheckboxField(String argName, boolean argChecked, FormValueProvider argEnteredValueProvider) {
		this(argName, null, argChecked, argEnteredValueProvider);
	}
	
	public CheckboxField(String argName, FormValueProvider argEnteredValueProvider) {
		this(argName, null, false, argEnteredValueProvider);
	}
	
	@Override
	protected boolean mayThereBeOtherFieldsWithTheSameNameAsThisOne() {
		return true;
	}
	
	@Override
	protected String getInputType() {
		return HTML_INPUT_TYPE;
	}
	
	@Override
	protected boolean isRequirable() {
		return false;
	}
}
