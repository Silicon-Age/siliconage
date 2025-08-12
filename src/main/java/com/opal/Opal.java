package com.opal;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

public interface Opal<U extends UserFacing> /* extends AbstractTransactionAware */ /* implements Serializable */ {

	//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(Opal.class.getName());
	
	/* THINK:  What, if anything, should be transient? */
	
	/* FEATURE: This should be transient or removed in favor of a lookup to a list of physical stores. */
	
//	private final OpalFactory<U, ? extends Opal<U>> myAbstractOpalFactory;
	
	/* This is "basically final":  Once it is set, it should never be changed. */
//	/* final */ private U myUserFacing; /* FIXME: Is there a way to make this actually final? */

//	private boolean myDataRead = false;
	
	/*
	 * This should only be used to construct the NOT_YET_LOADED placeholders.
	 */
	 
//	protected Opal() {
//		super();
//		myAbstractOpalFactory = null;
//	}
	
//	protected <O extends Opal<U>> Opal(OpalFactory<U, O> argOpalFactory) {
//		super();
//		
//		Validate.notNull(argOpalFactory);
//		
//		/* TODO: Can I remove the warning on this line? */
//		myAbstractOpalFactory = argOpalFactory;
//		
//		initializeReferences();
//	}
	
//	protected void initializeReferences() {
//		return;
//	}
	
	public U getUserFacing();
	
//	public U getUserFacing() {
//		return myUserFacing;
//	}
	
//	public void setUserFacing(U argUserFacing) {
//		myUserFacing = argUserFacing;
//	}
	
//	@Override
//	protected final TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap) {
//		return getOpalFactory().extractTransactionParameter(argTPMap);
//	}
	
//	@Override
//	public final boolean equals(Object lclO) {
////		ourLogger.debug(this + "/" + System.identityHashCode(this) + " and " + lclO + "/" + System.identityHashCode(lclO));
//		return this == lclO;
//	}
	
//	public abstract boolean exists();

//	public int getFieldCount();
	
//	protected abstract int getFieldCount();
	
//	public final OpalFactory<U, ? extends Opal<U>> getOpalFactory() {
//		return myAbstractOpalFactory;
//	}
	
//	@Override
//	public final int hashCode() {
////		ourLogger.debug("hashCode for " + this + " is " + super.hashCode());
//		return super.hashCode();
//	}
		
	/* Must be synchronized when you call this */
	
//	protected void leaveTransactionContextInternal() {
//		ensureMonitor();
//	}
	
//	@Override
//	protected void rollbackInternal() {
//		return;
//	}
	
//	public abstract void translateReferencesToFields();
	
//	public Object getField(String argFieldName) {
//		return getField(getFieldIndex(argFieldName));
//	}

	public Object getField(int argFieldIndex);
	
	default int getFieldIndex(String argFieldName) {
		Validate.notNull(argFieldName);
		String[] lclFieldNames = getFieldNames();
		for (int lclI = 0; lclI < lclFieldNames.length; ++lclI) {
			if (argFieldName.equals(lclFieldNames[lclI])) {
				return lclI;
			}
		}
		throw new IllegalArgumentException("\"" + argFieldName + "\" is not a valid field name.");
	}

	public String[] getFieldNames();
	public Class<?>[] getFieldTypes();
	public boolean[] getFieldNullability();
	public FieldValidator[] getFieldValidators();

	default int getFieldCount() {
		return getFieldNames().length;
	}
	
	default String getFieldName(int argFieldIndex) {
		return getFieldNames()[argFieldIndex];
	}
	
	default Class<?> getFieldType(String argFieldName) {
		return getFieldType(getFieldIndex(argFieldName));
	}
	
	default Class<?> getFieldType(int argFieldIndex) {
		return getFieldTypes()[argFieldIndex];
	}
	
	default boolean getFieldNullability(String argFieldName) {
		return getFieldNullability(getFieldIndex(argFieldName));
	}

	default boolean getFieldNullability(int argFieldIndex) {
		return getFieldNullability()[argFieldIndex];
	}

	default FieldValidator getFieldValidator(String argFieldName) {
		return getFieldValidator(getFieldIndex(argFieldName));
	}

	default FieldValidator getFieldValidator(int argFieldIndex) {
		return getFieldValidators()[argFieldIndex];
	}

//	protected abstract String toStringField(int argFieldIndex);
//	
//	protected abstract void copyOldValuesToNew();
//	
//	protected abstract void copyNewValuesToOld();
		
//	@Override
//	protected /* synchronized */ void joinTransactionContextInternal() {
//		ensureMonitor();
//		copyOldValuesToNew();
//		return;
//	}
	
//	@Override
//	public abstract Set<TransactionAware> getRequiredPriorCommits();

//	@Override
//	public abstract Set<TransactionAware> getRequiredSubsequentCommits();

//	default String defaultToString() {
//		return getClass().getName() + "@" + System.identityHashCode(this);
//	}
	
	public void output(PrintStream argPS) throws IOException;

//	public abstract void output(PrintStream argPS) throws IOException;

	public void output(PrintWriter argPW) throws IOException;

//	public abstract void output(PrintWriter argPW) throws IOException;
	
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
