package com.opal.cma;

import com.siliconage.web.form.CheckboxField;
import com.siliconage.web.form.FormValueProvider;

public class OpalCheckboxField<T extends OpalCheckboxField<?>> extends CheckboxField<T> {

	public OpalCheckboxField(String argName, String argValue, boolean argChecked, FormValueProvider argEnteredValueProvider) {
		super(argName, argValue, argChecked, argEnteredValueProvider);
	}
	
	public OpalCheckboxField(String argName, boolean argChecked, FormValueProvider argEnteredValueProvider) {
		this(argName, null, argChecked, argEnteredValueProvider);
	}
	
	public OpalCheckboxField(String argName, FormValueProvider argEnteredValueProvider) {
		this(argName, null, false, argEnteredValueProvider);
	}
	
	@Override
	protected boolean mayThereBeOtherFieldsWithTheSameNameAsThisOne() {
		return true;
	}
	
	@Override
	public String getId() {
		return getName();
	}
}
