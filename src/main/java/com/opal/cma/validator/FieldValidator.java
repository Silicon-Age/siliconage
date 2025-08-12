package com.opal.cma.validator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * @author topquark
 */
@Deprecated
public abstract class FieldValidator {
	protected FieldValidator() {
		super();
	}
	
	protected abstract Class<?> getParameterType();
	
	protected void checkObjectType(Object argObject) {
		Class<?> lclC = getParameterType();
		if (lclC == null || lclC.isInstance(argObject)) {
			return;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public List<String> validate(Object argObject) {
		try {
			if (argObject != null ) {
				checkObjectType(argObject);
			}
			ArrayList<String> lclAL = new ArrayList<>(); /* FIXME:  Lots of ArrayLists being built here */
			validateInternal(lclAL, argObject);
			return lclAL;
		} catch (IllegalArgumentException lclE) {
			Validate.notNull(argObject);
			ArrayList<String> lclAL = new ArrayList<>();
			lclAL.add("Parameter " + argObject + " of class " + argObject.getClass() + " as a " + getParameterType() + ".");
			return lclAL;
		}
	}
	
	protected abstract void validateInternal(List<String> argList, Object argObject);
}
