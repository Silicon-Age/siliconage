package com.opal;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.sql.DataSource;

public abstract class TransactionalOpal<U extends UserFacing/*<U>*/> extends AbstractTransactionAware implements Opal<U> { // OPALFIXME

	//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(Opal.class.getName());
	
	/* THINK:  What, if anything, should be transient? */
	
	/* FEATURE: This should be transient or removed in favor of a lookup to a list of physical stores. */
	
	private final OpalFactory<U, ? extends TransactionalOpal<U>> myAbstractOpalFactory;
	
	/* This is "basically final":  Once it is set, it should never be changed. */
	/* final */ private U myUserFacing; /* FIXME: Is there a way to make this actually final? */

	/*
	 * This ctor should only be used to construct the NOT_YET_LOADED placeholders.
	 */	 
	protected TransactionalOpal() {
		super();
		myAbstractOpalFactory = null;
	}
	
	protected <O extends TransactionalOpal<U>> TransactionalOpal(OpalFactory<U, O> argOpalFactory) {
		super();
		
		myAbstractOpalFactory = Objects.requireNonNull(argOpalFactory);
		
		initializeReferences();
	}
	
	protected void initializeReferences() {
		return;
	}
	

	@Override
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

	@Override
	public abstract void output(PrintStream argPS) throws IOException;
	
	@Override
	public abstract void output(PrintWriter argPW) throws IOException;	
}
