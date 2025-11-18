package com.opal;

public class OpalPlainField <U extends UserFacing, T> extends OpalSpecificField<U, T> {
	
	public OpalPlainField(OpalBaseField<U, T> argBaseField) {
		super(argBaseField);
	}
	
}
