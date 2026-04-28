package com.opal.cma;

import com.opal.IdentityFactory;
import com.opal.IdentityUserFacing;
import com.opal.UserFacing;

public abstract class SpecialHandler<U extends IdentityUserFacing/*<U>*/> { // OPALFIXME
	protected SpecialHandler() {
		super();
	}
	
	/* Note that OpalForm<?> (rather than OpalForm<U>) is correct.  U is the type of the field being computed,
	 * while OpalForm<?> is an OpalForm for a different UserFacing that contains the field.  So we might be working
	 * with a SpecialHandler<ContactType> as part of processing an OpalForm<Contact> . . . because a Contact has
	 * a child ContactType.
	 */
	public abstract String getDisplay(OpalForm<? extends UserFacing/*<?>*/> argForm, String argFieldName); // OPALFIXME

	@SuppressWarnings("unused")
	public String getDefault(OpalForm<? extends UserFacing/*<?>*/> argForm, String argFieldName) { // OPALFIXME
		return "";
	}

	public abstract U determineUserFacing(OpalFormUpdater<? extends IdentityUserFacing/*<?>*/> argUpdater, IdentityFactory<U> argFactory, String argFieldName, String argData); // OPALFIXME
}
