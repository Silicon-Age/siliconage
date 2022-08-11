package com.siliconage.util;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Cache<K, V> implements Map<K, V> {
	private final HashMap<K, Reference<V>> myMap;
	
	protected final HashMap<K, Reference<V>> getMap() {
		return myMap;
	}
	
	public Cache() {
		super();
		myMap = new HashMap<>();
	}
	
	public Cache(int argSize) {
		super();
		myMap = new HashMap<>(argSize);
	}
	
	@Override
	public V put(K argKey, V argValue) {
		if (argKey == null) {
			throw new IllegalArgumentException("argKey is null");
		}
		if (argValue == null) {
			throw new IllegalArgumentException("argValue is null");
		}
		
//		Object lclOldObject;
		
		SoftReference<V> lclR = new SoftReference<>(argValue);
		
		Reference<V> lclReplaced = getMap().put(argKey, lclR);
		
		return lclReplaced == null ? null : lclReplaced.get();
		
//		if (( lclOldObject =  getMap().put(argKey, lclR)) != null) {
//			lclR = (Reference) lclOldObject;
//			if (lclR.get() != null) {
//				ourLogger.error("Adding Opal " + argOpal + " (" + System.identityHashCode(argOpal) + ") for key " + argOK + " replaced reference " + lclR + " with object " + lclR.get() + " (" + System.identityHashCode(lclR.get()) + ")", new Exception());
//			} else {
//				ourLogger.debug("addOpal for " + argOK + " replaced expired Reference (probably ok)");
//			}
//		}
	}
	
	@Override
	public V remove(Object argKey) {
		if (argKey == null) {
			throw new IllegalArgumentException("argKey is null");
		}
//		ourLogger.debug("Removing " + argOK + " (" + argOK.hashCode() + ") from " + this); 
		Reference<V> lclR = getMap().remove(argKey); // TODO:  Probably shouldn't be a reference
		if (lclR == null) {
			return null;
		} else {
			return lclR.get();
		}
	}
	
	/* Form to make sure that we are removing the one that we think we are */
	
	public void removeWithVerify(Object argKey, Object argValue) {
//		ourLogger.debug("Removing " + argOK + " (" + argOK.hashCode() + ") (expected to be " + argOpal + ") from " + this); 
		Object lclRemoved = remove(argKey);
		if (lclRemoved == null) {
			throw new IllegalStateException("Removing the key " + argKey + " did not find a Reference to " + argValue + " to remove from the cache.");
		} else {
			Reference<?> lclR = (Reference<?>) lclRemoved;
			Object lclO = lclR.get();
			if (lclO == null) {
				return; /* Who knows?  It has expired */
			}
			if (lclO == argValue) {
				return; /* Yes, we're fine. */
			} else {
				throw new IllegalStateException("Removing the key " + argKey + " removed a Reference to " + lclO + " when we expected to remove a Reference to " + argValue);
			}
		}
	}
	
	@Override
	public V get(Object argKey) {
		if (argKey == null) {
			throw new IllegalArgumentException("argKey is null");
		}
		
//		ourLogger.debug("Asked OpalCache " + this + " for " + argOK + " (" + argOK.hashCode() + ")");
		
		getStatistics().tallyRequest();
		
		Reference<V> lclReference = getMap().get(argKey);
		
		if (lclReference == null) {
//			ourLogger.debug("Not found in cache");
			getStatistics().tallyMiss();
			return null;
		} else {
//			ourLogger.debug("CacheMap has a reference.");
			V lclValue = lclReference.get();
			if (lclValue == null) {
//				ourLogger.debug("CacheMap's reference has expired.");
//				Object lclRemoved = remove(argKey);
//				if (lclRemoved != lclReference) {
//					ourLogger.error("Object removed was not the expected Reference", new IllegalStateException("Object removed was not the expected Reference"));
//				}
//				ourLogger.debug("Found expired reference.");
				getStatistics().tallyExpired();
				return null;
			} else {
//				ourLogger.debug("CacheMap's reference has a real object.");
//				ourLogger.debug("Found " + lclOpal);
				getStatistics().tallyHit();
				return lclValue;
			}
		}
	}
	
	// THINK: Figure out how to make sure that there are no pending transactions
	
	@Override
	public void clear() {
		getMap().clear();
		getStatistics().clear();
	}
	
	private CacheStatistics myCacheStatistics = new CacheStatistics();
	
	public CacheStatistics getStatistics() {
		return myCacheStatistics;
	}
	
	public class CacheStatistics {
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
		/**
		 * @return Returns the myHits.
		 */
		public int getHits() {
			return myHits;
		}
		/**
		 * @return Returns the myMisses.
		 */
		public int getMisses() {
			return myMisses;
		}
		/**
		 * @return Returns the myRequests.
		 */
		public int getRequests() {
			return myRequests;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object argKey) {
		return get(argKey) != null;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set<K> keySet() {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> t) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	@Override
	public int size() {
		expungeExpired();
		return getMap().size();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection<V> values() {
		throw new UnimplementedOperationException();
	}
	
	public int expungeExpired() {
		int lclCount = 0;
		Iterator<Map.Entry<K, Reference<V>>> lclI = getMap().entrySet().iterator();
		while (lclI.hasNext()) {
			Map.Entry<K, Reference<V>> lclEntry = lclI.next();
			Reference<V> lclR = lclEntry.getValue();
			if (lclR != null) {
				V lclValue = lclR.get();
				if (lclValue == null) {
					lclI.remove();
					++lclCount;
				}
			}
		}
		return lclCount;
	}
}
