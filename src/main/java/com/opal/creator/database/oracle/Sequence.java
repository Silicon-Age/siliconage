package com.opal.creator.database.oracle;

import org.apache.commons.lang3.Validate;

public class Sequence {
	private final String myOwner;
	private final String myName;
	
	public Sequence(String argOwner, String argName) {
		super();
		
		Validate.notNull(argOwner);
		myOwner = argOwner;
		
		Validate.notNull(argName);
		myName = argName;
	}
	
	public String getName() {
		return myName;
	}
	
	public String getOwner() {
		return myOwner;
	}
}
