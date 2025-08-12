package com.opal;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import static com.opal.CommitStep.*;

/* TODO: Figure out the proper synchronization strategy for this class */

public final class TransactionManager {
	/* package */ static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(TransactionManager.class.getName());
	
	private static final TransactionManager ourInstance = new TransactionManager();

//	private final LinkedList<TransactionContext> myTransactionContexts = new LinkedList<>();	
	private final PriorityBlockingQueue<TransactionContext> myTransactionContexts = new PriorityBlockingQueue<>(11, TransactionContext.TIMEOUT_ID_COMPARATOR); // What should the initial capacity be?
	
	private boolean myActive = true;
	
	private final TransactionReaperThread myTransactionReaperThread = new TransactionReaperThread();
	
	{
		myTransactionReaperThread.start();
	}
	
	protected static final long DEFAULT_SLEEP_TIME = TransactionContext.DEFAULT_TIME_OUT;
	protected static final long MINIMUM_SLEEP_TIME = 1000L; // in milliseconds
	
	private class TransactionReaperThread extends Thread {
		
		private TransactionReaperThread() {
			super("TransactionReaperThread");
			ourLogger.info("TransactionReaperThread starting up.");
			setDaemon(true); // THINK: Is this right?  Or should we register for shutdown so we can roll back our TC?
		}
		
		@Override
		public void run() {
//			LinkedList<TransactionContext> lclLL = getTransactionContexts();
			
			long lclSleepTime;
			
			while (true) {
				try {
//					synchronized (lclLL) {
						lclSleepTime = reapThrough(System.currentTimeMillis());
//					}
					
					if (lclSleepTime < MINIMUM_SLEEP_TIME) {
						lclSleepTime = MINIMUM_SLEEP_TIME;
					}
					
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug("TransactionReaperThread: Sleeping for " + lclSleepTime);
					}
					
					try {
						Thread.sleep(lclSleepTime);
					} catch (InterruptedException lclE) {
						if (ourLogger.isDebugEnabled()) {
							ourLogger.debug("TransactionReaperThread:  Interrupted while sleeping.");
						}
						break;
					}
					
//					ourLogger.debug("Reaper woke up.");
					
				} catch (Exception lclE) {
					ourLogger.error("TransactionReaperThread: Squashing exception:");
					ourLogger.error(lclE.toString(), lclE);
				}
			}
			
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("TransactionReaperThread: Starting final reap.");
			}
//			synchronized (lclLL) {
				reapThrough(Long.MAX_VALUE); // Long.MAX_VALUE = end of time
//			}
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("TransactionReaperThread: Completed final reap.");
			}
		}
		
		private long reapThrough(long argTime) {
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("TransactionReaperThread: Reaping through " + argTime);
			}
			/*  Must be synchronized on LL before you call this */
			Queue<TransactionContext> lclTCs = getTransactionContexts();
			TransactionContext lclTC;
			while ((lclTC = lclTCs.poll()) != null) {
				long lclDifference = lclTC.getTimeOut() - argTime;
				if (lclDifference > 0L) {
					if (lclTCs.offer(lclTC) == false) { // Put it back in the queue.
						ourLogger.error("TransactionReaperThread: offer() returned false when returning TransactionContext " + lclTC + " to the reaping queue.");
					}
					if (lclDifference > DEFAULT_SLEEP_TIME) {
						if (ourLogger.isDebugEnabled()) {
							ourLogger.debug("TransactionReaperThread: Next timeout is " + lclDifference + "; sleeping for default.");
						}
						return DEFAULT_SLEEP_TIME;
					} else {
						if (ourLogger.isDebugEnabled()) {
							ourLogger.debug("TransactionReaperThread: Next timeout is " + lclDifference + "; sleeping 100ms more than that (or the minimum).");
						}
						return lclDifference + 100L; // FIXME: Magic number
					}
				}
				lockThenReap(lclTC, false);
			}
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("TransactionReaperThread: Queue is empty; sleeping for default.");
			}
			return DEFAULT_SLEEP_TIME;
		}
		
		private static final int REAP_ATTEMPT_COUNT = 3;
		
		private void lockThenReap(TransactionContext argTC, boolean argAlreadyInterrupted) {
			if (argTC == null) {
				ourLogger.warn("argTC is null in reap.");
				return;
			}
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("TransactionReaperThread: Reaping TransactionContext " + argTC + ".  argAlreadyInterrupted = " + argAlreadyInterrupted);
			}
			// FIXME: Verify that it should be reaped.
			TransactionContext lclTC = argTC;
			boolean lclReapCalled = false;
			int lclI = 0;
			while ((lclReapCalled == false) && lclI < REAP_ATTEMPT_COUNT) {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("TransactionReaperThread: Starting lock attempt #" + lclI + " (of " + REAP_ATTEMPT_COUNT + ") on " + argTC + ".");
				}
				boolean lclReapCalledThisAttempt;
				try {
					/* FIXME: The need for this next if test (because this method might ultimately invoke itself via co-routine reapWithLock
					 * and end up unlocking a lock that was already unlocked by its child invocation's finally{} clause), makes me think
					 * that I have designed these routines badly.  Probably this can be cleaned up.
					 */
					if (argAlreadyInterrupted == false) {
						if (lclTC.getLock().tryLock(5, TimeUnit.SECONDS)) { // FIXME: Magic number
							lclReapCalledThisAttempt = true;
							try {
								reapWithLock(lclTC, argAlreadyInterrupted);
							} finally {
								lclTC.getLock().unlock();
							}
						} else {
							lclReapCalledThisAttempt = false;
						}
					} else {
						if (lclTC.getLock().tryLock(5, TimeUnit.SECONDS)) { // FIXME: Magic number
							lclReapCalledThisAttempt = true;
							reapWithLock(lclTC, argAlreadyInterrupted);
						} else {
							lclReapCalledThisAttempt = false;
						}
					}
				} catch (InterruptedException lclE) {
					ourLogger.error("TransactionReaperThread: Interrupted while trying to lock " + lclTC + " to reap it.");
					lclReapCalledThisAttempt = false;
				}
				if (lclReapCalledThisAttempt == false) {
					ourLogger.warn("TransactionReaperThread: Could not acquire lock on " + lclTC + " during attempt #" + lclI + " to properly reap it.");
				}
				lclReapCalled |= lclReapCalledThisAttempt;
				++lclI;
			}
			if (lclReapCalled == false) {
				ourLogger.error("TransactionReaperThread: Could not acquire lock on " + lclTC + " to properly reap it.  We have removed it from the TransactionContext queue and are abandoning it to its fate.");
				// FIXME: Maybe add this to some list?				
			}
		}
		
		private void reapWithLock(TransactionContext argTC, boolean argAlreadyInterrupted) {
			ourLogger.debug("TransactionReaperThread: Locked " + argTC + " for reaping.  argAlreadyInterrupted = " + argAlreadyInterrupted);
			Validate.isTrue(argTC.getLock().isHeldByCurrentThread());
			CommitStep lclCS = argTC.getCommitStep();
			// FIXME: This if cascade should probably be replaced by a switch statement.
			if (lclCS == NOT_CURRENTLY_COMMITTING) {
				ourLogger.error("TransactionReaperThread: Reaper is timing out " + argTC + ".  argAlreadyInterrupted = " + argAlreadyInterrupted);
				try {
					Thread lclThread = argTC.getThread();
					if (lclThread != null && (argAlreadyInterrupted == false)) {
						if (ourLogger.isDebugEnabled()) {
							ourLogger.debug("Interrupting " + lclThread + " for " + argTC);
						}
						/* We release the lock so the Thread can work on itself. */
						argTC.getLock().unlock(); // THINK: Will this ever fail?
						lclThread.interrupt();
						
						/* Pause to see if the thread that we have interrupted can commit or roll itself back. */
						Thread.sleep(5000L); // FIXME: Magic number
						
						lockThenReap(argTC, true);
					}
					Validate.isTrue(argTC.getLock().isHeldByCurrentThread());
										
					lclCS = argTC.getCommitStep();
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug("TransactionReaperThread: After the interruption block, the commit step of " + argTC + " is " + lclCS + ".  argAlreadyInterrupted == " + argAlreadyInterrupted);
					}
					
					/* Did it manage to roll itself back or commit? */
					
					if (lclCS != ROLLED_BACK && lclCS != COMMITTED) {
						/* No.  So we'll roll it back. */
						ourLogger.debug("TransactionReaperThread: We are rolling back " + argTC + ".");
						argTC.rollback();
					} else {
						/* Yes.  Good. */
					}
					
					lclCS = argTC.getCommitStep();
					
					/* Did either we or it manage to roll it back? */
					if (lclCS != ROLLED_BACK && lclCS != COMMITTED) {
						/* No.  That is an error; not sure what else we can do.  We may need
						 * to look into forcibly detaching the opals that had been associated
						 * with the transaction. */
						ourLogger.error("TransactionReaperThread: At the end of reapWithLock, " + argTC + " has commit step " + lclCS + ", but it should be either COMMITTED or ROLLED_BACK.");
					} else {
						/* Yes.  Good. */
						if (ourLogger.isDebugEnabled()) {
							ourLogger.debug("TransactionReaperThread: At the end of reapWithLock, " + argTC + " has the appropriate commit step " + lclCS + ".");
						}
					}
				} catch (Exception lclE) {
					ourLogger.error("Exception squashed while reaper was timing out (rolling back) " + argTC + ":" + lclE.toString(), lclE);
				}
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("TransactionReaperThread: Rolling back " + argTC + " is completed");
				}
			} else if (lclCS == ROLLED_BACK || lclCS == COMMITTED) {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("TransactionReaperThread: Reaper is removing " + argTC + " with final commit step " + lclCS + ".");
				}
			} else if (lclCS == ROLLING_BACK || lclCS == STARTED_PHASE_ONE || lclCS == DOING_PHASE_ONE || lclCS == ENDED_PHASE_ONE || lclCS == STARTED_PHASE_TWO || lclCS == DOING_PHASE_TWO || lclCS == ENDED_PHASE_TWO) { 
				ourLogger.warn("Reaper locked " + argTC + " which is in step " + lclCS + ", but it shouldn't be able to get to that step without having the lock itself.");
			} else {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Reaper is abandoning over " + argTC + " which is in step " + lclCS);
				}
			}
		}
	}
	
	protected TransactionManager() {
		super();
	}
	
	protected void addTransactionContext(TransactionContext argTC) {
		Validate.notNull(argTC);
		
		if (!isActive()) {
			throw new IllegalStateException("Tried to add a new TransactionContext after the TransactionManager has been shut down.");
		}
		
		getTransactionContexts().add(argTC);
		
		/* THINK:  This will not work if TCs are not added in strictly increasing order of time-out times.
		 * 
		 * With the new (2021) change to a PriorityBlockiongQueue, the above comment is incorrect (because things
		 * will work).
		 */
		
		/* THINK:  Consider interrupting the ReaperThread if this new one is going to need to be reaped
		 * before it wakes up.  Perhaps the Reaper should wait on itself.
		 * 
		 * This may still (as of 2021) get things reaped faster, but it shouldn't affect code correctness.
		 */
		
//		synchronized (lclLL) {
//			lclLL.add(argTC);
//		}
	}
	
	public TransactionContext getTransactionContext(long argID) {
		TransactionContext[] lclTCs = getTransactionContexts().toArray(new TransactionContext[0]);
		
		for (TransactionContext lclTC : lclTCs) {
			if (lclTC.getID() == argID) {
				return lclTC;
			}
		}
		return null;
	}
	
	public static final TransactionManager getInstance() {
		return ourInstance;
	}
	
	protected TransactionReaperThread getReaperThread() {
		return myTransactionReaperThread;
	}
	
	protected Queue<TransactionContext> getTransactionContexts() { // THINK: Does this need to be a PriorityBlockingQueue?  BlockingQueue?  Apparently not (which surprises me).
		return myTransactionContexts;
	}
	
	protected synchronized boolean isActive() {
		return myActive;
	}
	
	protected synchronized void setActive(boolean argActive) {
		myActive = argActive;
	}
	
	public /* synchronized */ void shutdown() {
		setActive(false);
		getReaperThread().interrupt();
	}
	
	/* This is to be used for debugging/diagnostic purposes only.  It is not synchronized (and it doesn't synchronize on the List of 
	 * TransactionContexts--so it can be executed even when the general TransactionContext system is locked up.
	 */
	public List<Object[]> getDebugInformation() {
		return getTransactionContexts().stream()
				.map(TransactionContext::getDebugInformation)
				.collect(Collectors.toList());
	}
}
