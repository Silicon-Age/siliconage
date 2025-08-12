package com.opal.creator.database;

import org.apache.commons.lang3.Validate;

/**
 * @author topquark
 */
public abstract class CheckConstraint {
	private final String myName;
	private final String myText;
	
	public CheckConstraint(String argName, String argText) {
		super();
		Validate.notNull(argName);
		myName = argName;
		
		Validate.notNull(argText);
		myText = argText;
	}

	public String getName() {
		return myName;
	}
	
	public String getText() {
		return myText;
	}
	
	public abstract String generateFieldValidatorCode();
}
