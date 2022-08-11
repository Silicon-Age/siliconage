package com.siliconage.web.form;

import org.apache.commons.lang3.Validate;

public abstract class HTMLInputField<T extends HTMLInputField<?, V>, V> extends SingleValueFormField<T, V> {
	protected HTMLInputField(String argName, V argSavedValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValue, argEnteredValueProvider);
	}
	
	protected HTMLInputField(String argName, V argValue) {
		this(argName, argValue, null);
	}
	
	protected abstract String getInputType();
	
	protected String fieldTypeSpecificAttributes() {
		return "";
	}
	
	protected abstract String valueAttribute();
	
	@Override
	protected void appendFormField(StringBuilder argSB) {
		Validate.notNull(argSB);
		
		argSB.append("<input")
			.append(outputAttribute("type", getInputType()))
			.append(idAttribute())
			.append(nameAttribute())
			.append(valueAttribute())
			.append(fieldTypeSpecificAttributes())
			.append(attributesToString())
			.append(disabledAttribute())
			.append(outputCssClassAttribute());
		
		if (isEnabled() && hasAttribute("onchange") == false) {
			argSB.append(onChangeScript());
		}
		
		argSB.append(" />");
	}
}
