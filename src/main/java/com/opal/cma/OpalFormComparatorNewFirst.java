package com.opal.cma;

import java.util.Comparator;

import org.apache.commons.lang3.Validate;

import com.opal.IdentityUserFacing;

public class OpalFormComparatorNewFirst<U extends IdentityUserFacing> implements Comparator<OpalForm<U>> {
	private final Comparator<U> myComparator;
	
	public OpalFormComparatorNewFirst(Comparator<U> argComparator) {
		super();
		Validate.notNull(argComparator);
		myComparator = argComparator;
	}
	
	public Comparator<U> getComparator() {
		return myComparator;
	}
	
	@Override
	public int compare(OpalForm<U> argA, OpalForm<U> argB) {
		Validate.notNull(argA);
		Validate.notNull(argB);
		
		U lclA = argA.getUserFacing();
		U lclB = argB.getUserFacing();
		
		/* If the internal UserFacings are null, it indicates that the OpalForm corresponds to a blank
		 * record for inserting a new record.  In that case, they should be displayed at the beginning.
		 */
		if (lclA == null) {
			if (lclB == null) {
				return 0;
			} else {
				return -1; /* Blank A comes first */
			}
		} else {
			if (lclB == null) {
				return 1; /* Blank B comes first */
			} else {
				return getComparator().compare(lclA, lclB);
			}
		}
	}
}
