package com.opal;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.siliconage.util.UnimplementedOperationException;

public abstract class AbstractIdentityOpalFactory<U extends IdentityUserFacing, O extends IdentityOpal<U>> implements IdentityOpalFactory<U, O> {

//	private static final org.apache.log4j.Logger ourLogger = org.apache.log4j.Logger.getLogger(AbstractIdentityOpalFactory.class.getName());

	private final OpalCache<O> myCache;
	
	protected AbstractIdentityOpalFactory() {
		super();
		
		myCache = new OpalCache<>(usesSoftReferences());
	}

	protected boolean usesSoftReferences() {
		return true;
	}
	
	/* In general, anybody who calls getCache() with the intention of adding or removing things, should
	 * synchronize on the cache (not on this Factory object) to prevent threading errors.
	 */
	
	/* This should really be protected, but it's really convenient to be able to clear caches when writing
	 * unit tests.  I'm not sure how to best deal with that.
	 */
	public OpalCache<O> getCache() {
		return myCache;
	}
	
	@SuppressWarnings("unused")
	protected void afterDelete(TransactionParameter argTP, O argOpal) {
		return;
	}
	
	@SuppressWarnings("unused")
	protected void afterInsert(TransactionParameter argTP, O argOpal) throws PersistenceException {
		return;
	}

	@SuppressWarnings("unused")
	protected void afterSave(TransactionParameter argTP, O argOpal) throws PersistenceException {
		return;
	}
	
	@SuppressWarnings("unused")
	protected void afterUpdate(TransactionParameter argTP, O argOpal) throws PersistenceException {
		return;
	}
	
	@SuppressWarnings("unused")
	protected void beforeDelete(TransactionParameter argTP, O argOpal) {
		return;
	}
	
	@SuppressWarnings("unused")
	protected void beforeInsert(TransactionParameter argTP, O argOpal) throws PersistenceException {
		return;
	}
	
	@SuppressWarnings("unused")
	protected void beforeSave(TransactionParameter argTP, O argOpal) throws PersistenceException {
		return;
	}
	
	@SuppressWarnings("unused")
	protected void beforeUpdate(TransactionParameter argTP, O argOpal) throws PersistenceException {
		return;
	}
	
	@Override
	public void commitPhaseOne(TransactionParameter argTP, O argOpal) {
		if (argOpal.isDeleted()) {
			delete(argTP, argOpal);
		} else {
			save(argTP, argOpal);
		}
	}
	
	@Override
	public void commitPhaseTwo(TransactionParameter argTP, O argOpal) {
		return;
	}
	
	protected void delete(TransactionParameter argTP, O argOpal) {
		beforeDelete(argTP, argOpal);
		if (argOpal.isNew() == false) { // CHECK: This will need to change if we start putting uncommitted Opals into the/a cache
			unregisterOpal(argOpal);
		}
		deleteInternal(argTP, argOpal);
		afterDelete(argTP, argOpal);
	}
	
	protected abstract void deleteInternal(TransactionParameter argTP, O argOpal);
	
	protected O forOpalKey(OpalKey<O> argOK) throws PersistenceException {
		return forOpalKey(argOK, false);
	}
	
//	private int myRequests = 0;
//	private int myLoads = 0;
	
	protected O forOpalKey(OpalKey<O> argOK, boolean argConcrete) throws PersistenceException {
		Validate.notNull(argOK);
		OpalCache<O> lclOC = getCache();
		
		/* Lock the OpalCache so that we can check for the Opal's existence (and load it, if necessary), in an atomic fashion. */
		O lclOpal;
		synchronized (lclOC) {
			/* Look up the Opal. */
//			System.out.println("About to look up " + argOK + " in " + this);
			lclOpal = lclOC.forOpalKey(argOK);
//			System.out.println("Looked up " + argOK + "; found " + lclOpal);
			
//			if (ourLogger.isDebugEnabled()) {
//				if ((++myRequests) % 100 == 0) {
//					ourLogger.debug(this.getClass().getName() + " requests = " + myRequests);
//				}
//			}
		}
		
		/* Did we find it? */
		if (lclOpal != null) {
			/* Yes.  Go ahead and return it. */
			if (lclOpal.getUserFacing() == null) { // THINK: How could this happen?
				assert argConcrete;
				determineUserFacing(lclOpal, argConcrete); // THINK: Do we need to have the OpalCache locked while we do this?
			}
			return lclOpal;
		}
		/* No.  So we load it from the persistent store. */
		
//		System.out.println("About to loadFromPersistentStore");
//		if (ourLogger.isDebugEnabled()) {
//			if ((++myLoads) % 10 == 0) {
//				ourLogger.debug(this.getClass().getName() + " loads = " + myLoads);
//			}
//		}

		lclOpal = loadFromPersistentStore(argOK);
//		System.out.println("Done with loadFromPersistentStore");
		/* Did we load anything? */
		if (lclOpal == null) {
			return null;
		}
		
		/* We loaded something.  Did somebody else load it in the brief (?) window while we didn't have the cache locked? */
		
		synchronized (lclOC) {
			O lclCheckOpal = lclOC.forOpalKey(argOK);
			if (lclCheckOpal != null) {
				/* Yes.  Somebody else loaded it, so we'll return that cached one and discard the one we created. */
				lclOpal = lclCheckOpal;
			} else {
				/* No.  Nobody else loaded it.  We register its unique keys in the OpalCache. */
				registerOldOpal(lclOpal);
				assert lclOpal.getUserFacing() == null;
				determineUserFacing(lclOpal, argConcrete);
			}
		}
		if (lclOpal.getUserFacing() == null) {
			throw new IllegalStateException("lclOpal.getUserFacing() == null");
		}
		return lclOpal;
					
	}
	
	protected abstract void determineUserFacing(O argOpal, boolean argConcrete);
	
	protected void insert(TransactionParameter argTP, O argOpal) throws PersistenceException {
		beforeInsert(argTP, argOpal);
		insertInternal(argTP, argOpal);
		afterInsert(argTP, argOpal);
		
		/* At some point before this, any updating of the internal values (database defaults, looking
		 * up sequence values, etc.) should have occurred so that we can safely register this Opal
		 * with the central registry so that it can be looked up later. */
		 
		registerNewOpal(argOpal);
		
		return;
	}
	
	protected abstract void insertInternal(TransactionParameter argTP, O argOpal) throws PersistenceException;
	
	protected abstract O loadFromPersistentStore(OpalKey<O> argOK) throws PersistenceException;
	
	protected abstract void registerNewOpal(O argOpal);
	protected abstract void registerOldOpal(O argOpal);
	
	protected abstract void unregisterOpal(O argOpal); // FEATURE: Similar problems as with updateKeys
	
	protected void save(TransactionParameter argTP, O argOpal) throws PersistenceException {
//		ourLogger.debug(this + ": save()");
		beforeSave(argTP, argOpal);
		
		saveInternal(argTP, argOpal);
		
		afterSave(argTP, argOpal);
	}
	
	protected void saveInternal(TransactionParameter argTP, O argOpal) throws PersistenceException {		
//		ourLogger.debug(this + ": saveInternal()");
		if (argOpal.isNew()) {
			insert(argTP, argOpal);
		} else {
			update(argTP, argOpal);
		}
		return;
	}
	
	protected void update(TransactionParameter argTP, O argOpal) throws PersistenceException {
//		ourLogger.debug(this + ": update()");
		beforeUpdate(argTP, argOpal);
		updateInternal(argTP, argOpal);
		updateKeys(argOpal); // FEATURE: Non-transactional
		afterUpdate(argTP, argOpal);
		return;
	}
	
	protected abstract void updateInternal(TransactionParameter argTP, O argOpal) throws PersistenceException;
	
	@Override
	public abstract Set<O> getAll() throws PersistenceException;
	
	protected abstract String[] getFieldNames();
	protected abstract Class<?>[] getFieldTypes();
	protected abstract boolean[] getFieldNullability();
	protected abstract FieldValidator[] getFieldValidators();
	
	@Override
	public int getFieldCount() {
		return getFieldNames().length;
	}
	
	@Override
	public String getFieldName(int argFieldIndex) {
		return getFieldNames()[argFieldIndex];
	}
	
	@Override
	public Class<?> getFieldType(int argFieldIndex) {
		return getFieldTypes()[argFieldIndex];
	}
	
	@Override
	public boolean getFieldNullability(int argFieldIndex) {
		return getFieldNullability()[argFieldIndex];
	}
	
	@Override
	public FieldValidator getFieldValidator(int argFieldIndex) {
		return getFieldValidators()[argFieldIndex];
	}
	
	@Override
	public int getFieldIndex(String argFieldName) {
		Validate.notNull(argFieldName);
		String[] lclFieldNames = getFieldNames();
		for (int lclI = 0; lclI < lclFieldNames.length; ++lclI) {
			if (argFieldName.equals(lclFieldNames[lclI])) {
				return lclI;
			}
		}
		throw new IllegalArgumentException("\"" + argFieldName + "\" is not a valid field name.");
	}
	
	/**
	 * Must only be called when the thread has a lock on argOpal!!!
	 */
	public abstract void updateKeys(O argOpal);
	
	@Override
	public abstract void acquireForQuery(Collection<O> argCollection, Query argQuery);
	
	/* Do not call this without already being synchronized on argOpal or without it being
	 * part of the current TransactionContext.  In all likelihood, the only method that
	 * calls this should be Opal.reload(). */
	@Override
	public void reload(O argOpal) throws PersistenceException {
		Validate.notNull(argOpal);
		
		throw new UnimplementedOperationException();
		
//		/* Is the Opal newly created within this transaction? */
//		if (argOpal.isNew()) {
//			/* Yes.  It makes no sense to "reload" it since it's not in the persistent store. */
//			return;
//		}
//		/* Has the Opal been unlinked (effectively, deleted?) */
//		if (argOpal.isDeleted()) {
//			/* Yes.  It's not clear what it would mean to reload it; presumably that would involve
//			 * "undeleting" it, but then we'd have to undo the (unknown) effects of unlinkInternal.
//			 * Since the user will (should) know whether or not (s)he has unlinked the object and
//			 * therefore whether or not it can be reloaded, we make this an exception. */
//			 throw new IllegalStateException("Cannot reload unlinked Opal " + this);
//		}
//		
//		synchronized (this) {
//			OpalKey<O> lclOK = createOpalKeyForReloading(argOpal);
//			
//			Object[] lclNewValues = loadValuesFromPersistentStore(lclOK);
//			
//			/* Has the opal been deleted in the meantime? */
//			if (lclNewValues == null) {
//				/* Yes.  We unlink it. */
//				argOpal.unlink(); // THINK: Does this always work?
//			} else {
//				/* No.  We update it. */
//				argOpal.myNewValues = lclNewValues;
//				System.arraycopy(lclNewValues, 0, argOpal.myOldValues, 0, lclNewValues.length);
//				
//				argOpal.updateReferencesAfterReload();
//				argOpal.updateCollectionsAfterReload();
//			}
//			
//			return;
//		}
	}
	
	protected abstract OpalKey<O> createOpalKeyForReloading(O argOpal);
	
	protected abstract Object[] loadValuesFromPersistentStore(OpalKey<O> argOK);
	
	@Override
	public abstract void reloadForQuery(Collection<? extends O> argC, Query argQ);
}
