package com.siliconage.web.form;

import java.util.Collections;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public abstract class DropdownField<T extends DropdownField<?, C>, C> extends FormField<T, C> {
	public static final int DEFAULT_SIZE_MULTIPLE = 5;
	
	private String myInstructions = "Choose&hellip;";
	private boolean myMultiple = false;
	private int mySize = -1;
	
	protected DropdownField(String argName, Collection<C> argSavedValues, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValues, argEnteredValueProvider);
	}
	
	protected DropdownField(String argName, C argSavedValue, FormValueProvider argEnteredValueProvider) {
		this(argName, Collections.singleton(argSavedValue), argEnteredValueProvider);
	}
	
	// Instructions are separate from determineEntries(). Is this a good idea?
	protected DropdownOption<?> createInstructionsOption(boolean argSelected) {
		if (isMultiple() || myInstructions == null) {
			return null;
		} else {
			return DropdownOption.instructions(myInstructions, argSelected).castThis();
		}
	}
	
	public T instructions(String argI) {
		myInstructions = argI == null ? null : StringUtils.trimToEmpty(argI);
		
		return castThis();
	}
	
	public T setMultiple(boolean argV) {
		myMultiple = argV;
		
		return castThis();
	}
	
	public T multiple() {
		return setMultiple(true);
	}
	
	public T multiple(int argSize) {
		Validate.isTrue(argSize > 0);
		
		multiple();
		return size(argSize);
	}
	
	public boolean isMultiple() {
		return myMultiple;
	}
	
	public T size(int argSize) {
		Validate.isTrue(argSize > 0);
		
		mySize = argSize;
		
		return castThis();
	}
	
	public int determineSize() {
		if (isMultiple()) {
			return mySize;
		} else {
			return 1;
		}
	}
	
	public abstract List<? extends DropdownEntry<?>> determineEntries();
	
	protected abstract boolean isAnyOptionSelected();
	
	protected String allowClearAttribute(boolean argNothingSelected) {
		if (argNothingSelected) {
			return outputAttribute("data-allow-clear", "true");
		} else {
			return "";
		}
	}
	
	@Override
	protected void appendFormField(StringBuilder argSB) {
		argSB.append("<select")
			.append(idAttribute())
			.append(nameAttribute());
		
		if (isMultiple()) {
			argSB.append(outputAttribute("multiple", "multiple"))
				.append(outputAttribute("size", String.valueOf(mySize > 0 ? mySize : DEFAULT_SIZE_MULTIPLE)));
		} else if (mySize > 0) { // i.e., size has been set explicitly
			argSB.append(outputAttribute("size", String.valueOf(mySize)));
		}
		
		boolean lclNothingSelected = !isAnyOptionSelected();
		
		argSB.append(attributesToString())
			.append(disabledAttribute())
			.append(allowClearAttribute(lclNothingSelected))
			.append(outputCssClassAttribute());
		
		String lclScript = getScript();
		boolean lclHasScript = StringUtils.isNotBlank(lclScript);
		
		if (isEnabled() && (lclHasScript == false)) {
			argSB.append(onChangeScript());
		}
		
		if (lclHasScript) {
			argSB.append(' ');
			argSB.append(lclScript);
		}
		
		argSB.append(">\n");
		
		DropdownOption<?> lclInstructions = createInstructionsOption(lclNothingSelected);
		if (lclInstructions != null) {
			argSB.append(lclInstructions).append('\n');
		}
		
		for (DropdownEntry<?> lclE : determineEntries()) {
			argSB.append(lclE).append('\n');
		}
		
		argSB.append("</select>");
	}
}
