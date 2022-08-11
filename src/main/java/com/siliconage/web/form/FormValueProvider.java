package com.siliconage.web.form;

import java.util.Collection;

public interface FormValueProvider {
	// static final org.apache.log4j.Logger ourLogger = org.apache.log4j.Logger.getLogger(FormValueProvider.class.getName());
	
	public long getLoadTime();
	
	public Collection<String> getAll(String argKey);
	
	default public String get(String argKey) {
		Collection<String> lclAll = getAll(argKey);
		
		if (lclAll == null || lclAll.isEmpty()) {
			return null;
		} else if (lclAll.size() == 1) {
			return lclAll.iterator().next();
		} else {
			throw new IllegalStateException("Multiple values for '" + argKey + "' : " + lclAll.toString());
		}
	}
	
	public boolean hasValueFor(String argKey);
	
	public void setDisabled(String argKey, boolean argValue);
	public boolean isDisabled(String argKey);
	
	default public void setEnabled(String argKey, boolean argValue) {
		setDisabled(argKey, !argValue);
	}
	
	default public void enable(String argKey) {
		setDisabled(argKey, false);
	}
	
	default public void disable(String argKey) {
		setDisabled(argKey, true);
	}
	
	default public boolean isEnabled(String argKey) {
		return !isDisabled(argKey);
	}
	
	public boolean isIncorrect(String argKey);
	
	default boolean isChosen(String argKey, String argValue) {
		Collection<String> lclAll = getAll(argKey);
		
		return lclAll != null && lclAll.stream().anyMatch(argV -> argV.equals(argValue));
	}
	
	public FormFieldRequirement determineRequirement(String argKey);
}
