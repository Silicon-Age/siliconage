package com.opal;

public abstract /* value */ class SingleValueOpalKey<O extends Opal<? extends UserFacing>> extends OpalKey<O> {

	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(SingleValueOpalKey.class.getName());
	
	protected final Object myKeyValue;

	protected SingleValueOpalKey(Object argKeyValue) {
		super();
		myKeyValue = argKeyValue; // Might be null
	}
	
	@Override
	public final boolean equals(Object lclO) {
		if (lclO == null) {
			return false;
		}
		if (this == lclO) {
			return true;
		}
		if (this.getClass() != lclO.getClass()) {
			return false;
		}
		
		@SuppressWarnings("unchecked")
		SingleValueOpalKey<O> lclOtherKey = (SingleValueOpalKey<O>) lclO;
		
		Object lclOtherKeyValue = lclOtherKey.myKeyValue;
		
		if (this.myKeyValue == lclOtherKeyValue) {
			return true;
		}
		
		if (this.myKeyValue == null) {
			return false;
		} else {
			return myKeyValue.equals(lclOtherKeyValue); 
		}
	}
	
	protected Object getKeyValue() {
		return myKeyValue;
	}
	
	@Override
	public final int hashCode() {
		if (myKeyValue == null) {
			ourLogger.warn("Computing hashCode of OpalKey " + this + " with a single null value in " + this.getClass().getName() + ".");
		}
		return myKeyValue == null ? 0 : myKeyValue.hashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append(getClass().getName());
		lclSB.append('[');
		lclSB.append(String.valueOf(myKeyValue));
		lclSB.append(']');
		return lclSB.toString();
	}
}
