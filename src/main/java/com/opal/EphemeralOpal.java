package com.opal;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.Validate;

public abstract class EphemeralOpal<U extends UserFacing> implements Opal<U> {
	
	private U myUserFacing;
	
//	protected <O extends Opal<U>> ImmutableOpal(OpalFactory<O> argOpalFactory) {
//		super(argOpalFactory);
//	}
	
	private final Object[] myValues;
	
	protected <O extends Opal<U>> EphemeralOpal(OpalFactory<U, O> argOpalFactory, Object[] argValues) {
		super();
//		super(argOpalFactory); // Do we actually have to store this for EphemeralOpals?
		
		Validate.notNull(argValues);
		myValues = argValues;
	}
	
	/* Should only be used to create placeholder NOT_YET_LOADED Opals */
//	protected EphemeralOpal() {
//		super();
//		myValues = null;
//	}
	
	public U getUserFacing() {
		return myUserFacing;
	}
	
	public void setUserFacing(U argUserFacing) { // THINK: May argUserFacing be null?
		myUserFacing = argUserFacing;
	}
	
//	/* Must be synchronized when you call this */
//	@Override
//	protected final void copyNewValuesToOld() {
//		/* Since the Opal is immutable, there's nothing to do for the standard values. */
//		copyNewValuesToOldInternal();
//	}
//	
//	protected abstract void copyNewValuesToOldInternal();
//	
//	@Override
//	protected final void copyOldValuesToNew() {
//		/* Since the Opal is immutable, there's nothing to do for the standard values. */
//		copyOldValuesToNewInternal();
//	}
	
//	protected abstract void copyOldValuesToNewInternal();
	
//	@Override
//	public boolean isNew() {
//		return false;
//	}
//	
//	@Override
//	public boolean isDeleted() {
//		return false;
//	}
	
//	@Override
//	public boolean exists() {
//		return true;
//	}
	
//	@Override
//	public final void unlink() {
//		throw new UnsupportedOperationException();
//	}
	
	public final Object[] getValues() { // THINK: Does this have to be public?
		return myValues;
	}

	@Override
	public synchronized Object getField(int argFieldIndex) { // Does this have to be synchronized?
		return getValues()[argFieldIndex];
	}
	
//	@Override
	protected final String toStringField(int argFieldIndex) { // THINK: Can this be refactored higher?
		if (myValues != null) {
			return String.valueOf(myValues[argFieldIndex]);
		} else {
			return "no identifier"; // Why do we refer to this as an "identifier"?
		}
	}
	
//	@Override
//	public Set<TransactionAware> getRequiredPriorCommits() {
//		return Collections.emptySet();
//	}
//
//	@Override
//	public Set<TransactionAware> getRequiredSubsequentCommits() {
//		return Collections.emptySet();
//	}
	
	/* Because these are Ephemeral opals that can can never be updated (including quasi-updates like having
	 * Collections of other opals change), there is no need for committing to do anything.  In fact,
	 * these routines probably shouldn't exist at all, but it would be a fairly major architecture change
	 * to make IdentityOpals the only ones that implemented TransactionAware.
	 */
//	@Override
//	public void commitPhaseOneInternal(TransactionParameter argTP) {
//		return;
//	}
//	
//	@Override
//	public void commitPhaseTwoInternal(TransactionParameter argTP) {
//		return;
//	}
}
