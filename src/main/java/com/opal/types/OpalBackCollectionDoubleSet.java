package com.opal.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

import com.opal.Opal;
import com.opal.PersistenceException;
import com.opal.TransactionContext;
import com.opal.TransactionParameter;
import com.opal.TransactionalOpal;
import com.opal.UpdatableOpal;
import com.siliconage.util.Fast3Set;

/* C = child opal (contained in the Set), P = parent opal (owner of the Set) */
public class OpalBackCollectionDoubleSet<C extends TransactionalOpal<?>, P extends TransactionalOpal<?>> extends AbstractOpalBackCollectionSet<C, P> {
	
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalBackCollectionDoubleSet.class.getName());
	
	private static final Set<?> SENTINEL_OLD_SET_FOR_NEW_COLLECTIONS = new Fast3Set<>();
	
	@SuppressWarnings("unchecked")
	private static <C> Set<C> getSentinelOldSet() {
		return (Set<C>) SENTINEL_OLD_SET_FOR_NEW_COLLECTIONS;
	}
	
//	// FIXME: Allow this to contain sets of UserFacings to get rid of a whole bunch of Opal <-> UserFacing swaps
//	/* THINK: Jonah suggests that we can get rid of this and instead test for myLoaded == true by checking for
//	 * myOldSet == null.  This isn't a trivial change, but I think he's right that we can get rid of this.  I'm a
//	 * little gun-shy of making another big change like that right now, but I'll try to include this in a future
//	 * release.  Certainly saving another slot in every back collection allocation will make a difference in our
//	 * memory footprint.
//	 */ 
//	private boolean myLoaded = false;
	
	/* This is the set as seen by Threads without an active TransactionContext (or whose active TransactionContext is
	 * not the one this OpalBackCollectionSet (TransactionAware) has joined.  If the Thread asks for an Iterator,
	 * it'll get back an Iterator on this set.  This will be null until the set is loaded.  It might also be null
	 * for a (loaded-because-it's-new) back collection belonging to a new Opal.  (Because, of course, no other
	 * Thread should have access to that Opal's UserFacing, so they shouldn't be trying to look at its contents. 
	 */
	private Set<C> myOldSet;

	/* If myLoaded is true, then myNewSet will contain a Set of all child Opals from the database (plus whatever
	 * changes have been made in the current TransactionContext.  If myLoaded is false, then it will contain a Set of
	 * child opals that have been added or removed in the current TransactionContext (this is a replacement for the old
	 * functionality implemented by CachedOperation).  Upon loading, such a temporary Set will need to be integrated
	 * with the from-the-database values.
	 */
	private Set<C> myNewSet;
	
	/* The Opal for which this is a back collection. */
	private final P myOwner;
	
	/* A collection of method references and other information needed to load, add, and remove objects from this
	 * back collection.  For instance, myLoader provides a method that allows this class to set the parent of
	 * a child opal.  Doing that is necessary when one is removed from this Set via a remove() call.
	 */
	private final OpalBackCollectionLoader<C, P> myLoader;
	
	/* FIXME: Test cached operations replacement. */
	
	/* FIXME: Allow for only loading the count if all we need to do is report on whether we are empty. */
	
	public OpalBackCollectionDoubleSet(P argOwner, OpalBackCollectionLoader<C, P> argLoader, boolean argNew) {
		super();
		
		myOwner = Validate.notNull(argOwner);
		
		myLoader = Validate.notNull(argLoader);
		
		if (argNew) {
			TransactionContext.assertActive();
			joinTransactionContext(TransactionContext.getActive());
//			myLoaded = true;
			myOldSet = getSentinelOldSet(); // We shouldn't ever be accessing this anyway.
			myNewSet = createSet(); // THINK: As a later optimization, it would be nice to not create this set until we know we need it.
		}
	}
	
	protected boolean removalAllowed() {
		return getLoader().removalAllowed();
	}
	
	protected /* synchronized */ Set<C> determineSet(boolean argX) {
		ensureMonitor();
		return Validate.notNull(argX ? getNewSet() : getOldSet());
	}

	protected /* synchronized */ Set<C> determineSet() {
		ensureMonitor();
		Set<C> lclSet = determineSet(tryAccess());
		Set<C> lclWrongSet = getSentinelOldSet();
		Validate.isTrue(lclSet != lclWrongSet); // THINK: Eventually, we ought to be able to remove this.
		return lclSet;
	}
	
	protected /* synchronized */ Set<C> createSet() {
		return getLoader().getDefaultSetSupplier().get();
	}
	
	protected /* synchronized */ Set<C> createSet(Set<C> argC) {
		ensureMonitor();
		Validate.notNull(argC);
		return getLoader().getCopySetSupplier().apply(argC);
	}
	
	protected /* synchronized */ boolean isLoaded() {
		ensureMonitor();
		return myOldSet != null;
	}
	
	protected /* synchronized */ void ensureLoaded() {
		ensureMonitor();
		if (isLoaded() == false) {
			load();
		}
		Validate.isTrue(isLoaded());
		/* myOldSet may still be false (null?) here if this is a newly created Opal that only has a new set (prior to it being committed
		 * so the set can be shared with other threads.  It will still be "loaded" (isLoaded() == true) in that case, however.
		 */		
	}
	
	protected Set<C> store(Set<C> argCs) {
		Validate.notNull(argCs);
		int lclSize = argCs.size();
		if (lclSize == 0) {
			return Collections.emptySet();
		} else if (lclSize == 1) {
			return Collections.singleton(argCs.iterator().next()); // THINK: Is there a better way to get this element?
		} else if (lclSize <= 3) {
			return new Fast3Set<>(argCs);
		} else {
			return argCs;
		}
	}
	
	public OpalBackCollectionLoader<C, P> getLoader() {
		return myLoader;
	}
	
	protected /* synchronized */ void load() {
		ensureMonitor();
//		Validate.isTrue(myLoaded == false);
		Validate.isTrue(myOldSet == null);
		P lclOwner = getOwner();
		Set<C> lclLoadedCs = Validate.notNull(getLoader().getLoader().apply(lclOwner));
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("Loaded " + defaultToString() + " owned by " + lclOwner.defaultToString() + ".  Size is " + lclLoadedCs.size() + ".");
		}
		
		myOldSet = store(lclLoadedCs);
		if (myNewSet != null) {
			Set<C> lclCachedOperations = myNewSet;
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("There are " + lclCachedOperations.size() + " cached operations to perform for " + defaultToString() + " owned by " + lclOwner.defaultToString() + ".");
			}
			myNewSet = createSet(myOldSet);
			for (C lclC : lclCachedOperations) {
				P lclCurrentOwner = getLoader().getChildAccessor().apply(lclC);
				if (lclC.exists() && lclCurrentOwner == lclOwner) {
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug("Adding " + lclC + " as a cached operation to " + defaultToString() + " owned by " + lclOwner.defaultToString() + ".");
					}
					myNewSet.add(lclC);
				} else {
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug("Removing " + lclC + " as a cached operation from " + defaultToString() + " owned by " + lclOwner.defaultToString() + ".");
					}
					/*boolean lclSuccess = */ myNewSet.remove(lclC);
				}
			}
		}
		Validate.notNull(myOldSet);
//		myLoaded = true;
	}
	
	/* This will not return null. */ 
	protected /* synchronized */ Set<C> getOldSet() {
		ensureMonitor();
		ensureLoaded();
		return Validate.notNull(myOldSet);
	}
	
	/* This will not return null. */ 
	protected /* synchronized */ Set<C> getNewSet() {
		ensureMonitor();
		if (myNewSet == null) {
			if (isLoaded()) {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Initializing myNewSet with a copy of myOldSet in " + defaultToString() + " owned by " + getOwner() + ".");
				}
				myNewSet = Validate.notNull(createSet(getOldSet())); // FIXME: This may be unnecessary; do we actually need to copy it until we know we are changing things?  Can we handle Iterator.remove()?
			} else {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Initializing myNewSet with a new set for cached operations in " + defaultToString() + " owned by " + getOwner() + ".");
				}
				myNewSet = createSet(); // This will be a temporary set in which to cache added and removed C's in the current TransactionContext.
			}
		}
		Validate.notNull(myNewSet);
		return myNewSet;
	}
	
	/* myOwner is final, so no need for synchronization. */
	protected P getOwner() {
		return myOwner;
	}
	
	private boolean addForReal(C argC) {
		assert argC != null;
		TransactionContext.assertActive();
		P lclOwner = getOwner();
		boolean lclResult = (getLoader().getChildAccessor().apply(argC) != lclOwner);
		getLoader().getSafeChildMutator().accept(argC, lclOwner);
		return lclResult;
	}
	
	@Override
	public synchronized boolean add(C argC) { // THINK: Does this need to be synchronized?
		if (argC == null) {
			throw new IllegalArgumentException("Tried to add a null element to a back collection belonging to " + getOwner() + ".");
		}
		tryMutate();
		return addForReal(argC);
	}
	
	@Override
	public synchronized boolean addInternal(C argC) {
		if (argC == null) {
			throw new IllegalArgumentException("Tried to addInternal a null element to a back collection belonging to " + getOwner() + ".");
		}
		tryMutate();
		return getNewSet().add(argC);
	}
	
	@Override
	public synchronized boolean addAll(Collection<? extends C> argC) { // FIXME: Does this need to be synchronized?
		if (argC == null) { // THINK: Is this correct?  Or should this raise an Exception?
			return false;
		}
		tryMutate();
		boolean lclReturn = false;
		for (C lclC : argC) {
			if (lclC == null) {
				throw new IllegalStateException("Tried to add a null element to a back collection belonging to " + getOwner() + " via addAll.");
			}
			lclReturn ^= addForReal(lclC);
		}
		return lclReturn;
	}
	
	@Override
	public synchronized void clear() { // THINK: Does this need to be synchronized?
		tryMutate();
		if (isLoaded() == false) {
			ourLogger.debug("clear() called for unloaded back collection belonging to " + getOwner() + ".");
		}
		ensureLoaded();
		Set<C> lclNewSet = getNewSet();
		for (C lclC : lclNewSet) {
			if (lclC instanceof UpdatableOpal<?>) {
				UpdatableOpal<?> lclUO = (UpdatableOpal<?>) lclC;
				getLoader().getUnsafeChildMutator().accept(lclC, null);
				lclUO.unlink();
			} else {
				throw new IllegalStateException("Tried to clear " + lclC + " from a back collection owned by " + getOwner() + " but the reference cannot be set to null (and it is not an UpdatableOpal and therefore couldn't be unlinked).");
			}
		}
		lclNewSet.clear();
	}
	
	/* Calling the contains(...) method with a non-C Opal probably indicates incorrectly conceived code. */
	@Override
	public synchronized boolean contains(Object argO) {
		if (argO == null) {
			return false;
		}
		if ((argO instanceof Opal<?>) == false) {
			ourLogger.warn("Called OpalBackCollectionDoubleSet.contains() with a non-Opal argument.");
		}
		ensureLoaded();
		return determineSet().contains(argO); // THINK: This could be made faster if we had access to the Source's accessor
	}
	
	/* Calling the containsAll(...) method with a Collection that is not a Collection of COpals probably indicates
	 * incorrectly conceived code. */
	@Override
	public synchronized boolean containsAll(Collection<?> argC) {
		if (argC == null) {
			return false;
		}
		ensureLoaded();
		return determineSet().containsAll(argC); // THINK: This could be made faster if we had access to the Source's accessor
	}
	
	@Override
	public synchronized boolean isEmpty() {
		ensureLoaded();
		return determineSet().isEmpty();
	}
	
	@Override
	public synchronized Iterator<C> iterator() {
		ensureLoaded();
		boolean lclAccess = tryAccess();
		Set<C> lclS = determineSet(lclAccess);
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("Creating REI with lclS = " + lclS + " and lclAccess == " + lclAccess);
		}
		if (lclS == null) {
			throw new IllegalStateException("In iterator() for " + defaultToString() + " (belonging to " + getOwner() + " (" + ((com.opal.IdentityOpal<?>) getOwner()).getUniqueString() + ")), determineSet() returned null with lclAccess == " + lclAccess + ".  myOldSet == " + myOldSet + ".  myNewSet == " + myNewSet + ".");
		}
		return new RemovalEnabledIterator(lclS.iterator(), lclAccess);
	}
	
	/* Must have already called tryMutate() when invoking this. */
	private /* synchronized */ boolean removeForReal(C argC) {
		ensureMonitor();
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("removeForReal called for " + argC + " on " + defaultToString() + " belongng to " + getOwner().defaultToString() + ".");
		}
		assert argC != null;
		/* Does this child Opal actually belong to this Collection? */
		P lclParent = getLoader().getChildAccessor().apply(argC);
		if (lclParent != getOwner()) {
			ourLogger.warn("Tried to remove Opal " + argC + " from a back collection belonging to " + getOwner().defaultToString() + ", but it was actually owned by " + lclParent + ".");
			return false;
		} else {
			if (removalAllowed()) {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("About to invoke getSafeChildMutator on " + argC + " to set the parent to null.");
				}
				getLoader().getSafeChildMutator().accept(argC, null); // This will call our own removeInternal method for us.
				return true;
			} else {
				if (argC instanceof UpdatableOpal<?>) {
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug("About to invoke getSafeChildMutator on " + argC + " to set the parent to null and then unlink it.");
					}
					UpdatableOpal<?> lclUO = (UpdatableOpal<?>) argC;
					getLoader().getSafeChildMutator().accept(argC, null); // This will call our own removeInternal method for us.
					lclUO.unlink();
					return true;
				} else {
					throw new IllegalStateException("Could not remove " + argC + " from a back collection owned by " + getOwner().defaultToString() + " because its foreign key is NOT NULL (and it is not an UpdatableOpal and therefore does not have an unlink method).");
				}
			}
		}
	}
	
	@Override
	public synchronized boolean remove(Object argT) {
		if (argT == null) {
			throw new IllegalArgumentException("Tried to remove a null element to a back collection belonging to " + getOwner().defaultToString() + ".");
		}
		tryMutate();
		try {
			@SuppressWarnings("unchecked") C lclC = (C) argT;
			return removeForReal(lclC);
		} catch (ClassCastException lclE) {
			ourLogger.warn("Tried to remove an object of type " + argT.getClass().getName() + " from a back collection belonging to " + getOwner().defaultToString() + ".");
			return false; // I hate to do this, but nobody should be trying to remove things that aren't T's in the first place.
		}
	}
	
	/* This method's sole responsibility is putting the C into our internal Set.  It does not concern itself with maintaining any
	 * of the other Opal-to-Opal links.  (In particular, calling this to assign Opal X to Opal Y's collection of X's will not
	 * set X's reference/foreign key to Y.  It will only add X to Y's internal set.)
	 * @see com.opal.types.OpalBackCollectionSet#removeInternal(com.opal.Opal)
	 * 
	 * This comment is above the wrong method -- RRH
	 */
	@Override
	public synchronized boolean removeInternal(C argC) {
		if (argC == null) {
			throw new IllegalArgumentException("Tried to removeInternal a null element to a back collection belonging to " + getOwner().defaultToString() + ".");
		}
		tryMutate();
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("About to removeInternal() " + argC + " from " + defaultToString() + " owned by " + getOwner().defaultToString() + ".");
		}
		if (isLoaded()) {
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug(defaultToString() + " was loaded.");
			}
			return getNewSet().remove(argC);
		} else {
			getNewSet().add(argC); // Cached operation
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug(defaultToString() + " was not loaded.  Added " + argC + " to the list of cached operations (which is now size " + getNewSet().size() + ").");
			}
//			System.out.println(defaultToString() + " was not loaded.  Added " + argC + " to the list of cached operations (which is now size " + getNewSet().size() + ").");
			return true; /* Quite possibly a lie. */
		}
	}
	
	@Override
	public synchronized boolean removeAll(Collection<?> argC) {
		if (argC == null) {
			return false;
		}
		boolean lclResult = false;
		tryMutate();
		for (Object lclO : argC) {
			try {
				@SuppressWarnings("unchecked") C lclC = (C) lclO;
				if (lclC == null) {
					throw new IllegalStateException("Collection passed to removeAll() contained a null.");
				}
				lclResult ^= removeForReal(lclC);
			} catch (ClassCastException lclE) {
				ourLogger.warn("Collection passed to removeAll() had an object of type " + lclO.getClass().getName() + ".");
			}
		}
		return lclResult;
	}
	
	@Override
	public synchronized boolean removeIf(Predicate<? super C> argP) { /* THINK: Is it possible to not synchronize on the entire collection for the entirety of this method? */
		ensureLoaded();
		tryMutate();
		boolean lclResult = false;
		Iterator<C> lclPI = getNewSet().iterator();
		while (lclPI.hasNext()) {
			C lclC = lclPI.next();
			if (argP.test(lclC)) {
				lclPI.remove();
				lclResult = true;
				if (removalAllowed()) {
					getLoader().getUnsafeChildMutator().accept(lclC,  null);
				} else {
					if (lclC instanceof UpdatableOpal<?>) {
						UpdatableOpal<?> lclUO = (UpdatableOpal<?>) lclC;
						getLoader().getUnsafeChildMutator().accept(lclC, null);
						lclUO.unlink();
					} else {
						throw new IllegalStateException("Could not remove " + lclC + " from a back collection owned by " + getOwner() + " because it is not an UpdatableOpal and does not have an unlink method.");
					}
				}
			}
		}
		return lclResult;
	}
	
	@Override
	public synchronized boolean retainAll(Collection<?> argC) {
//		tryMutate();
		throw new UnsupportedOperationException("retainAll hasn't been written");
	}
	
	@Override
	public synchronized int size() {
		ensureLoaded();
		return determineSet().size();
	}
	
	@Override
	public synchronized Object[] toArray() {
		ensureLoaded();
		return determineSet().toArray();
	}
	
	@Override
	public synchronized <U> U[] toArray(U[] argA) { // Why can't/shouldn't this be "U super C"?
		ensureLoaded();
		return determineSet().toArray(argA);
	}
	
	/* commitPhaseOne should make any changes to external, persistent stores that need to happen to reflect the new
	 * state of the Opals.  In the case of a back collection set for a relational database, no changes need to be made.
	 * This is because the necessary UPDATE statements will be run when the child Opals execute their own phase-one
	 * commits (because every reference/foreign key manifests itself as columns in the child table).  This would not
	 * necessarily be true for all persistent stores, so this class should not be used with Opals not backed by
	 * relational databases until that issue has been thought about! 
	 */
	@Override
	public  /* synchronized */ void commitPhaseOneInternal(TransactionParameter argTP) throws PersistenceException {
		/* Nothing to do, assuming we are being backed by a relational database. */
	}
	
	/* commitPhaseTwo should make changes to the Java instances to reflect the updates that have taken place in the
	 * TransactionContext.  In the case of a back collection set, we just need to copy the new contents of the set
	 * over to the myOldSet variable.
	 * 
	 * There are two complications with this:
	 * 
	 * First, we only need to do this if the set has been loaded from the database.  If it has NOT been loaded, then
	 * myNewSet will either be null or will contain a Set of Opals that have been added or removed from it ("cached
	 * operations").  If it's null, that's fine:  We don't need to do anything because anybody who wants to iterate
	 * or otherwise look at the set will trigger a loading.  If it's not null, that's also fine.  We can discard the
	 * altered Opals since the proper list will be loaded in the future if the user tries to look at the contents.
	 * (Because the new parents of the altered Opals will be reflected in the database when the Opals' commitPhaseOne
	 * method is called.)  Either way, if it hasn't been loaded, myOldSet will continue to be null, and that won't
	 * cause any problems.
	 * 
	 * Second, for memory footprint/performance reasons, if the set is particularly small, we don't keep it in a HashSet
	 * but instead replace it with an emptySet(), singletonSet(), or Fast3Set.  This is taken care of by the store(Set<S>))
	 * procedure that returns a copy of myNewSet in an appropriately sized class.  Then we null out myNewSet.
	 */
	@Override
	public /* synchronized */ void commitPhaseTwoInternal(TransactionParameter argTP) throws PersistenceException {
		if (isLoaded()) {
			myOldSet = store(myNewSet);
		}
		myNewSet = null;
		if (isLoaded() && (myOldSet == null)) {
			ourLogger.warn("After phase-2 committing " + defaultToString() + ", isLoaded() is true while myOldSet is null.");
		}
		if (isLoaded() == false && (myOldSet != null)) {
			ourLogger.warn("After phase-2 committing " + defaultToString() + ", isLoaded() is false while myOldSet is not null.");
		}
	}
	
	/* The only thing we need to do here is set myNewSet to null; this is true whether or not the back collection has
	 * been loaded.  Note that an Opal that has been added to this set needs to have its reference/foreign key changes
	 * rolled back as well, but that is not our responsibility:  That will be taken care of when that Opal is rolled
	 * back.
	 */
	@Override
	public void rollbackInternal() {
		myNewSet = null;
		/* This condition is theoretically okay:  If we are rolling back a newly created object (i.e., we intended to insert it,
		 * but couldn't), then it will be loaded but have no old set.  However, since it is being rolled back, no other references
		 * to it should exist outside the creating thread (and the creating thread should know not to do anything with it).
		 */
		/* if (isLoaded() && (myOldSet == null)) {
			ourLogger.warn("When rolling back " + defaultToString() + ", isLoaded() is true while myOldSet is null.");
		} */
		/* This probably represents a real error condition (that will manifest as an Exception as soon as something tries to
		 * load() this collection.
		 */
		if (isLoaded() == false && myOldSet != null) {
			ourLogger.warn("When rolling back " + defaultToString() + ", isLoaded() is false while myOldSet is not null.");
		}
	}
	
	/* Since we don't need to make any changes to persistent stores (see the comment on our commitPhaseOne() method,
	 * this should never be called.  If it is called, there's nothing to do.
	 */
	@Override
	protected TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap) {
		return null;
	}
	
	/* Joining a TransactionContext means that the Thread that has that TransactionContext active can now make changes
	 * to the TransactionAware.  In this case, that means adding or removing Opals from our set.
	 * 
	 * It seems like we would need to copy myOldSet at this point, but, in fact, we don't have to, as determineSet()
	 * will take care of any copying when it is needed.  In addition, if the set is unloaded, we might end up storing
	 * "cached operations" in myNewSet instead of the actual set of Opals in the back collection (though if iterator()
	 * is called, we would then need to load the Set and incorporate the changes).
	 */
	@Override
	protected void joinTransactionContextInternal() {
		/* Nothing to do.  determineSet() takes care of the complicated aspects of replicating myOldSet into
		 * myNewSet.
		 */
	}
	
	private class RemovalEnabledIterator implements Iterator<C> {
		private final Iterator<C> myIterator;
		private C myLastC;
		private final boolean myAccess; // The tryAccess() level of the Iterator used to construct this; true = iterating on a (changeable) myNewSet, false = iterating on an (unchangeable) myOldSet.
		
		private RemovalEnabledIterator(Iterator<C> argIterator, boolean argAccess) {
			super();
			myIterator = Validate.notNull(argIterator);
			myLastC = null;
			myAccess = argAccess;
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("REI ctor for iterator = " + argIterator + " and argAccess == " + argAccess + ".");
			}
		}
		
		@Override
		public boolean hasNext() {
			return myIterator.hasNext();
		}
		
		@Override
		public C next() {
			myLastC = myIterator.next();
//			if (getLoader().getChildAccessor().apply(myLastC) != getOwner()) {
//				ourLogger.warn("While iterating on a back collection belonging to " + getOwner() + " (IHC: " + System.identityHashCode(getOwner()) + "), we came across " + myLastC + " (IHC: " + System.identityHashCode(myLastC) +"), whose accessor returned " + getLoader().getChildAccessor().apply(myLastC) + " (IHC: " + System.identityHashCode(getLoader().getChildAccessor().apply(myLastC)) + ").");
//			}
			return myLastC;
		}
		
		@Override
		public void remove() {
			if (myLastC == null) {
				throw new IllegalStateException("remove() cannot be called at this point.");
			} else {
				synchronized (OpalBackCollectionDoubleSet.this) { // I don't use this syntax very often!
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug("About to call tryMutate in REI.remove().  myLastC = " + myLastC + " myAccess == " + myAccess + " tryAccess() == " + OpalBackCollectionDoubleSet.this.tryAccess() + ".");
					}
					OpalBackCollectionDoubleSet.this.tryMutate(); // THINK: Can this be the only line covered by synchronized?
					if (myAccess == false) {
						OpalBackCollectionDoubleSet.this.removeForReal(myLastC); /* Note that this is okay, because it it'll come out of myNewSet, while myIterator is for myOldSet. */
					} else {
						myIterator.remove();
						if (removalAllowed()) {
							getLoader().getUnsafeChildMutator().accept(myLastC, (P) null);
						} else {
							if (myLastC instanceof UpdatableOpal<?>) {
								UpdatableOpal<?> lclUO = (UpdatableOpal<?>) myLastC;
								getLoader().getUnsafeChildMutator().accept(myLastC, null);
								lclUO.unlink();
							} else {
								throw new IllegalStateException("Tried to remove " + myLastC + " from a back collection owned by " + getOwner() + " but it is not an UpdatableOpal so it couldn't be unlinked.");
							}
						}
					}
				}
				myLastC = null;
			}
		}
	}
	
//	@Override
//	public boolean isMutated() {
//		return tryAccess();
//	}
//	
//	@Override
//	public synchronized boolean isSetLoaded() {
//		return isLoaded();
//	}
	
	/* FIXME: There's a fundamental problem-in-waiting with the (not very recent) change to this method to display the contents
	 * of the Collection (loading them if necessary).  Namely, the debugging code in the load() method, among other places,
	 * calls toString(), so we get infinite recursion when messages are logged (or constructed, in the absence of ifXEnabled()
	 * checks).  Generally we don't output those messages so everything works, but if we do enable them, things crash. 
	 */	
	@Override
	public synchronized String toString() {
		ensureLoaded();
//		return String.valueOf(System.identityHashCode(this)); // Use this when debugging (and comment out ensureLoaded()
		return determineSet().toString();
	}
}
