package com.opal;

public abstract sealed class IdentityOpal<U extends IdentityUserFacing> extends TransactionalOpal<U> permits UpdatableOpal/*<U>*/, ImmutableOpal/*<U>*/ {

	protected IdentityOpal() {
		super();
	}
	
	protected <O extends IdentityOpal<U>> IdentityOpal(IdentityOpalFactory<U, O> argOpalFactory) {
		super(argOpalFactory);
	}
	
	public IdentityOpalFactory<U, IdentityOpal<U>> getIdentityOpalFactory() {
		@SuppressWarnings("unchecked")
		IdentityOpalFactory<U, IdentityOpal<U>> lclIOF = (IdentityOpalFactory<U, IdentityOpal<U>>) getOpalFactory();
		return lclIOF;
	}
	
	public String getUniqueString() {
		Object[] lclUniqueStringValues = getUniqueStringKeyWhereClauseValues();
		if (lclUniqueStringValues.length == 1) {
			return String.valueOf(lclUniqueStringValues[0]);
		} else {
			/* FEATURE:  Must fix this to produce only printable strings and escape ampersands */
			StringBuilder lclSB = new StringBuilder();
			for (int lclI = 0; lclI < lclUniqueStringValues.length; ++lclI) {
				if (lclI != 0) {
					lclSB.append('|'); // FEATURE: This should be configurable
				}
				lclSB.append(String.valueOf(lclUniqueStringValues[lclI]));
			}
			return lclSB.toString();
		}
	}

	public Object[] getUniqueStringKeyWhereClauseValues() {
		return getPrimaryKeyWhereClauseValues();
	}

	public abstract Object[] getPrimaryKeyWhereClauseValues();

	public synchronized void reload() {
		tryMutate();
		try (TransactionContext lclTC = TransactionContext.joinActiveOrCreate()) {
			getIdentityOpalFactory().reload(this);
			lclTC.complete();
		}
	}
	
	protected void updateReferencesAfterReload() {
		/* The default action is to assume that there are no references that need to be updated
		 * and to do nothing. */
	}
	
	protected void updateCollectionsAfterReload() {
		/* The default action is to assume that there are no collections that need to be updated
		 * and to do nothing. */
	}
	
	public abstract boolean isNew();
	
	public abstract boolean isDeleted();
	
	public abstract void unlink();
	
	// TODO: Shouldn't be final
	@Override
	protected final void commitPhaseOneInternal(TransactionParameter argTP) throws PersistenceException {
		ensureMonitor();
		getIdentityOpalFactory().commitPhaseOne(argTP, this);
	}
	
	/* Must be synchronized when you call this */
	// TODO: Shouldn't be final
	@Override
	protected /* synchronized */ final void commitPhaseTwoInternal(TransactionParameter argTP) throws PersistenceException {
		ensureMonitor();
		copyNewValuesToOld();
		getIdentityOpalFactory().commitPhaseTwo(argTP, this);
	}
	
	protected abstract void markAsDataRead();
	
	public abstract boolean isDataRead();

}
