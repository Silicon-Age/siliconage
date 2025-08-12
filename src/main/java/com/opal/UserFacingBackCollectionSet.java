package com.opal;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;

import com.siliconage.util.UnimplementedOperationException;

public class UserFacingBackCollectionSet<U extends UserFacing, O extends Opal<U>> implements Set<U> {

//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(UserFacingBackCollectionSet.class.getName());

	private final Set<O> myOpalSet;
	
	public UserFacingBackCollectionSet(Set<O> argOpalSet) {
		super();
		
		myOpalSet = Validate.notNull(argOpalSet);
	}
	
	protected O determineOpal(U argU) {
		@SuppressWarnings("unchecked")
		O lclOpal = ((OpalBacked<U, O>) argU).getOpal();
		
//		/* FIXME: This is problematic because if we have a Set of child polymorphic Opals, then calling getOpal()
//		 * on the UserFacing of one we are trying to add might not always return the same type of Opal.  We need
//		 * to add getOpal() functionality that (somehow) returns the Opal at the correct level of the hierarchy
//		 * for whatever is supposed to go into the Collection.  I'm not quite sure how to do that; I'm also not sure
//		 * how the existing code manages to work.
//		 */
//		@SuppressWarnings("unchecked")
//		O lclReturnOpal = (O) lclOpal; // To reiterate, this is wrong, and will fail with some multi-table polymorphic calls.
		return lclOpal;
	}
	
	@Override
	public boolean add(U argU) {
		return myOpalSet.add(determineOpal(argU));
	}
	
	@Override
	public boolean addAll(Collection<? extends U> argUs) {
		boolean lclResult = false;
		for (U lclU : argUs) {
			lclResult ^= add(lclU);
		}
		return lclResult;
	}
	
	@Override
	public void clear() {
		myOpalSet.clear();
	}
	
	@Override
	public boolean contains(Object argO) {
		if (argO instanceof UserFacing) {
			@SuppressWarnings("unchecked")
			U lclU = (U) argO;
			O lclO = determineOpal(lclU);
			return myOpalSet.contains(lclO);
		} else {
			/* If it's not a U, it can't possibly have an Opal in our internal Set. */
			return false;
		}
	}
	
	@Override
	public boolean containsAll(Collection<?> argOs) {
		for (Object lclO : argOs) {
			if (contains(lclO) == false) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean isEmpty() {
		return myOpalSet.isEmpty();
	}
	
	public boolean isNonEmpty() {
		return isEmpty() == false;
	}
	
	@Override
	public Iterator<U> iterator() {
		return new OpalIterator<>(myOpalSet.iterator());
	}
	
	@Override
	public boolean remove(Object argO) {
		if (argO instanceof UserFacing) {
			@SuppressWarnings("unchecked")
			U lclU = (U) argO;
			return myOpalSet.remove(determineOpal(lclU));
		} else {
			/* If it's not a U, it can't possibly have an Opal in our internal Set. */
			return false;
		}
	}
	
	@Override
	public boolean removeAll(Collection<?> argUs) {
		boolean lclResult = false;
		for (Object lclO : argUs) {
			lclResult ^= remove(lclO);
		}
		return lclResult;
	}
	
	@Override
	public boolean removeIf(Predicate<? super U> argPredicate) {
		Validate.notNull(argPredicate);
		return myOpalSet.removeIf(x -> argPredicate.test(x.getUserFacing()));
	}
	
	@Override
	public boolean retainAll(Collection<?> argArg0) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public int size() {
		return myOpalSet.size();
	}
	
	@Override
	public Object[] toArray() {
		Object[] lclReturn = myOpalSet.toArray();
		for (int lclI = 0; lclI < lclReturn.length; ++lclI) {
			@SuppressWarnings("unchecked")
			U lclNew = ((Opal<U>) (lclReturn[lclI])).getUserFacing();
			lclReturn[lclI] = lclNew; 
		}
		return lclReturn;
	}
	
	/* THINK: What are we supposed to do if we are passed in an array of the wrong type to hold the
	 * UserFacings that we know are coming out of our internal set?
	 * 
	 * I think we let Java throw a ClassCastException and let the user sort it out. 
	 */
	@Override
	public <T> T[] toArray(T[] argTs) {
		if (argTs == null) {
			throw new IllegalArgumentException("argTs is null");
		}
		final int lclSize = size();
		/* Is the provided array too small? */
		if (argTs.length < lclSize) {
			@SuppressWarnings("unchecked")
			T[] lclTs = (T[]) Array.newInstance(argTs.getClass().getComponentType(), lclSize); // lclTs needed for warning suppression
			argTs = lclTs;
		}
		int lclIndex = 0;
		Iterator<U> lclI = iterator();
//		ourLogger.warn("size == " + size() + " hasNext() == " + lclI.hasNext() + " length == " + argTs.length);
		while (lclI.hasNext()) {
//			ourLogger.warn("lclIndex = " + lclIndex);
			@SuppressWarnings("unchecked")
			T lclT = (T) lclI.next();
			argTs[lclIndex++] = lclT;
		}
		/* If the provided array was too big, null out the remaining entries. */
		while (lclIndex < argTs.length) {
			argTs[lclIndex++] = null;
		}
		return argTs;
	}
	
	@Override
	public String toString() {
		return myOpalSet.toString();
	}
}
