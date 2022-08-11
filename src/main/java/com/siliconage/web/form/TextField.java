package com.siliconage.web.form;

import java.util.Set;

import org.apache.commons.lang3.Validate;

public class TextField<T extends TextField<?>> extends TextBasedHTMLInputField<T, String> {
	private static final String HTML_INPUT_TYPE = "text";
	private static final int DEFAULT_SIZE = 20;
	private static final Set<String> VALID_AUTOCOMPLETE_VALUES = Set.of("none", "off", "sentences", "words", "characters", "new-password"); // Must be all-lowercase
	
	public static final String DO_NOT_TRIM_VALUE = "no_trim";
	
	private boolean myTrimInput = true;
	
	public TextField(String argName, String argSavedValue, FormValueProvider argEnteredValueProvider, int argSize) {
		super(argName, argSavedValue, argEnteredValueProvider);
		
		size(argSize);
	}
	
	public TextField(String argName, String argSavedValue, int argSize) {
		this(argName, argSavedValue, new SingleValueProvider(String.valueOf(argSavedValue)), argSize);
	}
	
	public TextField(String argName, int argSize) {
		this(argName, "", argSize);
	}
	
	public T autocomplete(String argV) {
		Validate.notBlank(argV);
		
		String lclV = argV.toLowerCase();
		Validate.isTrue(VALID_AUTOCOMPLETE_VALUES.contains(lclV), "The only valid autocomplete values are " + VALID_AUTOCOMPLETE_VALUES);
		
		return attribute("autocomplete", lclV);
	}
	
	public boolean isTrimInput() {
		return myTrimInput;
	}
	
	public T trimInput() {
		setTrimInput(true);
		return castThis();
	}
	
	public T doNotTrimInput() {
		setTrimInput(false);
		return castThis();
	}
	
	public void setTrimInput(boolean argTrimInput) {
		myTrimInput = argTrimInput;
	}
	
	@Override
	public String valueAttribute() {
		return outputAttribute("value", determineStringToDisplay());
	}

	@Override
	protected String getInputType() {
		return HTML_INPUT_TYPE;
	}

	@Override
	protected void addFieldTypeSpecificHiddenParameters() {
		if (isTrimInput() == false) { // That is, NOT the default.
			hiddenParameter(getName() + "_Special", DO_NOT_TRIM_VALUE);
		}
	}
	
	@Override
	public int getDefaultSize() {
		return DEFAULT_SIZE;
	}
}
