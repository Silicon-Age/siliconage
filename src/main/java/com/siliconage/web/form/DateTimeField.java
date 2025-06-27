package com.siliconage.web.form;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateTimeField<T extends DateTimeField<?>> extends HTMLInputField<T, LocalDateTime> {
	// The lack of milliseconds may cause a value to lose precision when it round-trips through a field.
	// For our purposes, I think that's okay, but it's not ideal.
	public static final DateTimeFormatter WIRE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	
	public DateTimeField(String argName, LocalDateTime argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
		
		minutePrecision();
	}
	
	public DateTimeField(String argName, LocalDateTime argValue) {
		super(argName, argValue);
		
		minutePrecision();
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
	
	public T min(LocalDate argMin) {
		return min(argMin == null ? null : argMin.atStartOfDay());
	}
	
	public T max(LocalDateTime argMax) {
		if (argMax == null) {
			removeAttribute("max");
		} else {
			attribute("max", argMax.format(WIRE_FORMAT));
		}
		
		return castThis();
	}
	
	public T max(LocalDate argMax) {
		return max(argMax == null ? null : argMax.atTime(LocalTime.MAX));
	}
	
	public T step(Integer argStepSeconds) {
		if (argStepSeconds == null) {
			removeAttribute("step");
		} else {
			attribute("step", String.valueOf(argStepSeconds));
		}
		
		return castThis();
	}
	
	public T secondPrecision() {
		return step(1);
	}
	
	public T minutePrecision() {
		return step(TimeField.SECONDS_PER_MINUTE);
	}
	
	public T hourPrecision() {
		return step(TimeField.SECONDS_PER_MINUTE * TimeField.MINUTES_PER_HOUR);
	}
}
