package com.siliconage.web.form;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeField<T extends TimeField<?>> extends HTMLInputField<T, LocalTime> {
	public static final DateTimeFormatter WIRE_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;
	
	public TimeField(String argName, LocalTime argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
	}
	
	public TimeField(String argName, LocalTime argValue) {
		super(argName, argValue);
	}
	
	public TimeField(String argName) {
		this(argName, null);
	}
	
	@Override
	protected String getInputType() {
		return "time"; // CHECK: eventually change to time
	}
	
	@Override
	public String getSavedValueAsString() {
		if (isRealField()) {
			LocalTime lclSaved = getSavedValue();
			return lclSaved == null ? "" : lclSaved.format(WIRE_FORMAT);
		} else {
			throw new IllegalStateException("Cannot invoke getCurrentFieldValue for a FormField that does not represent a real field.");
		}
	}
	
	@Override
	public String valueAttribute() {
		return outputAttribute("value", determineStringToDisplay());
	}
	
	public T min(LocalTime argMin) {
		if (argMin == null) {
			removeAttribute("min");
		} else {
			attribute("min", argMin.format(WIRE_FORMAT));
		}
		
		return castThis();
	}
	
	public T max(LocalTime argMax) {
		if (argMax == null) {
			removeAttribute("max");
		} else {
			attribute("max", argMax.format(WIRE_FORMAT));
		}
		
		return castThis();
	}
}
