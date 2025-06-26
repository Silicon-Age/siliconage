package com.siliconage.web.form;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeField<T extends DateTimeField<?>> extends HTMLInputField<T, LocalDateTime> {
	public static final DateTimeFormatter WIRE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	
	public DateTimeField(String argName, LocalDateTime argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
	}
	
	public DateTimeField(String argName, LocalDateTime argValue) {
		super(argName, argValue);
	}
	
	public DateTimeField(String argName) {
		this(argName, null);
	}
	
	@Override
	protected String getInputType() {
		return "datetime-local";
	}
	
	@Override
	public String getSavedValueAsString() {
		if (isRealField()) {
			LocalDateTime lclSaved = getSavedValue();
			return lclSaved == null ? "" : lclSaved.format(WIRE_FORMAT);
		} else {
			throw new IllegalStateException("Cannot invoke getCurrentFieldValue for a FormField that does not represent a real field.");
		}
	}
	
	@Override
	public String valueAttribute() {
		return outputAttribute("value", determineStringToDisplay());
	}
	
	public T min(LocalDateTime argMin) {
		if (argMin == null) {
			removeAttribute("min");
		} else {
			attribute("min", argMin.format(WIRE_FORMAT));
		}
		
		return castThis();
	}
	
	public T max(LocalDateTime argMax) {
		if (argMax == null) {
			removeAttribute("max");
		} else {
			attribute("max", argMax.format(WIRE_FORMAT));
		}
		
		return castThis();
	}
}
