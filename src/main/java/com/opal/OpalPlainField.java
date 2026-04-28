package com.opal;

public class OpalPlainField <U extends UserFacing/*<U>*/, T> extends OpalSpecificField<U, T> { // OPALFIXME
	
	public OpalPlainField(OpalBaseField<U, T> argBaseField) {
		super(argBaseField);
	}
	
}
