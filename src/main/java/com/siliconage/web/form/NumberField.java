package com.siliconage.web.form;

import java.text.NumberFormat;
import java.text.DecimalFormat;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;

public class NumberField<T extends NumberField<?>> extends TextBasedHTMLInputField<T, Number> {
	private static final int DEFAULT_SIZE = 5;
	private static final String DEFAULT_FORMAT_STRING = "#.#########";
	
	private NumberFormat myFormatter;
	
	public NumberField(String argName, Number argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
	}
	
	public NumberField(String argName, Number argValue) {
		super(argName, argValue);
	}
	
	public NumberField(String argName, int argValue) {
		super(argName, Integer.valueOf(argValue));
	}
	
	public NumberField(String argName) {
		this(argName, null);
	}
	
	protected NumberFormat getDefaultFormatter() {
		return new DecimalFormat(DEFAULT_FORMAT_STRING);
	}
	
	public T formatter(NumberFormat argF) {
		myFormatter = argF;
		return castThis();
	}
	
	public NumberFormat getFormatter() {
		return ObjectUtils.firstNonNull(myFormatter, getDefaultFormatter());
	}
	
	public T defaultFormatter() {
		return formatter(null);
	}
	
	public boolean usingDefaultFormatter() {
		return myFormatter == null;
	}
	
	public T max(Number argMax) {
		if (argMax == null) {
			removeAttribute("max");
		} else {
			attribute("max", argMax.toString());
		}
		
		return castThis();
	}
	
	public T max(int argMax) {
		return max(Integer.valueOf(argMax));
	}
	
	public T max(double argMax) {
		return max(Double.valueOf(argMax));
	}
	
	public T min(Number argMin) {
		if (argMin == null) {
			removeAttribute("min");
		} else {
			attribute("min", argMin.toString());
		}
		
		return castThis();
	}

	public T min(int argMin) {
		return min(Integer.valueOf(argMin));
	}
	
	public T min(double argMin) {
		return min(Double.valueOf(argMin));
	}
	
	public T step(Number argStep) {
		if (argStep != null) {
			attribute("step", argStep instanceof Double ? String.format("%.9f", argStep) : String.valueOf(argStep)); // String.format prevents exponential-notation-ification
		}
		
		return castThis();
	}

	public T step(double argStep) {
		return step(Double.valueOf(argStep));
	}
	
	public T stepAny() {
		attribute("step", "any");
		return castThis();
	}
	
	public T range(Number argMin, Number argMax) {
		// Each may be null
		
		if (argMin != null && argMax != null) {
			Validate.isTrue(Double.compare(argMin.doubleValue(), argMax.doubleValue()) <= 0, "Invalid range: " + argMin + " to " + argMax); // i.e., argMin <= argMax
		}
		
		min(argMin);
		return max(argMax);
	}
	
	public T range(int argMin, int argMax) {
		return range(Integer.valueOf(argMin), Integer.valueOf(argMax));
	}
	
	public T range(double argMin, double argMax) {
		return range(Double.valueOf(argMin), Double.valueOf(argMax));
	}
	
	public T range(double argMin, double argMax, double argStep) {
		return range(Double.valueOf(argMin), Double.valueOf(argMax), Double.valueOf(argStep));
	}
	
	public T range(Number argMin, Number argMax, Number argStep) {
		// Each may be null
		
		if (argMin != null && argMax != null) {
			Validate.isTrue(Double.compare(argMin.doubleValue(), argMax.doubleValue()) <= 0); // i.e., argMin <= argMax
			
			if (argStep != null) {
				Validate.isTrue(Double.compare(argStep.doubleValue(), argMax.doubleValue() - argMin.doubleValue()) <= 0); // i.e., argStep <= (argMax - argMin)
			}
		}
		
		min(argMin);
		max(argMax);
		return step(argStep);
	}
	
	@Override
	protected String getInputType() {
		return "number";
	}

	@Override
	protected void addFieldTypeSpecificHiddenParameters() {
		if (usingDefaultFormatter() == false) {
			hiddenParameter("pattern", "unsupported"); // CHECK: How can we solve this?
		}
	}
	
	@Override
	public String getSavedValueAsString() {
		if (isRealField()) {
			Number lclSaved = getSavedValue();
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
	
	@Override
	public String sizeAttribute() {
		int lclSize = getCustomSize() > 0 ? getCustomSize() : getDefaultSize();
		
		int lclEms = lclSize + 1;
		
		if (hasAttribute("style")) {
			style("width: " + lclEms + "em; " + getAttributes().get("style")); // The extant attribute goes last so it overrides our width if it contains a width
			return "";
		} else {
			return " style=\"width: " + lclEms + "em\""; // Note leading space
		}
	}
}
