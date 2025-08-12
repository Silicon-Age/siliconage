package com.opal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

import static com.opal.CommitStep.*;

public abstract class AbstractTransactionAware implements TransactionAware /* implements Serializable */ {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(AbstractTransactionAware.class.getName());
	
	private static final int TRY_MUTATE_TIMEOUT = 60 * 1000; // 60 seconds
	
	private TransactionContext myTransactionContext;
	private CommitStep myCommitStep = NOT_CURRENTLY_COMMITTING;
	
	protected AbstractTransactionAware() {
		super();
	}
	
	/* THINK: Do we know for sure that we will already be synchronized on this object before commitPhaseOne is called? */
	@Override
	public final synchronized void commitPhaseOne(Map<DataSource, TransactionParameter> argTPMap) throws PersistenceException {
		Validate.notNull(argTPMap);
//		assert argTPMap != null;
		
		ensureCommitStep(STARTED_PHASE_ONE);
		setCommitStep(DOING_PHASE_ONE);
		
		// FEATURE: Cache the lclTP
		
		commitPhaseOneInternal(extractTransactionParameter(argTPMap));
		
		setCommitStep(ENDED_PHASE_ONE);
	}
	
	protected abstract /* synchronized */ void commitPhaseOneInternal(TransactionParameter argTP) throws PersistenceException;
	
	protected abstract TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap);

	/* THINK: Do we know for sure that we will already be synchronized on this object before commitPhaseTwo is called? */
	@Override
	public final synchronized void commitPhaseTwo(Map<DataSource, TransactionParameter> argTPMap) throws PersistenceException {
		Validate.notNull(argTPMap);
		
//		ourLogger.debug("commitPhaseTwo for " + defaultToString());
		
		ensureCommitStep(STARTED_PHASE_TWO);
		setCommitStep(DOING_PHASE_TWO);
		
		commitPhaseTwoInternal(extractTransactionParameter(argTPMap));
		
		setCommitStep(ENDED_PHASE_TWO);
	}
	
	/* This will almost never have to do anything, so we implement it. */
	protected /* synchronized */ void commitPhaseTwoInternal(@SuppressWarnings("unused") TransactionParameter argTP) throws PersistenceException {
		return;
	}
	
	@Override
	public /* synchronized */ final void ensureCommitStep(CommitStep argCommitStep) {
		if (getCommitStep() != argCommitStep) {
			throw new IllegalStateException(defaultToString() + " was asked to ensure that it had CommitStep " + argCommitStep + " but it had " + getCommitStep());
		}
		return;
	}
	
	@Override
	public final CommitStep getCommitStep() {
		return myCommitStep;
	}
	
	@Override
	public final TransactionContext getTransactionContext() {
		return myTransactionContext;
	}
	
	/* Must be synchronized when you call this */
	protected void ensureMonitor() {
		if (!Thread.holdsLock(this)) {
			throw new IllegalStateException("ensureLock() was called on " + defaultToString() + " when the thread did not already hold the lock on the object.");
		}
		return;
	}
	
	protected final /* synchronized */ void joinActiveTransactionContext() {
		ensureMonitor();
		TransactionContext lclTC = TransactionContext.getActive();
		if (lclTC == null) {
			throw new IllegalStateException("Cannot joinActiveTransactionContext because there is no active TransactionContext.");
		}
		joinTransactionContext(lclTC);
	}

	protected final synchronized void joinTransactionContext(TransactionContext argTC) {
		Validate.notNull(argTC);
		
		TransactionContext lclOldTC = getTransactionContext();
		if (lclOldTC == argTC) {
			return;
		}
		
		if (lclOldTC != null) {
			throw new IllegalStateException("Cannot change TransactionContext for an Opal after it is set.");
		}
		
		joinTransactionContextInternal();
		
		/* We have to do this first or various debug statements won't be able to figure out
		 * that they should use the new values rather than the old values. */
		setTransactionContext(argTC);
		
//		ourLogger.debug("Adding " + defaultToString() + " to " + argTC);
		try {
			argTC.add(this);
		} catch (RuntimeException lclE) {
			ourLogger.error("Could not add " + defaultToString() + " to " + argTC, lclE);
			setTransactionContext(null);
			throw lclE;
		}
		
//		ourLogger.debug(defaultToString() + " is joining " + argTC);
		
		return;
	}
	
	protected abstract void joinTransactionContextInternal();
	
	@Override
	public final synchronized void leaveTransactionContext() {
		TransactionContext lclTC = getTransactionContext();
		if (lclTC == null) {
			throw new IllegalStateException(defaultToString() + " cannot leaveTransactionContext() because it does not belong to one.");
		}
		
//		ourLogger.debug(defaultToString() + " is leaving " + lclTC);
//		ensureCommitStep(TransactionContext.ENDED_PHASE_TWO);
		
		setTransactionContext(null);
		setCommitStep(NOT_CURRENTLY_COMMITTING);
		
		/* This Opal is now free for other TransactionContexts to make use of it. */
		
//		ourLogger.debug("Notifying threads that " + defaultToString() + " is free.");
		
		this.notifyAll(); 
	}
	
	@Override
	public final synchronized void rollback() {
		if (getTransactionContext() != null) {
			try {
				rollbackInternal();
			} finally {
				leaveTransactionContext();
			}
		}
	}
	
	protected abstract void rollbackInternal();
	
	@Override
	public final void setCommitStep(CommitStep argCommitStep) {
//		ourLogger.debug("Setting CommitStep for " + defaultToString() + " from " + myCommitStep + " to " + argCommitStep);
		myCommitStep = argCommitStep;
		return;
	}
	
	@Override
	public final /* synchronized */ void setTransactionContext(TransactionContext argTransactionContext) {
		ensureMonitor();
		myTransactionContext = argTransactionContext;
	}
	
	protected final /* synchronized */ boolean tryAccess() {
		ensureMonitor();
		TransactionContext lclOpalTC = getTransactionContext();
		if (lclOpalTC == null) {
			return false;
		}
		
		TransactionContext lclThreadTC = TransactionContext.getActive();
		
		if (lclThreadTC == null) {
			return false;
		}
		
		if (lclOpalTC == lclThreadTC) {
			/* THINK: Does it make (logical/performance) sense to check whether the TransactionContext has been rolled back? */
			return true;
		} else {
			return false; /* We might want to pause here if we can't read the old values */
		}
	}
	
	protected synchronized final void tryMutate() { // THINK: Should this require that you already hold the monitor?
		TransactionContext lclThreadTC = TransactionContext.getActive();
		
//		ourLogger.debug("Trying to mutate " + defaultToString() + " in TC " + lclThreadTC + " with OpalTC = " + getTransactionContext());
		
		if (lclThreadTC == null) {
			throw new IllegalStateException("Modifications of Opals must be done within a TransactionContext.");
		}
		
		while (true) {
			TransactionContext lclOpalTC = getTransactionContext();
			
			if (lclOpalTC == null) {
				joinTransactionContext(lclThreadTC);
				return;
			} else if (lclOpalTC == lclThreadTC) {
				return;
			} else {
				try {
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug(lclThreadTC + " is waiting on " + defaultToString() + ", which is held by " + lclOpalTC);
					}
					wait(TRY_MUTATE_TIMEOUT);
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug(lclThreadTC + " has woken up from waiting on " + defaultToString());
					}
					if (lclThreadTC.getLock().tryLock(5, TimeUnit.SECONDS)) {
						try {
							CommitStep lclCS = lclThreadTC.getCommitStep();
							if (lclCS != CommitStep.NOT_CURRENTLY_COMMITTING) {
								if (ourLogger.isDebugEnabled()) {
									ourLogger.debug(lclThreadTC + " has found that its commit step is now " + lclCS + ".");
								}
								throw new RuntimeException("After waking up from waiting on " + defaultToString() + ", " + lclThreadTC + " found that its commit step had changed to " + lclCS + "."); 
							}
						} finally {
							lclThreadTC.getLock().unlock();
						}						
					} else {
						if (ourLogger.isDebugEnabled()) {
							ourLogger.debug("Unable to acquire the lock of " + lclThreadTC + " to check its commit step while trying to lock " + defaultToString() + ".");
						}						
					}
				} catch (InterruptedException lclE) {
					throw new RuntimeException("Thread interrupted while waiting to acquire lock on " + defaultToString(), lclE);
				}
			}
		}
	}
	
//	protected synchronized final void tryMutate() { // THINK: Should this require that you already hold the monitor?
//		TransactionContext lclThreadTC = TransactionContext.getActive();
//		
////		ourLogger.debug("Trying to mutate " + defaultToString() + " in TC " + lclThreadTC + " with OpalTC = " + getTransactionContext());
//		
//		if (lclThreadTC == null) {
//			throw new IllegalStateException("Modifications of Opals must be done within a TransactionContext.");
//		}
//		
//		while (true) {
//			TransactionContext lclOpalTC = getTransactionContext();
//			
//			if (lclOpalTC == null) {
//				joinTransactionContext(lclThreadTC);
//				return;
//			} else if (lclOpalTC == lclThreadTC) {
//				return;
//			} else {
//				try {
//					if (ourLogger.isDebugEnabled()) {
//						ourLogger.debug(lclThreadTC + " is waiting on " + defaultToString() + ", which is held by " + lclOpalTC);
//					}
//					wait(TRY_MUTATE_TIMEOUT);
//					if (ourLogger.isDebugEnabled()) {
//						ourLogger.debug(lclThreadTC + " has woken up from waiting on " + defaultToString());
//					}
//					CommitStep lclCS = lclThreadTC.getCommitStep();
//					if (lclCS != CommitStep.NOT_CURRENTLY_COMMITTING) {
//						if (ourLogger.isDebugEnabled()) {
//							ourLogger.debug(lclThreadTC + " has found that its commit step is now " + lclCS + ".");
//						}
//						throw new RuntimeException("After waking up from waiting on " + defaultToString() + ", " + lclThreadTC + " found that its commit step had changed to " + lclCS + "."); 
//					}
//				} catch (InterruptedException lclE) {
//					throw new RuntimeException("Thread interrupted while waiting to acquire lock on " + defaultToString(), lclE);
//				}
//			}
//		}
//	}
	
	@Override
	public Set<TransactionAware> getRequiredPriorCommits() {
		return Collections.emptySet();
	}
	
	@Override
	public Set<TransactionAware> getRequiredSubsequentCommits() {
		return Collections.emptySet();
	}
	
	public String defaultToString() {
		return getClass().getName() + '@' + System.identityHashCode(this);
	}

}
