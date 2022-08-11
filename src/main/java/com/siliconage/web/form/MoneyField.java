package com.siliconage.web.form;

import java.text.NumberFormat;
import java.text.DecimalFormat;

public class MoneyField<T extends MoneyField<?>> extends NumberField<T> {
	public static final int DEFAULT_SIZE = 5;
	
	public MoneyField(String argName, Number argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
		
		step(0.01d); // FIXME: locale-sensitive
		// attribute("onchange", "this.value = parseFloat(this.value).toFixed(2); this.className += ' " + getChangedCssClass() + "';"); // Make it always show two decimal places
	}
	
	public MoneyField(String argName, Number argValue) {
		super(argName, argValue);
		
		step(0.01d); // FIXME: locale-sensitive
		// attribute("onchange", "this.value = parseFloat(this.value).toFixed(2); this.className += ' " + getChangedCssClass() + "';"); // Make it always show two decimal places
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
