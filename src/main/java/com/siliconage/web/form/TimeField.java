package com.siliconage.web.form;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class TimeField<T extends TimeField<?>> extends TextBasedHTMLInputField<T, LocalTime> {
	private static final int DEFAULT_SIZE = 8;
	public static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
	private static final String DEFAULT_PLACEHOLDER_STRING = "hh:mm(:ss)";
	private static final String DEFAULT_REGULAR_EXPRESSION = "(2[0-3]|[01][0-9]):([0-5][0-9])(:([0-5]?[0-9]))?"; // This is hacky.  Eventually we should switch to <input type="time"> but that has various problems right now.
	
	private DateTimeFormatter myFormatter;
	private String myRegularExpression;
	
	public TimeField(String argName, LocalTime argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
		
		defaultFormat();
	}
	
	public TimeField(String argName, LocalTime argValue) {
		super(argName, argValue);
		
		defaultFormat();
	}
	
	public TimeField(String argName) {
		this(argName, null);
	}
	
	public DateTimeFormatter getDefaultFormatter() {
		return DEFAULT_FORMATTER;
	}
	
	public T format(DateTimeFormatter argDTF, String argPlaceholder, String argRegularExpression) {
		myFormatter = argDTF;
		placeholder(argPlaceholder);
		myRegularExpression = StringUtils.trimToEmpty(argRegularExpression);
		attribute("title", argPlaceholder);
		
		return castThis();
	}
	
	public T format(String argPattern, String argPlaceholder, String argRegularExpression) {
		Validate.notNull(argPattern);
		
		return format(DateTimeFormatter.ofPattern(argPattern), argPlaceholder, argRegularExpression);
	}
	
	public DateTimeFormatter getFormatter() {
		return ObjectUtils.firstNonNull(myFormatter, getDefaultFormatter());
	}
	
	public String getRegularExpression() {
		return myRegularExpression;
	}
	
	public T defaultFormat() {
		return format(getDefaultFormatter(), DEFAULT_PLACEHOLDER_STRING, DEFAULT_REGULAR_EXPRESSION);
	}
	
	public boolean usingDefaultFormatter() {
		return myFormatter == null || myFormatter.equals(getDefaultFormatter());
	}
	
	@Override
	protected String getInputType() {
		return "text"; // CHECK: eventually change to time
	}

	@Override
	protected void addFieldTypeSpecificHiddenParameters() {
		if (usingDefaultFormatter() == false) {
			hiddenParameter("pattern", "unsupported"); // CHECK: How can we solve this?
		}
	}
	
	@Override
	protected String fieldTypeSpecificAttributes() {
		return super.fieldTypeSpecificAttributes() + outputAttribute("data-content-type", "date") + outputAttribute("pattern", getRegularExpression());
	}
	
	@Override
	public String getSavedValueAsString() {
		if (isRealField()) {
			LocalTime lclSaved = getSavedValue();
			return lclSaved == null ? "" : getFormatter().format(lclSaved);
		} else {
			throw new IllegalStateException("Cannot invoke getCurrentFieldValue for a FormField that does not represent a real field.");
		}
	}
	
	@Override
	public String valueAttribute() {
		return outputAttribute("value", determineStringToDisplay());
	}
	
	@Override
	public int getDefaultSize() {
		return DEFAULT_SIZE;
	}
}
