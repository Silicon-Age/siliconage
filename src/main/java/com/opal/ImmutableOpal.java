package com.opal;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.Validate;

public abstract non-sealed class ImmutableOpal<U extends IdentityUserFacing> extends IdentityOpal<U> {

	//	protected <O extends Opal<U>> ImmutableOpal(OpalFactory<O> argOpalFactory) {
//		super(argOpalFactory);
//	}
	
	private boolean myDataRead = false;

	private final Object[] myValues;
	
	protected <O extends IdentityOpal<U>> ImmutableOpal(IdentityOpalFactory<U, O> argOpalFactory, Object[] argValues) {
		super(argOpalFactory);
		
		Validate.notNull(argValues);
		myValues = argValues;
	}
	
	/* Should only be used to create placeholder NOT_YET_LOADED Opals */
	protected ImmutableOpal() {
		super();
		myValues = null;
	}
	
	/* Must be synchronized when you call this */
	@Override
	protected final void copyNewValuesToOld() {
		/* Since the Opal is immutable, there's nothing to do for the standard values. */
		copyNewValuesToOldInternal();
	}
	
	protected abstract void copyNewValuesToOldInternal();
	
	@Override
	protected final void copyOldValuesToNew() {
		/* Since the Opal is immutable, there's nothing to do for the standard values. */
		copyOldValuesToNewInternal();
	}
	
	protected abstract void copyOldValuesToNewInternal();
	
	@Override
	public boolean isNew() {
		return false;
	}
	
	@Override
	public boolean isDeleted() {
		return false;
	}
	
	@Override
	public boolean exists() {
		return true;
	}
	
	@Override
	public final void unlink() {
		throw new IllegalStateException("Cannot unlink (i.e., delete) objects of this type.");
	}
	
	public final Object[] getValues() {
		return myValues;
	}

	@Override
	public synchronized Object getField(int argFieldIndex) {
		markAsDataRead(); // TODO: This should eventually go away
		return getValues()[argFieldIndex];
	}
	
	@Override
	protected final String toStringField(int argFieldIndex) {
		if (myValues != null) {
			return String.valueOf(myValues[argFieldIndex]);
		} else {
			return "no identifier";
		}
	}
	
	@Override
	public Set<TransactionAware> getRequiredPriorCommits() {
		return Collections.emptySet();
	}

	@Override
	public Set<TransactionAware> getRequiredSubsequentCommits() {
		return Collections.emptySet();
	}

	/* TODO:  This method should eventually go away. */
	@Override
	protected void markAsDataRead() {
		myDataRead = true;
	}
	
	/* TODO:  This method should eventually go away. */
	@Override
	public boolean isDataRead() {
		return myDataRead;
	}

}
