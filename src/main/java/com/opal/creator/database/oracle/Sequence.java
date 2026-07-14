package com.opal.creator.database.oracle;

import java.util.Objects;

public class Sequence {
	private final String myOwner;
	private final String myName;
	
	public Sequence(String argOwner, String argName) {
		super();
		
		myOwner = Objects.requireNonNull(argOwner);
		
		myName = Objects.requireNonNull(argName);
	}
	
	public String getName() {
		return myName;
	}
	
	public String getOwner() {
		return myOwner;
	}
}
