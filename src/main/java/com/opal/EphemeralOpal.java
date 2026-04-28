package com.opal;

import org.apache.commons.lang3.Validate;

public abstract class EphemeralOpal<U extends UserFacing/*<U>*/> implements Opal<U> { // OPALFIXME
	
	private U myUserFacing;
	
	private final Object[] myValues;
	
	protected <O extends Opal<U>> EphemeralOpal(OpalFactory<U, O> argOpalFactory, Object[] argValues) {
		super();
		
		Validate.notNull(argValues);
		myValues = argValues;
	}
	
	@Override
	public U getUserFacing() {
		return myUserFacing;
	}
	
	public void setUserFacing(U argUserFacing) { // THINK: May argUserFacing be null?
		myUserFacing = argUserFacing;
	}
	
	protected final Object[] getValues() { // THINK: Does this have to be public?
		return myValues;
	}

	@Override
	public synchronized Object getFieldValue(int argFieldIndex) { // Does this have to be synchronized?
		return getValues()[argFieldIndex];
	}
	
	protected final String toStringField(int argFieldIndex) { // THINK: Can this be refactored higher?
		if (myValues != null) {
			return String.valueOf(myValues[argFieldIndex]);
		} else {
			return "no identifier"; // Why do we refer to this as an "identifier"?
		}
	}
	
}
