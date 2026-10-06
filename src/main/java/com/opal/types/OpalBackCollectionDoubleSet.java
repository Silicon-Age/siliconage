package com.opal.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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
	
	/* The SENTINEL_OLD_SET_FOR_NEW_COLLECTIONS is always empty.  When a new OpalBackCollectionDoubleSet is created, its myOldSet
	 * field is to this value to indicate that should be treated as having been loaded from the database.  (We can do this
	 * without issuing any sort of query because there cannot be records in the database since it is new.)  No code should call
	 * any methods on this instance of Fast3Set.
	 */
	private static final Set<?> SENTINEL_OLD_SET_FOR_NEW_COLLECTIONS = new Fast3Set<>();
	
	@SuppressWarnings("unchecked")
	private static <C> Set<C> getSentinelOldSet() {
		return (Set<C>) SENTINEL_OLD_SET_FOR_NEW_COLLECTIONS;
	}
	
	/* This is the set as seen by Threads without an active TransactionContext (or whose active TransactionContext is
	 * not the one this OpalBackCollectionSet (TransactionAware) has joined.  If the Thread asks for an Iterator,
	 * it'll get back an Iterator on this set.  This member will be null until the set is loaded.  It might also be null
	 * for a (loaded-because-it's-new) back collection belonging to a new Opal.  (Because, of course, no other
	 * Thread should have access to that Opal's UserFacing, so they shouldn't be trying to look at its contents.
	 */
	private Set<C> myOldSet;

	/* If isLoaded() is true (i.e., myOldSet is null), then myNewSet will contain a Set of all child Opals from the
	 * database (plus whatever changes have been made in the current TransactionContext).  If isLoaded() is false (and
	 * myOldSet is null), then myNewSet will contain a Set of child opals that have been added or removed in the
	 * current TransactionContext (this is a replacement for the old functionality implemented by CachedOperation).
	 * Upon loading, such a temporary Set will need to be integrated with the from-the-database values.
	 */
	private Set<C> myNewSet;
	
	/* This new member expands the memory footprint of every instance of this class, which is irritating, after I worked so
	 * hard to slim it down.  As of right now, though, it seems to be necessary.  It indicates whether the cached
	 * operations stored in myNewSet need to be resolved.  We previously resolved them immediately upon loading,
	 * but the load could be triggered by a different Thread than the one that had cached the operations, and that
	 * led to problems.  Except for the extra memory, this is probably better for performance.
	 */ 
	private boolean myCachedOperationsResolved;
	
	/* The Opal for which this is a back collection. */
	private final P myOwner;
	
	/* A collection of method references and other information needed to load, add, and remove objects from this
	 * back collection.  For instance, myLoader provides a method that allows this class to set the parent of
	 * a child opal.  Doing that is necessary when one is removed from this Set via a remove() call.
	 */
	private final OpalBackCollectionLoader<C, P> myLoader;
	
	/* FEATURE: Allow for only loading the count if all we need to do is report on whether we are empty. */
	@SuppressWarnings("resource")
	public OpalBackCollectionDoubleSet(P argOwner, OpalBackCollectionLoader<C, P> argLoader, boolean argNew) {
		super();
		
		myOwner = Objects.requireNonNull(argOwner);
		
		myLoader = Objects.requireNonNull(argLoader);
		
		if (argNew) {
			TransactionContext.assertActive();
			joinTransactionContext(TransactionContext.getActive());
			myOldSet = getSentinelOldSet(); // We shouldn't ever be accessing this anyway.  Since this is not null, the collection will count as "Loaded"
			myNewSet = createSet(); // THINK: As a later optimization, it would be nice to not create this set until we know we need it.
			myCachedOperationsResolved = true;
		} else {
			myOldSet = null;
			myNewSet = null;
			myCachedOperationsResolved = false;
		}
	}
	
	/* Note that removalAllowed() could return true, but trying to remove an item without being in an active
	 * TransactionContext will still result in an Exception!
	 */
	protected boolean removalAllowed() {
		return getLoader().removalAllowed();
	}
	
	/* This returns either the "old" (outside of a TransactionContext) child set or "new" (inside a TransactionContext) depending
	 * on argX (true = new, false = old).  It guarantees that the set has been loaded from persistent storage and any necessary
	 * cached operations resolved.
	 * 
	 * In practical terms, if the next thing you want to do is iterate over the Set and truly process all the child Opals that
	 * should be there, this is what you want to call.
	 */
	protected /* synchronized */ Set<C> determineSetLoaded(boolean argX) {
		ensureMonitor();
		Set<C> set = Objects.requireNonNull(argX ? getNewSetLoaded() : getOldSetLoaded());
		Set<C> sentinel = getSentinelOldSet();
		Validate.isTrue(set != sentinel); // THINK: Eventually, we ought to be able to remove this check.
		return set;
	}

	/* Depending on whether we are outside or inside the current Thread's TransactionContext, this returns either the old child set
	 * or the new child set, guaranteeing that it has been loaded and had any necessary cached operations resolved.
	 */
	protected /* synchronized */ Set<C> determineSetLoaded() {
		ensureMonitor();
		Set<C> set = determineSetLoaded(tryAccess());
		Set<C> sentinel = getSentinelOldSet();
		Validate.isTrue(set != sentinel); // THINK: Eventually, we ought to be able to remove this check.
		return set;
	}
	
	protected /* synchronized */ Set<C> createSet() {
		return getLoader().getDefaultSetSupplier().get();
	}
	
	protected /* synchronized */ Set<C> createSet(Set<C> argC) {
		ensureMonitor();
		Objects.requireNonNull(argC);
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
	}
	
	/* This creates the appropriate Set to use for the "old" child set (taking advantage of the fact that they can
	 * be unmodifiable).  Small sets are put into custom classes, large sets are just returned.
	 */
	private static <C> Set<C> store(Set<C> argCs) {
		Objects.requireNonNull(argCs);
		int lclSize = argCs.size();
		if (lclSize == 0) {
			return Collections.emptySet();
		} else if (lclSize == 1) {
			return Collections.singleton(argCs.iterator().next()); // THINK: Is there a classier way to get this element?
		} else if (lclSize <= 3) {
			return new Fast3Set<>(argCs); // THINK: Should make an unmodifiable version of this for extra safety?
		} else {
			return argCs;
		}
	}
	
	protected OpalBackCollectionLoader<C, P> getLoader() {
		return myLoader;
	}
	
	protected /* synchronized */ void load() {
		ensureMonitor();
		Validate.isTrue(myOldSet == null); // Synonymous with isLoaded() == false
		P lclOwner = getOwner();
		Set<C> lclLoadedCs = Objects.requireNonNull(getLoader().getLoader().apply(lclOwner));
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("Loaded {} owned by {}.  Size is {}.",
					defaultToString(),
					lclOwner.defaultToString(),
					Integer.valueOf(lclLoadedCs.size())
					);
		}
		
		/* This next assignment is fine to do even if it overwrites the sentinel value for myOldSet. */
		myOldSet = store(lclLoadedCs);
		Objects.requireNonNull(myOldSet, "myOldSet is null");
	}
	
	// THINK: Can this ever be called when isLoaded is false?
	@SuppressWarnings("resource")
	protected /* synchronized */ void resolveCachedOperations() {
		ensureMonitor();
		Set<C> sentinel = getSentinelOldSet();
		Validate.isTrue(myOldSet != sentinel);
		Validate.isTrue(myCachedOperationsResolved == false);
	
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("resolveCachedOperations on {} belonging to {}.",
					defaultToString(),
					getOwner()
					);
		}
		
		if (isLoaded() == false) {
			ourLogger.warn("resolvedCachedOperations called on {} belonging to {} when isLoaded() == false.",
					defaultToString(),
					getOwner()
					);
		}		
		
		/* Note that myOldSet can be null here.  If we grab a reference to an Opal and then immediately clear one of its
		 * child collections, both myOldSet and myNewSet will be null and myCachedOperationsResolved will be false.
		 */		
		if (myNewSet != null) {
			TransactionContext tc = TransactionContext.getActive();
			if (tc == null) {
				ourLogger.warn("In OpalBackCollectionDoubleSet::resolveCachedOperations, myNewSet is not null but we aren't in an active TransactionContext.");
			}
			if (tc != getTransactionContext()) {
				ourLogger.warn("In OpalBackCollectionDoubleSet::resolveCachedOperations, myNewSet is not null but belong to a TransactionContext that is not the active one.");
			}
			P lclOwner = getOwner();
			Set<C> lclCachedOperations = myNewSet;
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("There are {} cached operations to perform for {} owned by {}.",
						Integer.valueOf(lclCachedOperations.size()),
						defaultToString(),
						lclOwner.defaultToString()
						);
			}
			ensureLoaded();
			myNewSet = createSet(myOldSet);
			for (C lclC : lclCachedOperations) {
				P lclCurrentOwner = getLoader().getChildAccessor().apply(lclC);
				if (lclC.exists() && lclCurrentOwner == lclOwner) {
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug("Adding {} as a cached operation to {} owned by {}.",
								lclC,
								defaultToString(),								
								lclOwner.defaultToString()
								);
					}
					if (myNewSet.add(lclC) == false) { // false = Collection was not changed as a result of this call.
						final String newC, deletedC;
						if (lclC instanceof UpdatableOpal<?> uo) {
							newC = uo.isNew() ? "new" : "not new";
							deletedC = uo.isDeleted() ? "deleted" : "not deleted";
						} else {
							newC = "-";
							deletedC = "-";
						}						
						final String newP, deletedP;						
						if (lclOwner instanceof UpdatableOpal<?> uo) {
							newP = uo.isNew() ? "new" : "not new";
							deletedP = uo.isDeleted() ? "deleted" : "not deleted";
						} else {
							newP = "-";
							deletedP = "-";
						}
						final String newO, deletedO;
						if (lclCurrentOwner == null) {
							newO = "null";
							deletedO = "null";
						} else if (lclCurrentOwner instanceof UpdatableOpal<?> uo) {
							newO = uo.isNew() ? "new" : "not new";
							deletedO = uo.isDeleted() ? "deleted" : "not deleted";
						} else {
							newO = "-";
							deletedO = "-";
						}
						ourLogger.warn("While implementing cached-operation addition of {} ({}, {}) to {} owned by {} ({}, {}), it was already found in myNewSet and was owned by {} ({}, {}).",
								lclC,
								newC, deletedC,
								defaultToString(),
								lclOwner.defaultToString(),
								newP, deletedP,
								(lclCurrentOwner != null) ? lclCurrentOwner.defaultToString() : "null",
								newO, deletedO
								);
					}
				} else {
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug("Removing {} as a cached operation from {} owned by {}.",
								lclC,
								defaultToString(),
								lclOwner.defaultToString()
								);
					}
					if (myNewSet.remove(lclC) == false) {
						ourLogger.warn("While implementing cached-operation removal of {} from {}, it was not found in myNewSet owned by {}.",
								lclC,
								defaultToString(),
								lclOwner.defaultToString()
								);
					}
				}
			}
			Objects.requireNonNull(myNewSet, "myNewSet is null");
			myCachedOperationsResolved = true;
		} else if (isLoaded()) {
			myNewSet = createSet(myOldSet);
			myCachedOperationsResolved = true;
		} else {
			// Nothing to do
		}
		// Note that resolveCachedOperations could, in theory, still be false here.
	}
		
	/* This returns the "old" (outside of a TransactionContext) child set guaranteeing that it has been loaded from persistent storage. */ 
	protected /* synchronized */ Set<C> getOldSetLoaded() {
		ensureMonitor();
		ensureLoaded();
		return Objects.requireNonNull(myOldSet);
	}
	
	/* This will not return null. */ 
	protected /* synchronized */ Set<C> getNewSetForAddsAndRemoves() {
		ensureMonitor();
		if (myNewSet == null) {
			if (isLoaded()) {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Initializing myNewSet with a copy of myOldSet in {} owned by {}.",
							defaultToString(),
							getOwner()
							);
				}
				myNewSet = Objects.requireNonNull(createSet(myOldSet)); // FIXME: This may be unnecessary; do we actually need to copy it until we know we are changing things?  Can we handle Iterator.remove()?
			} else {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Initializing myNewSet with a new set for cached operations in {} owned by {}.",
							defaultToString(),
							getOwner()
							);
				}
				Validate.isTrue(myCachedOperationsResolved == false);
				myNewSet = Objects.requireNonNull(createSet()); // This will be a temporary Set in which to cache added and removed C's in the current TransactionContext.
			}
		} else {
			/* myNewSet might be either for cached operations or actual operations. */
		}
		Objects.requireNonNull(myNewSet);
		return myNewSet;
	}
	
	/* This returns the "new" (inside the current Thread's TransactionContext) child set guaranteeing that it has been loaded from
	 * persistent storage and any necessary cached operations resolved.
	 */ 
	protected /* synchronized */ Set<C> getNewSetLoaded() {
		ensureMonitor();
		ensureLoaded();
		if (myNewSet == null) {
			myNewSet = Objects.requireNonNull(createSet(myOldSet)); // We do have to copy it at this point; people calling getNewSetLoaded are ready to use the contents
		} else {
			if (myCachedOperationsResolved == false) {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("There are {} cached operations to resolve when accessing the new set for {} owned by {}.",
							Integer.valueOf(myNewSet.size()),
							defaultToString(),
							getOwner()
							);
				}
				resolveCachedOperations(); // This might replace myNewSet with a different Set
			} else {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Cached operations have already been resolved when accessing the new set for {} owned by {}.",
							defaultToString(),
							getOwner()
							);
				}
			}
		}
		Objects.requireNonNull(myNewSet);
		return myNewSet;
	}
	
	/* myOwner is final, so no need for synchronization. */
	protected P getOwner() {
		return myOwner;
	}
	
	private /* synchronized */ boolean addForReal(C argC) {
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
	
	/* This method's sole responsibility is putting the C into our internal Set.  It does not concern itself with maintaining any
	 * of the other Opal-to-Opal links.  (In particular, calling this to assign Opal X to Opal Y's collection of X's will not
	 * set X's reference/foreign key to Y.  It will only add X to Y's internal set.)
	 * @see com.opal.types.OpalBackCollectionSet#removeInternal(com.opal.Opal)
	 */
	@Override
	public synchronized boolean addInternal(C argC) {
		if (argC == null) {
			throw new IllegalArgumentException("Tried to addInternal a null element to a back collection belonging to " + getOwner() + ".");
		}
		tryMutate();
		return getNewSetForAddsAndRemoves().add(argC); // This might end up being a cached operation.
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
			lclReturn |= addForReal(lclC);
		}
		return lclReturn;
	}
	
	@Override
	public synchronized void clear() { // THINK: Does this need to be synchronized?
		tryMutate();
		if (isLoaded() == false) {
			ourLogger.debug("clear() called for unloaded back collection belonging to {}.", getOwner());
		}
		Set<C> lclNewSet = getNewSetLoaded();
//		System.out.println("In clear() myOldSet has " + myOldSet.size() + " elements and myNewSet has " + myNewSet.size() + "elements.");
		for (C lclC : lclNewSet) {
//			System.out.println("Clearing " + lclC + " from " + defaultToString());
			if (lclC instanceof UpdatableOpal<?>) {
				UpdatableOpal<?> lclUO = (UpdatableOpal<?>) lclC;
//				System.out.println("In clear(), calling the unsafe child mutator for " + lclC);
				getLoader().getUnsafeChildMutator().accept(lclC, null);
//				System.out.println("In clear(), unlinking " + lclC);
				lclUO.unlink();
//				System.out.println("In clear(), unlinked " + lclC);
			} else {
				throw new IllegalStateException("Tried to clear " + lclC + " from a back collection owned by " + getOwner() + " but the reference cannot be set to null (and it is not an UpdatableOpal and therefore couldn't be unlinked).");
			}
		}
		lclNewSet.clear();
		Validate.isTrue(lclNewSet == myNewSet);
//		System.out.println("Ending clear for " + defaultToString() + " with myOldSet = " + System.identityHashCode(myOldSet) + " and size() = " + myOldSet.size() + "; myNewSet = " +System.identityHashCode(myNewSet) + " and size() = " + myNewSet.size());
	}
	
	/* Calling the contains(...) method with a non-C Opal probably indicates incorrectly conceived code. */
	@Override
	@SuppressWarnings("unlikely-arg-type")
	public synchronized boolean contains(Object argO) {
		if (argO == null) {
			return false;
		}
		if ((argO instanceof Opal<?>) == false) {
			ourLogger.warn("Called OpalBackCollectionDoubleSet.contains() with a non-Opal argument {}.", argO);
		}
		return determineSetLoaded().contains(argO); // THINK: This could be made faster if we had access to the Source's accessor
	}
	
	/* Calling the containsAll(...) method with a Collection that is not a Collection of COpals probably indicates
	 * incorrectly conceived code. */
	@Override
	@SuppressWarnings("unlikely-arg-type")
	public synchronized boolean containsAll(Collection<?> argC) {
		if (argC == null) {
			return false;
		}
		return determineSetLoaded().containsAll(argC); // THINK: This could be made faster if we had access to the Source's accessor
	}
	
	@Override
	public synchronized boolean isEmpty() {
		return determineSetLoaded().isEmpty();
	}
	
	@Override
	public synchronized Iterator<C> iterator() {
		boolean lclAccess = tryAccess();
		Set<C> lclS = determineSetLoaded(lclAccess);
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("Creating REI with lclS = {} and lclAccess == {}.", lclS, Boolean.valueOf(lclAccess));
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
			ourLogger.debug("removeForReal called for " + argC + " on " + defaultToString() + " belonging to " + getOwner().defaultToString() + ".");
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
			ourLogger.warn("Tried to remove an object of type {} from a back collection belonging to {}.",
					argT.getClass().getName(),
					getOwner().defaultToString(),
					lclE
					);
			return false; // I hate to do this, but nobody should be trying to remove things that aren't T's in the first place.
		}
	}
	
	@Override
	public synchronized boolean removeInternal(C argC) {
		if (argC == null) {
			throw new IllegalArgumentException("Tried to removeInternal a null element from a back collection belonging to " + getOwner().defaultToString() + ".");
		}
		tryMutate();
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("About to removeInternal() {} from {} owned by {}.",
					argC,
					defaultToString(),
					getOwner().defaultToString()
					);
		}
		return getNewSetForAddsAndRemoves().remove(argC);
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
				lclResult |= removeForReal(lclC);
			} catch (ClassCastException e) {
				ourLogger.warn("Collection passed to removeAll() had an object of type {}.", lclO.getClass().getName(), e);
			}
		}
		return lclResult;
	}
	
	@Override
	public synchronized boolean removeIf(Predicate<? super C> argP) { /* THINK: Is it possible to not synchronize on the entire collection for the entirety of this method? */
		tryMutate();
		boolean lclResult = false;
		Iterator<C> lclPI = getNewSetLoaded().iterator();
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
		throw new UnsupportedOperationException("retainAll hasn't been written");
	}
	
	@Override
	public synchronized int size() {
		return determineSetLoaded().size();
	}
	
	@Override
	public synchronized Object[] toArray() {
		return determineSetLoaded().toArray();
	}
	
	@Override
	public synchronized <U> U[] toArray(U[] argA) { // Why can't/shouldn't this be "U super C"?
		return determineSetLoaded().toArray(argA);
	}
	
	/* commitPhaseOne should make any changes to external, persistent stores that need to happen to reflect the new
	 * state of the Opals.  In the case of a back collection set for a relational database, no changes need to be made.
	 * This is because the necessary UPDATE statements will be run when the child Opals execute their own phase-one
	 * commits (because every reference/foreign key manifests itself as columns in the child table).  This would not
	 * necessarily be true for all persistent stores, so this class should not be used with Opals not backed by
	 * relational databases until that issue has been thought about! 
	 */
	@Override
	protected  /* synchronized */ void commitPhaseOneInternal(TransactionParameter argTP) throws PersistenceException {
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
	protected /* synchronized */ void commitPhaseTwoInternal(TransactionParameter argTP) throws PersistenceException {
		Set<C> sentinel = getSentinelOldSet();
		if (isLoaded()) {
			if (myOldSet == sentinel) {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("After phase-2 commit of {}, myOldSet was the sentinel.", defaultToString());
				}
			}
			if (myNewSet != null) {
				if (myCachedOperationsResolved == false) {
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug("After phase-2 commit of {}, cached operations are being resolved", defaultToString());
					}
					resolveCachedOperations();
				}
				Validate.isTrue(myCachedOperationsResolved);
				myOldSet = store(myNewSet);
			} else {
				// TODO: Explain why there is nothing to do here.
			}
		} else {
			// TODO: Explain why there is nothing to do here.
		}
		myNewSet = null;
		if (myOldSet == sentinel) {
			ourLogger.error("After phase-2 committing {}, myOldSet is the sentinel.", defaultToString());
		}
		if (myOldSet != null) {
			for (var c : myOldSet) {
				if (c instanceof UpdatableOpal<?> uo) {
					if (uo.isDeleted()) {
						ourLogger.warn("After phase-2 committing {}, myOldSet contains {}, which is deleted.", defaultToString(), c.defaultToString());
					}
				}
			}
		}
		if (isLoaded() && (myOldSet == null)) {
			ourLogger.warn("After phase-2 committing {}, isLoaded() is true while myOldSet is null.", defaultToString());
		}
		if (isLoaded() == false && (myOldSet != null)) {
			ourLogger.warn("After phase-2 committing, isLoaded() is false while myOldSet is not null.", defaultToString());
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
		/* If this was a new OpalBackCollectionDoubleSet for a new Opal, then rolling back the TransactionContext should
		 * make it effectively unreachable.  Still, we don't want to retain references to the sentinel set in
		 * a place where code could reference them (though said code would be calling methods on Opals that don't
		 * really have any old fields set, so they should mostly lead to Exceptions).
		 */
		Set<C> sentinel = getSentinelOldSet();
		if (myOldSet == sentinel) {
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("When rolling back {}, myOldSet equaled the sentinel set, so it has been cleared.", defaultToString());
			}
			myOldSet = null;
		}
		/* This should never be possible because isLoaded() is precisely equivalent to myOldSet being null. */
		if (isLoaded() == false && myOldSet != null) {
			ourLogger.warn("When rolling back {}, isLoaded() is false while myOldSet is not null.", defaultToString());
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
			myIterator = Objects.requireNonNull(argIterator);
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
	
	/* FIXME: There's a fundamental problem-in-waiting with the (not very recent) change to this method to display the contents
	 * of the Collection (loading them if necessary).  Namely, the debugging code in the load() method, among other places,
	 * calls toString(), so we get infinite recursion when messages are logged (or constructed, in the absence of ifXEnabled()
	 * checks).  Generally we don't output those messages so everything works, but if we do enable them, things crash. 
	 */	
	@Override
	public synchronized String toString() {
//		ensureLoaded();
//		return String.valueOf(System.identityHashCode(this)); // Use this when debugging (and comment out ensureLoaded()
		return determineSetLoaded().toString();
	}
}
