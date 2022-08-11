package com.siliconage.web.form;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class DateField<T extends DateField<?>> extends TextBasedHTMLInputField<T, LocalDate> {
	
	private static final int DEFAULT_SIZE = 10;
	public static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final String DEFAULT_PLACEHOLDER_STRING = "yyyy-mm-dd"; // This is notably not the same as the corresponding string for DEFAULT_FORMATTER, which would have the 'm''s capitalized, because that would look weird.
	private static final String DEFAULT_REGULAR_EXPRESSION = "(?:19|20)[0-9]{2}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1[0-9]|2[0-9])|(?:(?!02)(?:0[1-9]|1[0-2])-(?:30))|(?:(?:0[13578]|1[02])-31))"; // This is hacky.  Eventually we should switch to <input type="date"> but that has various problems right now.
	
	private DateTimeFormatter myFormatter;
	private String myRegularExpression;
	
	public DateField(String argName, LocalDate argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
		
		defaultFormat();
	}
	
	public DateField(String argName, LocalDate argValue) {
		super(argName, argValue);
		
		defaultFormat();
	}
	
	public DateField(String argName) {
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
		return "text"; // CHECK: eventually change to date
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
			LocalDate lclSaved = getSavedValue();
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
