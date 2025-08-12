package com.opal;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

public abstract class TransactionalOpal<U extends UserFacing> extends AbstractTransactionAware implements Opal<U> /*, Serializable */ {

	//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(Opal.class.getName());
	
	/* THINK:  What, if anything, should be transient? */
	
	/* FEATURE: This should be transient or removed in favor of a lookup to a list of physical stores. */
	
	private final OpalFactory<U, ? extends TransactionalOpal<U>> myAbstractOpalFactory;
	
	/* This is "basically final":  Once it is set, it should never be changed. */
	/* final */ private U myUserFacing; /* FIXME: Is there a way to make this actually final? */

	/*
	 * This should only be used to construct the NOT_YET_LOADED placeholders.
	 */
	 
	protected TransactionalOpal() {
		super();
		myAbstractOpalFactory = null;
	}
	
	protected <O extends TransactionalOpal<U>> TransactionalOpal(OpalFactory<U, O> argOpalFactory) {
		super();
		
		Validate.notNull(argOpalFactory);
		
		myAbstractOpalFactory = argOpalFactory;
		
		initializeReferences();
	}
	
	protected void initializeReferences() {
		return;
	}
	

	public final U getUserFacing() {
		return myUserFacing;
	}
	
	public final void setUserFacing(U argUserFacing) {
		myUserFacing = argUserFacing;
	}
	
	@Override
	protected final TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap) {
		return getOpalFactory().extractTransactionParameter(argTPMap);
	}
	
	@Override
	public final boolean equals(Object lclO) {
//		ourLogger.debug(this + "/" + System.identityHashCode(this) + " and " + lclO + "/" + System.identityHashCode(lclO));
		return this == lclO;
	}
	
	public abstract boolean exists();
	
//	public abstract int getFieldCount();
	
	public final OpalFactory<U, ? extends TransactionalOpal<U>> getOpalFactory() {
		return myAbstractOpalFactory;
	}
	
	@Override
	public final int hashCode() {
//		ourLogger.debug("hashCode for " + this + " is " + super.hashCode());
		return super.hashCode();
	}
		
	/* Must be synchronized when you call this */	
	protected /* synchronized */ void leaveTransactionContextInternal() {
		ensureMonitor();
	}
	
	/* Must be synchronized when you call this */	
	@Override
	protected /* synchronized */ void rollbackInternal() {
		return;
	}
	
	public abstract void translateReferencesToFields();
	
//	public Object getField(String argFieldName) {
//		return getField(getFieldIndex(argFieldName));
//	}
//	
//	public abstract Object getField(int argFieldIndex);
//	
//	public Class<?> getFieldType(String argFieldName) {
//		return getFieldType(getFieldIndex(argFieldName));
//	}
//	
//	public Class<?> getFieldType(int argFieldIndex) {
//		return getFieldTypes()[argFieldIndex];
//	}
//	
//	public int getFieldIndex(String argFieldName) {
//		Validate.notNull(argFieldName);
//		String[] lclFieldNames = getFieldNames();
//		for (int lclI = 0; lclI < lclFieldNames.length; ++lclI) {
//			if (argFieldName.equals(lclFieldNames[lclI])) {
//				return lclI;
//			}
//		}
//		throw new IllegalArgumentException("\"" + argFieldName + "\" is not a valid field name.");
//	}
	
//	protected abstract String[] getFieldNames();
//	protected abstract Class<?>[] getFieldTypes();
//	protected abstract boolean[] getFieldNullability();
//	protected abstract FieldValidator[] getFieldValidators();
	
//	public abstract String[] getFieldNames();
//	public abstract Class<?>[] getFieldTypes();
//	public abstract boolean[] getFieldNullability();
//	public abstract FieldValidator[] getFieldValidators();
//	
//	public String getFieldName(int argFieldIndex) {
//		return getFieldNames()[argFieldIndex];
//	}
//	
//	public boolean getFieldNullability(String argFieldName) {
//		return getFieldNullability(getFieldIndex(argFieldName));
//	}
//	
//	public boolean getFieldNullability(int argFieldIndex) {
//		return getFieldNullability()[argFieldIndex];
//	}
//	
//	public FieldValidator getFieldValidator(String argFieldName) {
//		return getFieldValidator(getFieldIndex(argFieldName));
//	}
//	
//	public FieldValidator getFieldValidator(int argFieldIndex) {
//		return getFieldValidators()[argFieldIndex];
//	}
	
	protected abstract String toStringField(int argFieldIndex); // THINK: Can this be default-implemented somewhere higher?
	
	protected abstract void copyOldValuesToNew();
	
	protected abstract void copyNewValuesToOld();
		
	@Override
	protected /* synchronized */ void joinTransactionContextInternal() {
		ensureMonitor();
		copyOldValuesToNew();
		return;
	}
	
	@Override
	public abstract Set<TransactionAware> getRequiredPriorCommits();

	@Override
	public abstract Set<TransactionAware> getRequiredSubsequentCommits();

	public abstract void output(PrintStream argPS) throws IOException;
	
	public abstract void output(PrintWriter argPW) throws IOException;
	
//	/* TODO:  This method should eventually go away. */
//	protected void markAsDataRead() {
//		myDataRead = true;
//	}
//	
//	/* TODO:  This method should eventually go away. */
//	public boolean isDataRead() {
//		return myDataRead;
//	}
	
}
