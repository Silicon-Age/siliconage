package com.siliconage.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An Iterator over a single object.
 * <P>
 * Copyright &copy; 2000, 2001 Silicon Age, Inc. All Rights Reserved.
 *
 * @author	<a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author	<a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public final class SingletonIterator<T> implements Iterator<T> {
	/**
	 * Object used to indicate that the SingletonIterator has already iterated
	 * over its Object. This allows the SingletonIterator to iterate over a
	 * null.
	 */
	private boolean myHasNext;
	
	private T myObject;
	
	/**
	 * SingletonIterator constructor that sets the value of the object.
	 * @param  argObject The lone object for this Iterator to contain
	 */
	public SingletonIterator(T argObject) {
		super();
		myHasNext = true;
		set(argObject);
	}
	
	/**
	 * @return the lone object that this Iterator contains
	 */
	protected T get() {
		return myObject;
	}
	
	/**
	 * Returns <code>true</code> SingletonIterator has not yet iterated over its
	 * Object.
	 * @return <code>true</code> if the SingletonIterator has not yet
	 *         iterated over its Object; <code>false</code> otherwise.
	 */
	@Override
	public boolean hasNext() {
		return myHasNext;
	}
	
	/**
	 * @return The only object that this Iterator contains
	 * @throws NoSuchElementException if the Iterator has already produced its element
	 */
	@Override
	public T next() throws NoSuchElementException {
		if (hasNext()) {
			T lclObject = get();
			myHasNext = false;
			return lclObject;
		} else {
			throw new NoSuchElementException();
		}
	}
	
	/**
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @param argObject The lone object for this Iterator to contain
	 */
	protected void set(T argObject) {
		myObject = argObject;
	}
}
