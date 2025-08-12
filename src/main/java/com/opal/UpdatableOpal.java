package com.opal;

import org.apache.commons.lang3.Validate;

public abstract non-sealed class UpdatableOpal<U extends IdentityUserFacing> extends IdentityOpal<U> {

//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(UpdatableOpal.class.getName());
	
	/* Bit field for various status flags. */
	private static final int MY_OLD_OPAL_EXISTS = 1 << 0;
	private static final int MY_NEW_OPAL_EXISTS = 1 << 1;
	private static final int MY_DATA_READ = 1 << 2;
	private int myFlags = MY_OLD_OPAL_EXISTS | MY_NEW_OPAL_EXISTS;
	
	private Object[] myOldValues;
	private Object[] myNewValues;
	
	protected UpdatableOpal(){
		super();
	}
	
	protected <O extends IdentityOpal<U>> UpdatableOpal(IdentityOpalFactory<U, O> argOpalFactory) {
		super(argOpalFactory);
	}
	
	protected <O extends IdentityOpal<U>> UpdatableOpal(IdentityOpalFactory<U, O> argOpalFactory, Object[] argValues) {
		this(argOpalFactory);
		
		myOldValues = argValues; /* Might be null */
	}
	
	private /* synchronized */ final boolean doesOldOpalExist() {
		return (myFlags & MY_OLD_OPAL_EXISTS) != 0;
	}
	
	private /* synchronized */ final boolean doesNewOpalExist() {
		return (myFlags & MY_NEW_OPAL_EXISTS) != 0;
	}
	
	private /* synchronized */ final void setOldOpalExists(boolean argValue) {
		if (argValue) {
			myFlags |= MY_OLD_OPAL_EXISTS;
		} else {
			myFlags &= ~MY_OLD_OPAL_EXISTS;
		}
	}
	
	private /* synchronized */ final void setNewOpalExists(boolean argValue) {
		if (argValue) {
			myFlags |= MY_NEW_OPAL_EXISTS;
		} else {
			myFlags &= ~MY_NEW_OPAL_EXISTS;
		}
	}
	
	@Override
	public synchronized final boolean isDeleted() {
		/* Note that an object can be both "new" and "deleted" at the same time. */
		return !doesNewOpalExist();
	}
	
	@Override
	public synchronized boolean isNew() {
		/* Note that an object can be "New" even if it has been deleted before being persisted; that is, both
		myOldExists and myNewExists are false. */
		return !doesOldOpalExist();
	}
	
	@Override
	public synchronized final boolean exists() {
		return tryAccess() ? doesNewOpalExist() : doesOldOpalExist();
	}
	
	/*
	 * For the love of God, don't call this method unless you know what you are doing.
	 * Might return null!
	 */ 
	public final Object[] getOldValues() {
		return myOldValues;
	}
	
	/*
	 * For the love of God, don't call this method unless you know what you are doing.
	 * Might return null!
	 */
	public final Object[] getNewValues() {
		return myNewValues;
	}

	@Override
	public synchronized Object getField(int argFieldIndex) {
		markAsDataRead(); // TODO: This should eventually go away
		return getReadValueSet()[argFieldIndex];
	}
	
	public void setField(String argFieldName, Object argValue) {
//		markAsDataRead(); // TODO: This should eventually go away
		setField(getFieldIndex(argFieldName), argValue);
		return;
	}
	
	public synchronized void setField(int argFieldIndex, Object argValue) {
		if (argValue == null) {
			if (getFieldNullability(argFieldIndex)) {
				tryMutate();
				getReadValueSet()[argFieldIndex] = null;
			} else {
				throw new IllegalNullArgumentException("Field " + getFieldName(argFieldIndex) + " of " + this + " may not be set to null.");
			}
		} else {
			if (getFieldType(argFieldIndex).isAssignableFrom(argValue.getClass())) {
				tryMutate();
				getReadValueSet()[argFieldIndex] = argValue;
			} else {
				throw new IllegalArgumentException("The value " + argValue + " may not be assigned to field " + getFieldName(argFieldIndex) + ".");
			}
		}
	}
	
	/* Must be synchronized when you call this */
	/* CHECK: Let's see if synchronizing this is enough to make FindBugs realize that all accesses to the fields are actually synchronized.
	 * Nope.  !&@^#%$ */
	@Override
	protected /* synchronized */ final void copyNewValuesToOld() {
//		ourLogger.debug("cNVTO mOE = " + myOldExists + " mNE = " + myNewExists);
		setOldOpalExists(doesNewOpalExist());
		
		/* THINK: If this Opal has been deleted, should we null out both of these arrays? */
		Validate.notNull(myNewValues);
		
		if (myNewValues != null) {
			myOldValues = myNewValues;
			myNewValues = null;
		}
		
		copyNewValuesToOldInternal();
	}
	
	@Override
	protected /* synchronized */ final void copyOldValuesToNew() {
		setNewOpalExists(doesOldOpalExist()); /* THINK: This should always be true, right? */
		
		final int lclLength = getFieldCount();
		if (myNewValues == null) { myNewValues = new Object[lclLength]; }
		if (myOldValues != null) { System.arraycopy(myOldValues, 0, myNewValues, 0, lclLength); }
		
		copyOldValuesToNewInternal();
	}
	
	protected abstract void copyNewValuesToOldInternal();
	
	protected abstract void copyOldValuesToNewInternal();
	
	protected synchronized final Object[] getReadValueSet() {
		return tryAccess() ? myNewValues : myOldValues;
	}
	
	/* Must be synchronized when you call this */
	public /* synchronized */ final void newObject() {
		ensureMonitor();
		
		setOldOpalExists(false); /* Identify the object as new */
		
		/* FEATURE:  It may be more efficient to be able to pass in a TransactionContext */
		
		joinActiveTransactionContext();
		
		/* If this Opal maps a table that participates in subtable polymorphism, then we also need to create
		 * the superclass Opals.
		 */
		createSuperclassOpals();
		
		/* A side effect on joining the active TransactionContext is that the Opal will have its old values
		 * copied to its new values which will end up creating the array that will hold the new values.  The
		 * default values could not be set until that array is created. */
		
		applyDefaults();
		
		/* Copying old values to new also copies the existence flag; in this case we need to set it right. */
		
		setNewOpalExists(true);
	}
	
	/* By default, there are no defaults */
	protected void applyDefaults() {
		return;
	}
	
	/* A critical side effect of "real" createSuperClassOpals methods in subclasses is that they join this new Opal
	 * to the active TransactionContext.  If we don't have a superclass, though, we still need to join that
	 * TransactionContext.
	 */
	protected void createSuperclassOpals() {
		return;
	}
	
	@Override
	public synchronized void unlink() {
		tryMutate();
		unlinkInternal();
		setNewOpalExists(false);
//		ourLogger.debug("Unlinking " + this + ".  isDeleted() == " + isDeleted());
	}
	
	protected abstract void unlinkInternal();
	
	/* FIXME: Ideally this method would only exist for Creatable Opals */
	public UpdatableOpal<U> copy() {
		@SuppressWarnings("unchecked")
		UpdatableOpal<U> lclCopy = (UpdatableOpal<U>) ((OpalFactoryCreator<U, Opal<U>>) getOpalFactory()).create();
		copyFieldsToInternal(lclCopy);
		return lclCopy;
	}
	
	protected abstract void copyFieldsToInternal(UpdatableOpal<U> argOriginal);
	
	@Override
	protected final String toStringField(int argFieldIndex) {
		if (myNewValues != null) {
			return String.valueOf(myNewValues[argFieldIndex]);
		} else if (myOldValues != null) {
			return String.valueOf(myOldValues[argFieldIndex]);
		} else {
			return "no identifier";
		}
	}
	
	/* TODO:  This method should eventually go away. */
	@Override
	protected void markAsDataRead() {
		myFlags |= MY_DATA_READ;
	}
	
	/* TODO:  This method should eventually go away. */
	@Override
	public boolean isDataRead() {
		return (myFlags & MY_DATA_READ) != 0;
	}

}
