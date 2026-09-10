package com.opal.creator.database;

import java.util.Objects;

/**
 * @author topquark
 */
public abstract class CheckConstraint {
	private final String myName;
	private final String mySQLDefinition;
	
	public CheckConstraint(String argName, String argText) {
		super();
		myName = Objects.requireNonNull(argName);
		
		mySQLDefinition = Objects.requireNonNull(argText);
	}

	public String getName() {
		return myName;
	}
	
	public String getSQLDefinition() {
		return mySQLDefinition;
	}	
}


