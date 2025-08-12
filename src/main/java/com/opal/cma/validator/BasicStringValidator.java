package com.opal.cma.validator;

import java.util.List;

/**
 * @author topquark
 */
@Deprecated
public class BasicStringValidator extends FieldValidator {
	private final boolean myNullAllowed;
	private final int myMinLength;
	private final int myMaxLength;
	
	public BasicStringValidator(boolean argNullAllowed, int argMinLength, int argMaxLength) {
		super();
		myNullAllowed = argNullAllowed;
		myMinLength = argMinLength;
		myMaxLength = argMaxLength;
	}
	
	public boolean isNullAllowed() {
		return myNullAllowed;
	}
	
	public int getMinLength() {
		return myMinLength;
	}
	
	public int getMaxLength() {
		return myMaxLength;
	}
	
	@Override
	protected Class<?> getParameterType() {
		return String.class;
	}
	
	@Override
	protected void validateInternal(List<String> argList, Object argObject) {
		int lclLength;
		if (argObject == null) {
			if (!isNullAllowed()) {
				argList.add("%f may not be blank.");
				return;
			}
			lclLength = 0;
		} else {
			lclLength = ((String) argObject).length();
		}
		if (lclLength < getMinLength()) {
			argList.add("%f must be at least " + getMinLength() + " characters long.");
		} else if (lclLength > getMaxLength()) {
			argList.add("%f must be no more than " + getMaxLength() + " characters long.");
		}
		return;
	}
}
