package com.opal.cma;

import com.opal.IdentityFactory;
import com.opal.IdentityUserFacing;
import com.opal.UserFacing;

public abstract class SpecialHandler<U extends IdentityUserFacing> {
	protected SpecialHandler() {
		super();
	}
	
	/* Note that OpalForm<?> (rather than OpalForm<U> is correct.  U is the type of the field being computed,
	 * while OpalForm<?> is an OpalForm for the UserFacing that contains the field.  So we might be working
	 * with a SpecialHandler<ContactType> as part of processing an OpalForm<Contact>.
	 */
	public abstract String getDisplay(OpalForm<? extends UserFacing> argForm, String argFieldName /*, boolean argDisabled */);

	@SuppressWarnings("unused")
	public String getDefault(OpalForm<? extends UserFacing> argForm, String argFieldName /*, boolean argDisabled */) {
		return "";
	}

	public abstract U determineUserFacing(OpalFormUpdater<? extends IdentityUserFacing> argUpdater, IdentityFactory<U> argFactory, String argFieldName, String argData);
}
