package com.siliconage.web.form;

import java.util.function.Function;

import org.apache.commons.lang3.Validate;

public class BooleanDropdownField<T extends BooleanDropdownField<T>> extends AssembledDropdownField<T, Boolean> {
	public BooleanDropdownField(String argName, Boolean argCurrentValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argCurrentValue, argEnteredValueProvider);
		
		choices(Boolean.TRUE, Boolean.FALSE);
		doNotSort();
		namer(DefaultBooleanDropdownNameCodeExtractor.getInstance());
	}
	
	public BooleanDropdownField(String argName, Boolean argCurrentValue) {
		this(argName, argCurrentValue, null);
	}
	
	public BooleanDropdownField(String argName, boolean argCurrentValue) {
		this(argName, Boolean.valueOf(argCurrentValue), null);
	}
	
	public T namer(Function<Boolean, String> argNamer) {
		Validate.notNull(argNamer);
		
		return namer(new FunctionalNameCodeExtractor<>(argNamer, DefaultBooleanDropdownNameCodeExtractor.getInstance()::extractCode));
	}
	
	public T namer(String argTrueLabel, String argFalseLabel) {
		Validate.notBlank(argTrueLabel);
		Validate.notBlank(argFalseLabel);
		
		return namer(x -> x.booleanValue() ? argTrueLabel : argFalseLabel);
	}
}
