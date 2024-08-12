package com.siliconage.web.form;

import java.text.NumberFormat;
import java.text.DecimalFormat;

public class MoneyField<T extends MoneyField<?>> extends NumberField<T> {
	public static final int DEFAULT_SIZE = 5;
	
	public static final Double DEFAULT_STEP = Double.valueOf(0.01d);
	
	public MoneyField(String argName, Number argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
		
		step(DEFAULT_STEP); // FIXME: locale-sensitive
	}
	
	public MoneyField(String argName, Number argValue) {
		super(argName, argValue);
		
		step(DEFAULT_STEP); // FIXME: locale-sensitive
	}
	
	public MoneyField(String argName, double argValue) {
		this(argName, Double.valueOf(argValue));
	}
	
	public MoneyField(String argName) {
		this(argName, null);
	}
	
	@Override
	protected NumberFormat getDefaultFormatter() {
		return new DecimalFormat("0.00"); // FIXME: locale-sensitive
	}
	
	@Override
	public int getDefaultSize() {
		return DEFAULT_SIZE;
	}
}
