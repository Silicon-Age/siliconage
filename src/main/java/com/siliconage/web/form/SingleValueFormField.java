package com.siliconage.web.form;

import java.util.Collections;

public abstract class SingleValueFormField<T extends SingleValueFormField<?, V>, V> extends FormField<T, V> {
	// private static final org.apache.log4j.Logger ourLogger = org.apache.log4j.Logger.getLogger(SingleValueFormField.class.getName());
	
	protected SingleValueFormField(String argName, V argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, Collections.singleton(argSavedValue), argEnteredValueProvider);
	}
	
	protected SingleValueFormField(String argName, V argValue) {
		this(argName, argValue, new SingleValueProvider(String.valueOf(argValue)));
	}
	
	protected V getSavedValue() {
		return getSavedValues().iterator().next();
	}
	
	protected String getSavedValueAsString() {
		return getSavedValuesAsString().iterator().next();
	}
	
	// A null return value indicates that no entry has been given (which is distinct from an empty string having been entered/left in place)
	protected String getEnteredValue() {
		return getEnteredValueProvider() == null ? null : getEnteredValueProvider().get(getId());
	}
	
	protected String determineStringToDisplay() {
		// ourLogger.debug("determineStringToDisplay for " + getId());
		if (getEnteredValueProvider() == null) {
			return getSavedValueAsString();
		} else {
			if (hasEnteredValue()) {
				// ourLogger.debug(getId() + " hasEnteredValue of " + (getEnteredValue() == null ? "null" : "\"" + getEnteredValue() + "\""));
				return getEnteredValue();
			} else {
				// ourLogger.debug(getId() + " hasSavedValue of " + (getSavedValueAsString() == null ? "null" : "\"" + getSavedValueAsString() + "\""));
				return getSavedValueAsString();
			}
		}
	}
}
