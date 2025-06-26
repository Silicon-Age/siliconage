package com.siliconage.web.form;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateField<T extends DateField<?>> extends HTMLInputField<T, LocalDate> {
	public static final DateTimeFormatter WIRE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
	
	public DateField(String argName, LocalDate argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
	}
	
	public DateField(String argName, LocalDate argValue) {
		super(argName, argValue);
	}
	
	public DateField(String argName) {
		this(argName, null);
	}
	
	@Override
	protected String getInputType() {
		return "date";
	}
	
	@Override
	public String getSavedValueAsString() {
		if (isRealField()) {
			LocalDate lclSaved = getSavedValue();
			return lclSaved == null ? "" : lclSaved.format(WIRE_FORMAT);
		} else {
			throw new IllegalStateException("Cannot invoke getCurrentFieldValue for a FormField that does not represent a real field.");
		}
	}
	
	@Override
	public String valueAttribute() {
		return outputAttribute("value", determineStringToDisplay());
	}
	
	public T min(LocalDate argMin) {
		if (argMin == null) {
			removeAttribute("min");
		} else {
			attribute("min", argMin.format(WIRE_FORMAT));
		}
		
		return castThis();
	}
	
	public T max(LocalDate argMax) {
		if (argMax == null) {
			removeAttribute("max");
		} else {
			attribute("max", argMax.format(WIRE_FORMAT));
		}
		
		return castThis();
	}
}
