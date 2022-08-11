package com.siliconage.web.form;

import java.util.Collections;
import java.util.Collection;

import org.apache.commons.lang3.Validate;

public class SingleValueProvider implements FormValueProvider {
	private final long myConstructionTime;
	private final String myValue;
	private boolean myDisabled;
	private FormFieldRequirement myRequirement;
	
	public SingleValueProvider(String argValue) {
		this(argValue, FormFieldRequirement.NOT_REQUIRED);
	}
	
	public SingleValueProvider(String argValue, FormFieldRequirement argRequirement) {
		super();
		
		myConstructionTime = System.currentTimeMillis();
		myValue = argValue; // which may be null
		myDisabled = false;
		myRequirement = Validate.notNull(argRequirement);
	}
	
	@Override
	public long getLoadTime() {
		return myConstructionTime;
	}
	
	@Override
	public Collection<String> getAll(String argKey) {
		return Collections.singleton(myValue);
	}
	
	@Override
	public String get(String argKey) {
		return myValue;
	}
	
	public String get() {
		return get(null);
	}
	
	@Override
	public boolean hasValueFor(String argKey) {
		return true; // This doesn't actually make much sense, but inasmuch as getCurrentValue(argKey) will always make sense, it seems like the least nonsensical thing to do.
	}
	
	@Override
	public boolean isIncorrect(String argIncorrect) {
		return false;
	}
	
//	@Override
//	public boolean isChecked(String argKey, String argValue) {
//		return false;
//	}
	
	@Override
	public boolean isDisabled(String argKey) {
		return myDisabled;
	}
	
	@Override
	public void setDisabled(String argKey, boolean argValue) {
		myDisabled = argValue;
	}
	
	@Override
	public FormFieldRequirement determineRequirement(String argKey) {
		return myRequirement;
	}
}
