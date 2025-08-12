package com.opal;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

public abstract class AbstractEphemeralOpalFactory<U extends UserFacing, O extends Opal<U>> implements OpalFactory<U, O> {

	protected AbstractEphemeralOpalFactory() {
		super();
	}
	
	@SuppressWarnings("unused")
	protected final void afterDelete(TransactionParameter argTP, O argOpal) {
		throw new IllegalStateException();
	}
	
	@SuppressWarnings("unused")
	protected final void afterInsert(TransactionParameter argTP, O argOpal) throws PersistenceException {
		throw new IllegalStateException();
	}

	@SuppressWarnings("unused")
	protected final void afterSave(TransactionParameter argTP, O argOpal) throws PersistenceException {
		throw new IllegalStateException();
	}
	
	@SuppressWarnings("unused")
	protected final void afterUpdate(TransactionParameter argTP, O argOpal) throws PersistenceException {
		throw new IllegalStateException();
	}
	
	@SuppressWarnings("unused")
	protected final void beforeDelete(TransactionParameter argTP, O argOpal) {
		throw new IllegalStateException();
	}
	
	@SuppressWarnings("unused")
	protected final void beforeInsert(TransactionParameter argTP, O argOpal) throws PersistenceException {
		throw new IllegalStateException();
	}
	
	@SuppressWarnings("unused")
	protected final void beforeSave(TransactionParameter argTP, O argOpal) throws PersistenceException {
		throw new IllegalStateException();
	}
	
	@SuppressWarnings("unused")
	protected final void beforeUpdate(TransactionParameter argTP, O argOpal) throws PersistenceException {
		throw new IllegalStateException();
	}
	
//	@Override
//	public final void commitPhaseOne(TransactionParameter argTP, O argOpal) {
//		throw new IllegalStateException();
//	}
//	
//	@Override
//	public final void commitPhaseTwo(TransactionParameter argTP, O argOpal) {
//		throw new IllegalStateException();
//	}

	@Override
	public TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap) {
		throw new UnsupportedOperationException();
	}
	
	protected abstract void determineUserFacing(O argOpal, boolean argConcrete);
		
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
		
//	/* Do not call this without already being synchronized on argOpal or without it being
//	 * part of the current TransactionContext.  In all likelihood, the only method that
//	 * calls this should be Opal.reload(). */
//	@Override
//	public void reload(O argOpal) throws PersistenceException {
//		Validate.notNull(argOpal);
//		
//		throw new UnimplementedOperationException();
//		
////		/* Is the Opal newly created within this transaction? */
////		if (argOpal.isNew()) {
////			/* Yes.  It makes no sense to "reload" it since it's not in the persistent store. */
////			return;
////		}
////		/* Has the Opal been unlinked (effectively, deleted?) */
////		if (argOpal.isDeleted()) {
////			/* Yes.  It's not clear what it would mean to reload it; presumably that would involve
////			 * "undeleting" it, but then we'd have to undo the (unknown) effects of unlinkInternal.
////			 * Since the user will (should) know whether or not (s)he has unlinked the object and
////			 * therefore whether or not it can be reloaded, we make this an exception. */
////			 throw new IllegalStateException("Cannot reload unlinked Opal " + this);
////		}
////		
////		synchronized (this) {
////			OpalKey<O> lclOK = createOpalKeyForReloading(argOpal);
////			
////			Object[] lclNewValues = loadValuesFromPersistentStore(lclOK);
////			
////			/* Has the opal been deleted in the meantime? */
////			if (lclNewValues == null) {
////				/* Yes.  We unlink it. */
////				argOpal.unlink(); // THINK: Does this always work?
////			} else {
////				/* No.  We update it. */
////				argOpal.myNewValues = lclNewValues;
////				System.arraycopy(lclNewValues, 0, argOpal.myOldValues, 0, lclNewValues.length);
////				
////				argOpal.updateReferencesAfterReload();
////				argOpal.updateCollectionsAfterReload();
////			}
////			
////			return;
////		}
//	}
//	
//	@Override
//	public final void reloadForQuery(Collection<O> argC, Query argQ) {
//		throw new IllegalStateException();
//	}
//	
//	@Override
//	public O forUniqueString(String argUniqueString) {
//		throw new UnimplementedOperationException();
//	}
}
