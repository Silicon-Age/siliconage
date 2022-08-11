package com.siliconage.util;
import java.util.Collection;
import java.util.Iterator;

/**
 * Iterator that contains an internal Iterator and internal
 * RecursiveIterator.
 * <BR><BR>
 * Copyright &copy; 2000, 2001 Silicon Age, Inc. All Rights Reserved.
 *
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public final class RecursiveIterator<T> implements Iterator<T> {
	/* This guy can be improved by instantiating with the actual root Object
	that is also an Iterator or a Collection */
	
	private Iterator<T> myIterator;
	private RecursiveIterator<T> myChildRecursiveIterator;
	
	protected RecursiveIterator() {
		super();
	}
	
	/**
	 * Constructs a new RecursiveIterator over the specified Iterable.
	 * @param argIterable
	 *        The Iterable over which to recursively iterate.
	 */
	public RecursiveIterator(Iterable<T> argIterable) {
		this(argIterable.iterator());
	}
	
	/**
	 * Constructs a new RecursiveIterator over the specified Collection.
	 * @param argCollection
	 *        The Collection over which to recursively iterate.
	 */
	public RecursiveIterator(Collection<T> argCollection) {
		this(argCollection.iterator());
	}
	
	/**
	 * RecursiveIterator constructor that sets the value of the internal Iterator.
	 * @param argI Iterator
	 */
	public RecursiveIterator(Iterator<T> argI) {
		setIterator(argI);
	}
	
	/**
	 * Returns the internal recursive iterator.
	 * @return RecursiveIterator
	 */
	public RecursiveIterator<T> getChildRecursiveIterator() {
		return myChildRecursiveIterator;
	}
	
	/**
	 * Returns the internal iterator.
	 * @return Iterator
	 */
	public Iterator<T> getIterator() {
		return myIterator;
	}
	
	// Iterator methods
	/**
	 * Returns <code>true</code> if the iteration has more elements.
	 * @return boolean returns <code>true</code> if the internal recursive
	 * iterator is not <code>null</code> and either the internal recursive iterator or the
	 * internal iterator has more elements; <code>false</code> otherwise.
	 */
	@Override
	public boolean hasNext() {
		return ((getChildRecursiveIterator() != null) && (getChildRecursiveIterator().hasNext()) || getIterator().hasNext());
	}
	
	/**
	 * If the internal recursive iterator is not <code>null</code> and has more elements, 
	 * returns the next element.  Otherwise, returns the next element in the 
	 * internal iterator.
	 * @return Object
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T next() {
		if ((getChildRecursiveIterator() != null) && getChildRecursiveIterator().hasNext()) {
			return getChildRecursiveIterator().next();
		} else {
			setChildRecursiveIterator(null);
			T lclObject = getIterator().next();
			if (lclObject instanceof Iterable) {
				Iterator<T> lclIterator = ((Iterable<T>) lclObject).iterator();
				setChildRecursiveIterator(new RecursiveIterator<>(lclIterator));
			} else if (lclObject instanceof Collection) {
				Iterator<T> lclIterator = ((Collection<T>) lclObject).iterator();
				setChildRecursiveIterator(new RecursiveIterator<>(lclIterator));
			}
			return lclObject;
		}
	}
	
	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @param argChildRecursiveIterator A child to be set as the object's internal recursive iterator
	 */
	protected void setChildRecursiveIterator(RecursiveIterator<T> argChildRecursiveIterator) {
		myChildRecursiveIterator = argChildRecursiveIterator;
	}
	
	/**
	 * Sets the internal iterator.
	 * @param argIterator Iterator
	 */
	protected void setIterator(Iterator<T> argIterator) {
		myIterator = argIterator;
	}
}
