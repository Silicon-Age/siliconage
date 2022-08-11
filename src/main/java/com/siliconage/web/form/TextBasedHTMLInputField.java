package com.siliconage.web.form;

import org.apache.commons.lang3.Validate;

public abstract class TextBasedHTMLInputField<T extends HTMLInputField<?, V>, V> extends HTMLInputField<T, V> {
	
	private int myCustomSize = -1;
	
	protected TextBasedHTMLInputField(String argName, V argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
	}
	
	protected TextBasedHTMLInputField(String argName, V argValue) {
		this(argName, argValue, null);
	}
	
	public T maxlength(long argMaxLength) {
		Validate.isTrue(argMaxLength > 0L, "Maximum length must be positive");
		
		return attribute("maxlength", String.valueOf(argMaxLength));
		
		// TODO: If a minlength has been specified, check that it is <= argMaxLength
	}
	
	public T minlength(long argMinLength) {
		Validate.isTrue(argMinLength > 0L, "Minimum length must be positive");
		
		return attribute("minlength", String.valueOf(argMinLength));
		
		// TODO: If a maxlength has been specified, check that it is >= argMinLength
	}
	
	public T pattern(String argPattern) {
		Validate.notNull(argPattern);
		
		return attribute("pattern", argPattern);
	}
	
	public T placeholder(String argPlaceholder) {
		Validate.notNull(argPlaceholder);
		
		return attribute("placeholder", argPlaceholder);
	}
	
	public T size(int argSize) {
		Validate.isTrue(argSize > 0, "Size must be positive");
		
		myCustomSize = argSize;
		
		return castThis();
	}
	
	public abstract int getDefaultSize();
	
	protected int getCustomSize() {
		return myCustomSize;
	}
	
	public String sizeAttribute() {
		int lclSize = getCustomSize() > 0 ? getCustomSize() : getDefaultSize();
		
		return " size=\"" + lclSize + "\""; // Note leading space
	}
	
	@Override
	protected String fieldTypeSpecificAttributes() {
		return sizeAttribute();
	}
}
