package com.siliconage.web.form;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;


public class TextAreaField<T extends TextAreaField<?>> extends SingleValueFormField<T, String> {
	private boolean myTrimInput = true;
	
	public TextAreaField(String argName, String argSavedValue, FormValueProvider argEnteredValueProvider, int argCols, int argRows) {
		super(argName, argSavedValue, argEnteredValueProvider);
		
		Validate.isTrue(argCols > 0, "Number of columns must be positive");
		attribute("cols", String.valueOf(argCols));
		
		Validate.isTrue(argRows > 0, "Number of rows must be positive");
		attribute("rows", String.valueOf(argRows));
	}
	
	public TextAreaField(String argName, String argValue, int argCols, int argRows) {
		this(argName, argValue, null, argCols, argRows);
	}
	
	public T width(int argCols) {
		Validate.isTrue(argCols > 0, "Number of columns must be positive");
		
		return attribute("cols", String.valueOf(argCols));
	}
	
	public T height(int argRows) {
		Validate.isTrue(argRows > 0, "Number of rows must be positive");
		
		return attribute("rows", String.valueOf(argRows));
	}
	
	public T autocomplete(String argV) {
		Validate.isTrue(argV.equalsIgnoreCase("none") || argV.equalsIgnoreCase("sentences") || argV.equalsIgnoreCase("words") || argV.equalsIgnoreCase("characters"));
		return attribute("autocomplete", argV);
	}
	
	public T maxlength(long argMaxLength) {
		Validate.isTrue(argMaxLength > 0L, "Maximum length must be positive");
		
		return attribute("maxlength", String.valueOf(argMaxLength));
		
		// TODO: If a minlength has been specified, check that it is <= argMaxLength
	}
	
	public T minlength(int argMinLength) {
		Validate.isTrue(argMinLength > 0, "Minimum length must be positive");
		
		return attribute("minlength", String.valueOf(argMinLength));
		
		// TODO: If a maxlength has been specified, check that it is >= argMinLength
	}
	
	public T placeholder(String argPlaceholder) {
		Validate.notNull(argPlaceholder);
		
		return attribute("placeholder", argPlaceholder);
	}
	
	public T wrap(String argV) {
		Validate.isTrue(argV.equalsIgnoreCase("hard") || argV.equalsIgnoreCase("soft"));
		
		return attribute("wrap", argV);
	}
	
	// TODO: figure out how to remove copied trimming-related code from TextField
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
	protected void addFieldTypeSpecificHiddenParameters() {
		if (isTrimInput() == false) { // That is, NOT the default.
			hiddenParameter(getName() + "_Special", TextField.DO_NOT_TRIM_VALUE);
		}
	}
	
	@Override
	public void appendFormField(StringBuilder argSB) {
		argSB.append("<textarea")
			.append(idAttribute())
			.append(nameAttribute())
			.append(attributesToString())
			.append(disabledAttribute())
			.append(outputCssClassAttribute());
		
		String lclScript = getScript();
		boolean lclHasScript = StringUtils.isNotBlank(lclScript);
		
		if (isEnabled() && (lclHasScript == false)) {
			argSB.append(onChangeScript());
		}
		
		if (lclHasScript) {
			argSB.append(' ');
			argSB.append(lclScript);
		}
		
		argSB.append('>')
			.append(scrub(determineStringToDisplay()))
			.append("</textarea>");
	}
}
