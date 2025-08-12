package com.opal;

import java.util.Iterator;

import org.apache.commons.collections4.map.AbstractReferenceMap;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.commons.lang3.Validate;

public final class OpalCache<I extends IdentityOpal<? extends IdentityUserFacing>> {
	/* package */ static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalCache.class.getName());
	
	private final ReferenceMap<OpalKey<I>, I> myCache;

	public OpalCache() {
		this(true);
	}

	public OpalCache(boolean argSoftReferences) {
		super();
		
		myCache = new ReferenceMap<>( // THINK: Could this be a ReferenceIdentityMap?  Would it make a difference?
			AbstractReferenceMap.ReferenceStrength.HARD,
			argSoftReferences ? AbstractReferenceMap.ReferenceStrength.SOFT : AbstractReferenceMap.ReferenceStrength.WEAK
		);
	}
	
	private void ensureLock() {
		if (!Thread.holdsLock(this)) {
			throw new IllegalStateException("ensureLock() was called on " + this + " when the thread did not already hold the lock on the object.");
		}
		return;
	}
	
	/* One must already have a lock on this (i.e., the OpalCache for the factory) to call this. */
	public /* synchronized */ void addOpal(OpalKey<I> argOK, I argOpal, @SuppressWarnings("unused") boolean argLasting) {
//		System.out.println("Adding OpalKey " + argOK + " (" + argOK.hashCode() + ") for opal " + argOpal + " to cache " + this);
		ensureLock();
		
		Validate.notNull(argOK);
		Validate.notNull(argOpal);
		
		Object lclOldObject;
		
		if ((lclOldObject = getCache().put(argOK, argOpal)) != null) {
			throw new RuntimeException("Adding Opal " + argOpal + " (" + System.identityHashCode(argOpal) + ") for key " + argOK + " replaced Opal " + lclOldObject + " (" + System.identityHashCode(lclOldObject) + ")");
		}
//		ourLogger.debug("Cache now has " + getCache().size() + " elements.");
	}
	
	/* One must already have a lock on this (i.e., the OpalCache for the factory) to call this. */
	public /* synchronized */ I removeOpal(OpalKey<I> argOK) {
		ensureLock();
		Validate.notNull(argOK);
//		ourLogger.debug("Removing " + argOK + " (" + argOK.hashCode() + ") from " + this); 
		return getCache().remove(argOK);
	}
	
	/* One must already have a lock on this (i.e., the OpalCache for the factory) to call this. */
	public /* synchronized */ I forOpalKey(OpalKey<I> argOK) {
		ensureLock();
		Validate.notNull(argOK);
		
//		ourLogger.debug("Asked OpalCache " + this + " for " + argOK + " (" + argOK.hashCode() + ")");
		
		/* The following cast to (O) seems like it shouldn't be necessary, but I can't figure out how to declare the cache in such a way
		 * that the OpalKey's opal must match the Opal.
		 */
		
		I lclO = getCache().get(argOK);
		
//		getStatistics().tallyRequest();
		
		return lclO;
	}
	
	/**
	 * Used by the OpalCacheServlet
	 * @param argKey The key to look up
	 * @return The value for argKey
	 */
	
	/* This method intentionally does not require holding the lock.  This makes it fundamentally unsafe. */
	public I get(Object argKey) {
		return getCache().get(argKey);
	}
	
	private ReferenceMap<OpalKey<I>, I> getCache() {
		return myCache;
	}
	
	/**
	 * Used by the OpalCacheServlet
	 * @return An Iterator of all the OpalKeys in the OpalCache
	 */
	public synchronized Iterator<OpalKey<I>> keyIterator() {
		return getCache().keySet().iterator();
	}
	
	// THINK: Figure out how to make sure that there are no pending transactions
	// THINK: If you think you want to call this, you probably don't.  If you still do, check with RRH
	// so he can try to convince you that you really don't want to.
	public synchronized void clear() {
		getCache().clear();
		getStatistics().clear();
	}
	
	private CacheStatistics myCacheStatistics = new CacheStatistics();
	
	public CacheStatistics getStatistics() {
		return myCacheStatistics;
	}
	
	public int getSize() {
		return getCache().size();
	}
	
	public long getRealKeyCount() {
		return getCache().keySet().size();
	}

	public long getDistinctValueCount() {
		return getCache().values().stream().distinct().count();
	}

	public long getDataReadCount() {
		return getCache().values().stream().filter(IdentityOpal::isDataRead).distinct().count();
	}

	public static class CacheStatistics {
		private int myRequests = 0;
		private int myHits = 0;
		private int myExpired = 0;
		private int myMisses = 0;
		
		/* package */  CacheStatistics() {
			super();
		}
		
		public void clear() {
			myRequests = 0;
			myHits = 0;
			myExpired = 0;
			myMisses = 0;
		}
		
		public void tallyExpired() {
			myExpired++;
		}
		
		public void tallyMiss() {
			myMisses++;
		}
		
		public void tallyHit() {
			myHits++;
		}
		
		public void tallyRequest() {
			myRequests++;
		}
		
		public int getExpired() {
			return myExpired;
		}
		
		public int getHits() {
			return myHits;
		}
		
		public int getMisses() {
			return myMisses;
		}
		
		public int getRequests() {
			return myRequests;
		}
	}
	
//	public int[] getMemoryUsage() {
//		int lclKeys = 0;
//		int lclValues = 0;
//		int lclBlanked = 0;
//		for(Map.Entry<OpalKey<? extends Opal<? extends UserFacing>>, OpalReference<? extends Opal<? extends UserFacing>>> lclEntry : getCache().entrySet()) {
//			++lclKeys;
//			OpalReference<? extends Opal<? extends UserFacing>> lclR = lclEntry.getValue();
//			if (lclR != null) {
//				lclValues++;
//				if (lclR.get() != null) {
//					lclBlanked ++;
//				}
//			}
//		}
//		return new int[] { lclKeys, lclValues, lclBlanked, };
//	}
	
//	public Map<Class<?>, int[]> getObjects() {
//		HashMap<Class<?>, int[]> lclTally = new HashMap<Class<?>, int[]>();
//		for(Map.Entry<OpalKey<? extends Opal<? extends UserFacing>>, OpalReference<? extends Opal<? extends UserFacing>>> lclEntry : getCache().entrySet()) {
//			OpalReference<? extends Opal<? extends UserFacing>> lclR = lclEntry.getValue();
//			Class<?> lclC = lclEntry.getKey().getClass();
//			int[] lclCount = lclTally.get(lclC);
//			if (lclCount == null) {
//				lclTally.put(lclC, lclCount = new int[2]);
//			}
//			lclCount[0]++;
//			if (lclR != null) {
//				if (lclR.get() != null) {
//					lclCount[1]++;
//				}
//			}
//		}
//		return lclTally;
//	}
	
//	public void shutdown() {
//		return;
//	}
}
