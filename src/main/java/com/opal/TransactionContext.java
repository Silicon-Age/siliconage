package com.opal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.siliconage.util.Fast3Set;

import static com.opal.CommitStep.COMMITTED;
import static com.opal.CommitStep.ENDED_PHASE_ONE;
import static com.opal.CommitStep.ENDED_PHASE_TWO;
import static com.opal.CommitStep.NOT_CURRENTLY_COMMITTING;
import static com.opal.CommitStep.ROLLED_BACK;
import static com.opal.CommitStep.ROLLING_BACK;
import static com.opal.CommitStep.STARTED_PHASE_ONE;
import static com.opal.CommitStep.STARTED_PHASE_TWO;

public class TransactionContext implements AutoCloseable {
	private static final Logger ourLogger = LoggerFactory.getLogger(TransactionContext.class.getName());

	private static final ThreadLocal<TransactionContext> ourActiveTransactionContexts = new ThreadLocal<>();
	
	/* THINK: Lots of synchronization to clean up in the static methods */
	
	public static final long DEFAULT_TIME_OUT = 30L * 1000L; /* Thirty seconds */
	public static final Level DEFAULT_LOGGING_LEVEL = Level.DEBUG;

	private static volatile long ourIdCounter = 0;

	public static final Comparator<TransactionContext> TIMEOUT_ID_COMPARATOR = Comparator.comparing(TransactionContext::getTimeOut)
			.thenComparing(TransactionContext::getID); // Basically, as a tiebreaker.
	
	private final long myTimeOut;
	private final long myId;	
	private final Level myLoggingLevel;

	private final ReentrantLock myLock = new ReentrantLock(); // not a fair lock, as we probably wouldn't benefit from its guarantees
	
	private Thread myThread;	
	private CommitStep myCommitStep = NOT_CURRENTLY_COMMITTING;
	
	private final ArrayList<TransactionAware> myItems = new ArrayList<>();
	private final ArrayList<Runnable> mySuccessfulCommitActions = new ArrayList<>();
	private final ArrayList<Runnable> myFailureActions = new ArrayList<>();
		
	private int myStartCount;
	
	protected TransactionContext(Level argLoggingLevel, long argTimeOut) {
		super();
		
		Validate.isTrue(argTimeOut >= 1L, "argTimeOut must be positive");
		
		// We shouldn't have to acquire the lock in the ctor as nobody else can have a reference to this TransactionContext.
		
		myId = nextId(); // THINK: Check on the synchronization here
		myTimeOut = System.currentTimeMillis() + argTimeOut;
		myStartCount = 1;
		
		myLoggingLevel = argLoggingLevel;
		
		TransactionManager.getInstance().addTransactionContext(this);
	}
		
	protected ReentrantLock getLock() {
		return myLock;
	}
	
	protected boolean lockOrThrow() {
		boolean lclSuccess;
		try {
			lclSuccess = getLock().tryLock(5, TimeUnit.SECONDS);
		} catch (InterruptedException lclE) {
			ourLogger.error("*** Thread interrupted while trying to lock " + this + " ***");
			lclSuccess = false;
		}
		if (lclSuccess) {
			return true;
		} else {
			throw new IllegalStateException("Unable to lock " + this);
		}
	}
	
	@SuppressWarnings("resource")
	public void add(TransactionAware argItem) {
		Validate.notNull(argItem);
		if (lockOrThrow()) {
			try {
				if (getCommitStep() != NOT_CURRENTLY_COMMITTING) {
					throw new IllegalStateException("Tried to add " + argItem + " to " + this + " when it was in state " + getCommitStep() + " (must be NOT_CURRENTLY_COMMITTING)");
				}
				
				if (argItem.getTransactionContext() != this) {
					throw new IllegalStateException("Cannot assign " + argItem + " to " + this + "; it is already assigned to " + argItem.getTransactionContext());
				}
				
				List<TransactionAware> lclL = getItems();
				
				// This check can eventually be removed.
				if (!lclL.contains(argItem)) {
					lclL.add(argItem);
				} else {
					ourLogger.error("*** " + argItem + " was already in " + this + " ***");
				}
			} finally {
				getLock().unlock();
			}
//		} else {
//			ourLogger.error("*** Unable to add " + argItem + " to " + this + " within timeout period ***");
		}			
	}
	
	public void uponSuccessfulCommit(Runnable argAction) {
		Validate.notNull(argAction);
		if (lockOrThrow()) {
			try {
				if (getCommitStep() != NOT_CURRENTLY_COMMITTING) {
					throw new IllegalStateException("Tried to add " + argAction + " to " + this + " when it was in state " + getCommitStep() + " (must be NOT_CURRENTLY_COMMITTING)");
				}
				List<Runnable> lclL = getSuccessfulCommitActions();
				
				// This check can eventually be removed.
				if (!lclL.contains(argAction)) {
					lclL.add(argAction);
				} else {
					ourLogger.error("*** " + argAction + " was already scheduled for running upon successful commit of " + this + " ***");
				}
			} finally {
				getLock().unlock();
			}
//		} else {
//			ourLogger.error("*** Unable to add Runnable to the list of successful-commit actions for " + this + " within timeout period ***");
		}
	}
	
	public void uponFailure(Runnable argAction) {
		Validate.notNull(argAction);
		if (lockOrThrow()) {
			try {
				if (getCommitStep() != NOT_CURRENTLY_COMMITTING) {
					throw new IllegalStateException("Tried to add " + argAction + " to " + this + " when it was in state " + getCommitStep() + " (must be NOT_CURRENTLY_COMMITTING)");
				}
				
				List<Runnable> lclL = getFailureActions();
				
				// This check can eventually be removed.
				if (!lclL.contains(argAction)) {
					lclL.add(argAction);
				} else {
					ourLogger.error("*** " + argAction + " was already scheduled for running upon failure of " + this + " ***");
				}
			} finally {
				getLock().unlock();
			}
//		} else {
//			ourLogger.error("*** Unable to add Runnable to the list of failed-commit actions for " + this + " within timeout period ***");
		}
	}
	
	// FIXME: Wo calls this, and are they required to be synchronized/have the lock?	
	protected TransactionAware[] arrangeOpals(ArrayList<TransactionAware> argTAs) {
		Validate.notNull(argTAs, "Null TransactionAwares");
		
		Map<TransactionAware, Set<TransactionAware>> lclLinks = new IdentityHashMap<>((int) (argTAs.size() / 0.75)); // We really need a Multimap here, but Guava isn't a dependency
		
		final boolean lclLogging = ourLogger.isEnabledForLevel(getLoggingLevel());
		
		for (TransactionAware lclTA : argTAs) {
			Validate.notNull(lclTA, "Null TransactionAware");
			Validate.isTrue(lclLinks.containsKey(lclTA) == false, "Duplicate TransactionAware");
			Set<TransactionAware> lclRequiredPriorCommits = lclTA.getRequiredPriorCommits();
			if (lclRequiredPriorCommits == null) {
				throw new IllegalStateException("Null RequiredPriorCommits for " + lclTA);
			}
			if (lclLogging) {
				ourLogger.atLevel(getLoggingLevel()).log(lclTA + " has " + lclRequiredPriorCommits.size() + " required prior commits.");
			}
			lclLinks.put(lclTA, lclRequiredPriorCommits);
		}
		
		for (TransactionAware lclTA : argTAs) {
			Set<TransactionAware> lclAfters = lclTA.getRequiredSubsequentCommits();
			if (lclAfters == null) {
				throw new IllegalStateException("Null subsequent commits for " + lclTA);
			}
			if (lclLogging) {
				ourLogger.atLevel(getLoggingLevel()).log(lclTA + " has " + lclAfters.size() + " required subsequent commits.");
			}
			for (TransactionAware lclTA2 : lclAfters) {
				Set<TransactionAware> lclTAs = lclLinks.get(lclTA2);
				if (lclTAs == null) {
					final String lclTA2Newness;
					if (lclTA2 instanceof UpdatableOpal) {
						UpdatableOpal<?> lclUO = (UpdatableOpal<?>) lclTA2;
						if (lclUO.isNew()) {
							if (lclUO.isDeleted()) {
								lclTA2Newness = " (N/D)";
							} else {
								lclTA2Newness = " (N/-)";
							}
						} else {
							if (lclUO.isDeleted()) {
								lclTA2Newness = " (-/D)";
							} else {
								lclTA2Newness = " (-/-)";
							}
						}
					} else {
						lclTA2Newness = " (-)";
					}
					final String lclTANewness;
					if (lclTA2 instanceof UpdatableOpal) {
						UpdatableOpal<?> lclUO = (UpdatableOpal<?>) lclTA2;
						if (lclUO.isNew()) {
							if (lclUO.isDeleted()) {
								lclTANewness = " (N/D)";
							} else {
								lclTANewness = " (N/-)";
							}
						} else {
							if (lclUO.isDeleted()) {
								lclTANewness = " (-/D)";
							} else {
								lclTANewness = " (-/-)";
							}
						}
					} else {
						lclTANewness = " (-)";
					}
					
					throw new IllegalStateException("Null Set of TransactionAwares returned for " + lclTA2 + lclTA2Newness + ", which is a subsequent commit for " + lclTA + lclTANewness + ".");
				}
				if (lclLogging) {
					ourLogger.atLevel(getLoggingLevel()).log("Of those required subsequent commits, " + lclTA2 + " has " + lclTAs.size() + " required prior commits.");
				}
				if (lclTAs.isEmpty()) {
					lclTAs = new Fast3Set<>();
					lclLinks.put(lclTA2, lclTAs);
				}
				lclTAs.add(lclTA);
				if (lclLogging) {
					ourLogger.atLevel(getLoggingLevel()).log(lclTA2 + " now has " + lclTAs.size() + " required prior commits.");
				}
			}
		}
		
		if (lclLogging) {
			for (Map.Entry<TransactionAware, Set<TransactionAware>> lclEntry : lclLinks.entrySet()) {
				ourLogger.atLevel(getLoggingLevel()).log(lclEntry.getKey() + " -> " + lclEntry.getValue().size());
			}
		}
		
		// FIXME: Why do we make this copy?
		TransactionAware[] lclTAs = argTAs.toArray(new TransactionAware[argTAs.size()]);
		int lclI = 0;
		
		TransactionAware[] lclProperlyOrderedArray = new TransactionAware[lclTAs.length];
		int lclPOI = 0;
		
		while (lclI < lclTAs.length) {
			TransactionAware lclTA = lclTAs[lclI];
			if (lclLogging) {
				ourLogger.atLevel(getLoggingLevel()).log("lclI = " + lclI + " lclTA = " + lclTA);
			}
			if (lclLinks.containsKey(lclTA) == false) {
				++lclI;
				continue;
			}
			Set<TransactionAware> lclS = lclLinks.get(lclTA);
			Validate.notNull(lclS);
			while (lclS != null && lclS.isEmpty() == false) {
				TransactionAware lclTA2 = null;
				Iterator<TransactionAware> lclTAI = lclS.iterator();
				while (lclTAI.hasNext()) {
					TransactionAware lclTA3 = lclTAI.next();
					if (lclLogging) {
						ourLogger.atLevel(getLoggingLevel()).log("Checking on " + lclTA3);
					}
					lclS = lclLinks.get(lclTA3);
					if (lclS == null) {
						continue;
					}
					lclTA2 = lclTA3;
					break;
				}
				if (lclTA2 != null) {
					if (lclLogging) {
						ourLogger.atLevel(getLoggingLevel()).log("lclTA2 is " + lclTA2 + " lclS.size = " + (lclS != null ? lclS.size() : -1));
					}
					lclTA = lclTA2;
					// lclS has already been set
				} else {
					lclS = null;
				}
			}
			if (lclLogging) {
				ourLogger.atLevel(getLoggingLevel()).log("Commit slot " + lclPOI + " is " + lclTA);
			}
			lclProperlyOrderedArray[lclPOI++] = lclTA;
			lclS = lclLinks.remove(lclTA);
			Validate.notNull(lclS);
		}

		if (lclLogging) {
			ourLogger.atLevel(getLoggingLevel()).log("Commit ordering:");
			for (lclI = 0; lclI < lclProperlyOrderedArray.length; ++lclI) {
				ourLogger.atLevel(getLoggingLevel()).log(lclI + ". " + lclProperlyOrderedArray[lclI]);
			}
		}
		
		return lclProperlyOrderedArray;
	}
	
	public void complete() {
		if (lockOrThrow()) {
			try {
				if (getCommitStep() != NOT_CURRENTLY_COMMITTING) {
					throw new IllegalStateException("Tried to complete() " + this + " but its commit step is " + getCommitStep() + "; it must have the commit step of " + NOT_CURRENTLY_COMMITTING + '.');
				}
				
				if (getStartCount() <= 0) {
					throw new IllegalStateException("Tried to complete() " + this + " but it has a start count of 0; that is, every operation that was started appears to already have been completed.");
				}
				
				ourLogger.atLevel(getLoggingLevel()).log("Completing " + this + "; start count = " + getStartCount());
				if (getStartCount() == 1) {
					commit();
				}
				/* QUIZ: Why don't we call decrementStartCount() here? */
			} finally {
				getLock().unlock();
			}
//		} else {
//			ourLogger.error("*** Unable to acquire the lock in complete() for " + this + " within timeout period ***");
		}
	}
	
	public void commit() {
		if (lockOrThrow()) {
			try {
				Validate.isTrue(getCommitStep() == NOT_CURRENTLY_COMMITTING);
				
				ourLogger.atLevel(getLoggingLevel()).log("Committing " + this);
				
				if (getItems().isEmpty()) {
					/* No opals have been changed as part of this transaction so there's nothing to do */
					setCommitStep(COMMITTED);
					if (getActive() == this) {
						setActive(null);
					}
				} else {
					/* We have actual work to do. */
					HashMap<DataSource, TransactionParameter> lclTPMap = null;
					
					try {
						try {
							setCommitStep(STARTED_PHASE_ONE);
							
							lclTPMap = new HashMap<>();
							
							/* FEATURE:  Must sort Opals so referential integrity is maintained */
							
							ArrayList<TransactionAware> lclList = getItems();
							
							TransactionAware[] lclItems = arrangeOpals(lclList);
							
							/* Sanity-check the list of Opals belonging to this Transaction */
							for (TransactionAware lclItem : lclItems) {
								Validate.notNull(lclItem);
								
								if (lclItem.getTransactionContext() != this) {
									throw new IllegalStateException("lclItem.getTransactionContext() != this");
								}
								lclItem.ensureCommitStep(NOT_CURRENTLY_COMMITTING);
							}
							
							for (TransactionAware lclItem : lclItems) {
								lclItem.setCommitStep(STARTED_PHASE_ONE);
							}
							
							/* Run the SQL for each object */
							for (TransactionAware lclItem : lclItems) {
								// TODO: These should not be asserts
								lclItem.ensureCommitStep(STARTED_PHASE_ONE);
								lclItem.commitPhaseOne(lclTPMap);
								lclItem.ensureCommitStep(ENDED_PHASE_ONE);
							}
							
							/* Execute a commit */
							for (TransactionParameter lclTP : lclTPMap.values()) {
								Validate.notNull(lclTP);
								lclTP.commitPhaseOne();
							}
							
							setCommitStep(ENDED_PHASE_ONE);
							
							setCommitStep(STARTED_PHASE_TWO);
							
							for (TransactionAware lclItem : lclItems) {
								lclItem.ensureCommitStep(ENDED_PHASE_ONE);
								lclItem.setCommitStep(STARTED_PHASE_TWO);
							}
							
							for (TransactionAware lclItem : lclItems) {
								lclItem.commitPhaseTwo(lclTPMap);
								lclItem.ensureCommitStep(ENDED_PHASE_TWO);
							}
							
							/* Will this ever do anything? */
							for (TransactionParameter lclTP : lclTPMap.values()) {
								lclTP.commitPhaseTwo();
							}
							
							setCommitStep(ENDED_PHASE_TWO);
							
							/* Everything is committed, the transaction has ended properly. */
						} catch (RuntimeException lclE) {
							throw new TransactionException("Caught exception while processing Transaction " + this + "; converting it to a TransactionException", lclE);
						}
					} catch (TransactionException lclE) {
						// setCommitStep(ROLLING_BACK);
						ourLogger.error("Exception caught while committing Transaction " + this + "; rolling it back.", lclE);
						
						if (lclTPMap != null) {
							for (TransactionParameter lclTP : lclTPMap.values()) {
								try {
									ourLogger.error("Rolling back TransactionParameter " + lclTP);
									lclTP.rollback(); /* TODO: Does this close the connection? */
								} catch (Exception lclF) {
									ourLogger.error("Squashing exception thrown while rolling back " + lclTP, lclF);
								}
							}
						}
						try {
							ourLogger.error("Rolling back " + this + '.');
							rollback();
						} catch (Exception lclF) {
							ourLogger.error("Squashing exception thrown while rolling back " + this, lclF);
						}
						
						if (getItems().size() != 0) {
							ourLogger.error("getItems().size() != 0");
							getItems().clear();
						}
						
						// setCommitStep(ROLLED_BACK);
						Validate.isTrue(getItems().isEmpty());
						
						/* THINK:  Is there anything else that we need to do to correct the state of the objects? */
						throw new RuntimeException("Could not commit transaction " + this, lclE);
					} finally {
						if (getActive() == this) {
							setActive(null);
						}
						
						if (lclTPMap != null) {
							for (TransactionParameter lclTP : lclTPMap.values()) {
								try {
									lclTP.close();
								} catch (Exception lclF) {
									ourLogger.error("Squashing exception thrown while closing TransactionParameter " + lclTP, lclF);
									/* THINK: Is there anything else that we need to do? */
								}
							}
							lclTPMap.clear();
							if (lclTPMap.size() != 0) {
								ourLogger.error("lclTPMap.size() != 0");
							}
						}
					}
					
					/* The transaction has been successfully committed; we now remove the Opals from the TransactionContext.
					 * No Exceptions should ever be thrown during this process. */
					try {
						for (TransactionAware lclItem : getItems()) {
							/* We have to synchronize here otherwise some other thread could pick up the
							Opal between the return from the leaveTransactionContext() call and the
							ensure call.  When we trust the system enough to remove the ensure call, we
							can drop the synchronization. */
							
							synchronized (lclItem) {
								lclItem.leaveTransactionContext();
								lclItem.ensureCommitStep(NOT_CURRENTLY_COMMITTING);
								if (lclItem.getTransactionContext() != null) {
									ourLogger.error("TransactionAware item " + lclItem + " has non-null TransactionContext");
								}
							}
						}
					} catch (Exception lclE) {
						ourLogger.error("Exception thrown while removing Opals from completed transaction " + this + '.', lclE);
					} finally {
						getItems().clear();
						Validate.isTrue(getItems().isEmpty());
						setCommitStep(COMMITTED);
					}
					
					ourLogger.atLevel(getLoggingLevel()).log("Finished committing " + this);
					
					if (getCommitStep() == COMMITTED) {
						for (Runnable lclR : getSuccessfulCommitActions()) {
							try {
								lclR.run();
							} catch (Exception lclE) {
								ourLogger.error("Exception thrown when running " + lclR.getClass().getName() + " following successful commit of " + this + '.', lclE);
							}
						}
					} else if (getCommitStep() == ROLLED_BACK) { // CHECK: I don't know if this will ever trigger if an Exception is thrown during commit.
						for (Runnable lclR : getFailureActions()) {
							try {
								lclR.run();
							} catch (Exception lclE) {
								ourLogger.error("Exception thrown when running " + lclR.getClass().getName() + " following successful commit of " + this + '.', lclE);
							}
						}
					}
				}
			} finally {
				getLock().unlock();
			}
//		} else {
//			ourLogger.error("*** Unable to acquire the lock in commit() for " + this + " within timeout period ***");
		}
		return;
	}
	
	private boolean holdsLock() {
		return getLock().isHeldByCurrentThread();
	}
	
	/* Ultimately, this method can be removed. */
	private void ensureLock() {
		if (holdsLock() == false) {
			throw new IllegalStateException("Do not have Lock!");
		}
	}
	
	protected Iterator<TransactionAware> iterator() {
		ensureLock(); // Ultimately, this call can be removed.
		return getItems().iterator();
	}

	protected CommitStep getCommitStep() {
		ensureLock(); // Ultimately, this call can be removed.
		return myCommitStep;
	}
	
	private final void setCommitStep(CommitStep argCommitStep) {
		ensureLock(); // Ultimately, this call can be removed.
		// ourLogger.atLevel(getLoggingLevel()).log("Changing CommitStep for " + this + " from " + myCommitStep + " to " + argCommitStep);
		myCommitStep = argCommitStep;
	}
	
	protected int getStartCount() {
//		ensureLock(); // Ultimately, this call can be removed. // There is some inconsistency here in the locked-ness of access to start count.
		return myStartCount;
	}
	
	protected int decrementStartCount() {
		ensureLock(); // Ultimately, this call can be removed.
		return --myStartCount;
	}
	
	protected int incrementStartCount() {
		ensureLock(); // Ultimately, this call can be removed.
		return ++myStartCount;
	}
	
	protected void zeroStartCount() {
		ensureLock(); // Ultimately, this call can be removed.
		myStartCount = 0;
	}
	
	/* myId is final, so we don't need the Lock to guarantee that we are seeing the proper data. */
	protected long getID() {
		return myId;
	}
	
	protected ArrayList<TransactionAware> getItems() {
		ensureLock(); // Ultimately, this call can be removed.
		return myItems;
	}
	
	protected ArrayList<Runnable> getSuccessfulCommitActions() {
		ensureLock(); // Ultimately, this call can be removed.
		return mySuccessfulCommitActions;
	}
	
	protected ArrayList<Runnable> getFailureActions() {
		ensureLock(); // Ultimately, this call can be removed.
		return myFailureActions;
	}

	/* myTimeOut is final, so we don't need the Lock to guarantee we are seeing the proper data. */
	/* package */ long getTimeOut() {
		return myTimeOut;
	}

	/* myLoggingLevel is final, so we don't need the Lock to guarantee we are seeing the proper data. */
	public Level getLoggingLevel() {
		return myLoggingLevel;
	}
	
	private static long nextId() {
		return ourIdCounter++;
	}
	
	protected Thread getThread() {
		ensureLock(); // Ultimately, this call can be removed.
		return myThread;
	}
	
	protected void setThread(Thread argThread) {
		ensureLock(); // Ultimately, this call can be removed.
		myThread = argThread;
	}
	
	public void rollback() {
		if (lockOrThrow()) {
			try {
				CommitStep lclCS = getCommitStep();
				Validate.isTrue(lclCS != COMMITTED);
				Validate.isTrue(lclCS != ROLLING_BACK);
				Validate.isTrue(lclCS != ROLLED_BACK);
				
				try {
					setCommitStep(ROLLING_BACK);
					ourLogger.atLevel(getLoggingLevel()).log("Rolling back " + this);
					
					Iterator<TransactionAware> lclI = getItems().iterator();
					
					while (lclI.hasNext()) {
						try {
							final TransactionAware lclItem = lclI.next();
							if (lclItem != null) {
								/* TODO: Should we check for a specific status on the Opal to make sure things are functioning properly? */ 
								
								// ourLogger.atLevel(getLoggingLevel()).log("Rolling back changes to " + lclO.toDebugString());
								
								synchronized (lclItem) {
									lclItem.rollback();
									lclItem.ensureCommitStep(NOT_CURRENTLY_COMMITTING);
									if (lclItem.getTransactionContext() != null) {
										ourLogger.error("TransactionAware item " + lclItem + " did not have a null TransactionContext.");
									}
								}
							}
						} catch (Exception lclE) {
							ourLogger.error("Squashing exception while rolling back an Opal", lclE);
						} finally {
							lclI.remove();
						}
					}
					
					if (getItems().isEmpty() == false) {
						ourLogger.error("When rolling back " + this + " we ended up with " + getItems().size() + " items.");
					}
					Validate.isTrue(getItems().isEmpty());
					
					setCommitStep(ROLLED_BACK);
				} finally {
					if (getActive() == this) {
						setActive(null);
					}
					// ourLogger.atLevel(getLoggingLevel()).log("In the finally block for rolling back " + this);
				}
			} finally {
				getLock().unlock();
			}
//		} else {
//			ourLogger.error("*** Unable to acquire the lock in rollback() for " + this + " within timeout period ***");
		}
	}
	
	@Override
	public String toString() {
		return "Transaction[" + getID() + "/" + getStartCount() + "]"; // Dangerous: runs without the Lock required for getStartCount.
	}
	
	@Override
	public void close() {
		if (lockOrThrow()) {
			try {
				CommitStep lclCS = getCommitStep();
				if (lclCS == COMMITTED || lclCS == ROLLED_BACK || lclCS == ROLLING_BACK) {
					zeroStartCount();
					if (getActive() == this) {
						setActive(null);
					}
					return;
				}
				ourLogger.atLevel(getLoggingLevel()).log("Closing " + this + "; start count = " + getStartCount());
				if (getStartCount() == 1) {
					rollback();
					zeroStartCount();
					Validate.isTrue(getCommitStep() == ROLLED_BACK);
				} else {
					decrementStartCount();
				}
			} finally {
				getLock().unlock();
			}
//		} else {
//			ourLogger.error("*** Unable to acquire the lock in close() for " + this + " within timeout period ***");
		}
	}
	
	/* THINK: Do we need rollbackIfCreator? */
	
	public static TransactionContext createAndActivate() {
		return createAndActivate(DEFAULT_LOGGING_LEVEL, DEFAULT_TIME_OUT);
	}
	
	public static TransactionContext createAndActivate(long argTimeOut) {
		return createAndActivate(DEFAULT_LOGGING_LEVEL, argTimeOut);
	}
	
	public static TransactionContext createAndActivate(Level argLoggingLevel) {
		return createAndActivate(argLoggingLevel, DEFAULT_TIME_OUT);
	}
	
	public static TransactionContext createAndActivate(Level argLoggingLevel, long argTimeOut) {
		return activate(create(argLoggingLevel, argTimeOut));
	}
	
	public static TransactionContext create() {
		return create(DEFAULT_LOGGING_LEVEL);
	}
	
	public static TransactionContext create(Level argLoggingLevel) {
		return create(argLoggingLevel, DEFAULT_TIME_OUT);
	}
	
	public static TransactionContext create(long argTimeOut) {
		return create(DEFAULT_LOGGING_LEVEL, argTimeOut);
	}
	
	public static TransactionContext create(Level argLoggingLevel, long argTimeOut) {
		return new TransactionContext(argLoggingLevel, argTimeOut);
	}

	public static TransactionContext joinActiveOrCreate() {
		return joinActiveOrCreate(DEFAULT_TIME_OUT);
	}
	
	public static TransactionContext joinActiveOrCreate(long argTimeOut) {
		TransactionContext lclTC = getActive();
		if (lclTC == null) {
			return createAndActivate(argTimeOut);
		} else {
			lclTC.lockOrThrow();
			try { // FIXME: Should we be checking the CommitStep here?
				lclTC.incrementStartCount();
				return lclTC;
			} finally {
				lclTC.getLock().unlock();
			}
//			} else {
//				throw new IllegalStateException("Unable to acquire the lock in joinActiveOrCreate() for existing TransactionContext " + lclTC + " within timeout period.");
//			}
		}
	}
	
	/* Static convenience methods for working with the Thread's active TransactionContext. */
	
	// FIXME: Verify synchronization/locking here.
	public static TransactionContext activate(TransactionContext argTC) {
		Validate.notNull(argTC);
		TransactionContext lclCurrentlyActive = getActive();
		if (lclCurrentlyActive != null) {
			throw new IllegalStateException("Cannot activate " + argTC + " because " + lclCurrentlyActive + " is already active.");
		}
		argTC.lockOrThrow();
		try {
			if (argTC.getCommitStep() != NOT_CURRENTLY_COMMITTING) {
				throw new IllegalStateException(argTC + " cannot be activated because it is not in the " + NOT_CURRENTLY_COMMITTING + " state.");
			}
			setActive(argTC);
		} finally {
			argTC.getLock().unlock();
		}
		
		return argTC;
	}

//	@Deprecated
//	public static void commitActive() {
//		TransactionContext lclTC = getActive();
//		Validate.notNull(lclTC);
//		
//		/* commit() now removes itself as the active TransactionContxt */
//		lclTC.commit();
//	}
	
	// FIXME: Verify synchronization/locking here.
	public static TransactionContext deactivateActive() {
		/* Make sure we're in one */
		TransactionContext lclTC = getActive();
		Validate.notNull(lclTC, "deactivateActive() called with no active TransactionContext");
		setActive(null);
		return lclTC;
	}
	
	// FIXME: Verify synchronization/locking here.
	public static void assertActive() {
		Validate.notNull(getActive(), "assertActive() called with no active TransactionContext.");
	}
	
	// FIXME: Verify synchronization/locking here.
	public static void assertNoActive() {
		TransactionContext lclTC = getActive();
		if (lclTC != null) {
			throw new IllegalStateException("assertNoActive() called with " + lclTC + " was active.");
		}
	}
	
	public static TransactionContext getActive() {
		return ourActiveTransactionContexts.get();
	}
	
	public static boolean hasActive() {
		return getActive() != null;
	}
	
	public static void rollbackAnyActive() {
		if (hasActive()) { // THINK:  Synchronize? 
			rollbackActive();
		}
	}
	
	private static void setActive(TransactionContext argTC) {
		// ourLogger.atLevel(getLoggingLevel()).log("Setting active TC to " + String.valueOf(argTC));
		
//		synchronized (ourActiveTransactionContexts) { // THINK: Does one really have to synchronize on a ThreadLocal?
			final TransactionContext lclTC = ourActiveTransactionContexts.get();
			
			if (lclTC != null) {
				if (lclTC.lockOrThrow()) {
					try {
						lclTC.setThread(null);
					} finally {
						lclTC.getLock().unlock();
					}
				} else { // FIXME: Since lockOrThrow never returns (normally) with false, this can never be reached.
					throw new IllegalStateException("Unable to acquire the lock in setActive() for previously active TransactionContext " + lclTC + " within timeout period.");
				}
			}
			
			ourActiveTransactionContexts.set(argTC);
			
			if (argTC != null) {
				if (argTC.lockOrThrow()) {
					try {
						argTC.setThread(Thread.currentThread());
					} finally {
						argTC.getLock().unlock();
					}
				} else { // FIXME: Since lockOrThrow never returns (normally) with false, this can never be reached.
					throw new IllegalStateException("Unable to acquire the lock in setActive() for newly active TransactionContext " + argTC + " within timeout period.");
				}
			}
//		}
		// ourLogger.atLevel(getLoggingLevel()).log("Done setting TC");
	}
	
	public static void rollbackActive() {
		TransactionContext lclTC = getActive();
		Validate.notNull(lclTC, "rollbackActive() called with no active TransactionContext.");
		
		lclTC.rollback();
	}
	
	/* This is to be used for debugging/diagnostic purposes only.  It is not synchronized so it can be executed even when 
	 * the general TransactionContext system is locked up.
	 */
	/* package */ Object[] getDebugInformation() {
		return new Object[] {
				myId,
				myTimeOut,
				myCommitStep,
				myItems.size(),
				mySuccessfulCommitActions.size(),
				myFailureActions.size(),
				(myThread != null) ? myThread.getName() : "NULL_NAME",
				myStartCount,
		};
	}
}
