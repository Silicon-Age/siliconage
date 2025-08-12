package com.opal;

import org.apache.commons.lang3.Validate;

public abstract class MultipleValueOpalKey<O extends Opal<? extends UserFacing>> extends OpalKey<O> {

	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(MultipleValueOpalKey.class.getName());
	
	protected final Object[] myFields;

	protected MultipleValueOpalKey(Object[] argValues) {
		super();
		Validate.notNull(argValues);
		myFields = argValues;
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
		MultipleValueOpalKey<O> lclOtherKey = (MultipleValueOpalKey<O>) lclO;
		
		Object[] lclOtherFields = lclOtherKey.myFields;
		
		if (this.myFields == null || lclOtherFields == null) {
			return false;
		}
		
		/* The following check should not be necessary based on the way in which
		they are constructed, but we'll check it anyway. */
		
		if (this.myFields.length != lclOtherFields.length) {
			return false;
		}
		
		/* FEATURE: We can speed this up in a subclass (?) if we know that the fields aren't null */
		
		for (int lclI = myFields.length-1; lclI >=0; --lclI) {
			Object lclTemp = this.myFields[lclI];
			if (lclTemp == null) {
				if (lclOtherFields[lclI] != null) {
					return false;
				}
			} else if (!lclTemp.equals(lclOtherFields[lclI])) {
				return false;
			}
		}
		
		return true;
	}
	
	protected java.lang.Object[] getFields() {
		return myFields;
	}
	
	// THINK: Is this really the ideal way to generate a hash code?  Probably there is research on this.
	@Override
	public final int hashCode() {
		Validate.notNull(myFields);
		
		int lclHashCode = 0;
		boolean lclAtLeastOneNotNullField = false;
		for (int lclI = 0; lclI < myFields.length; ++lclI) {
			Object lclField = myFields[lclI];
			lclHashCode += (lclField != null) ? lclField.hashCode() : 0;
			if (lclField != null) {
				lclAtLeastOneNotNullField = true;
			} else { // THINK: Does this even rise to the level of Info?  Can we figure out when null fields are to be expected?
//				if (ourLogger.isInfoEnabled()) {
//					ourLogger.info("Field " + lclI + " is null for " + this + ".hashCode() (class = " + this.getClass().getName() + ")");
//				}
			}
		}
		if (lclAtLeastOneNotNullField == false) {
			ourLogger.error("All fields are null for " + this + ".hashCode() (class = " + this.getClass().getName() + ")");
		}
		
		return lclHashCode;
	}
	
	@Override
	public String toString() {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append(getClass().getName());
		lclSB.append('[');
		for (int lclI = 0; lclI < myFields.length; ++lclI) {
			if (lclI !=0) { 
				lclSB.append(',');
			}
			lclSB.append(String.valueOf(myFields[lclI]));
		}
		lclSB.append(']');
		return lclSB.toString();
	}
}
