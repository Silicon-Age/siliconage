package com.siliconage.util;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Implements the Iterator and ListIterator interfaces to allow iteration over
 * an array.
 * <P>
 * Does not support any of the add, remove, or set methods.
 * <P>
 * Copyright &copy; 2000, 2001 Silicon Age, Inc. All Rights Reserved.
 *
 * @author	<a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
 * @author	<a href="mailto:matt.mcglincy@silicon-age.com">Matt McGlincy</a>
 * @author	<a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class ArrayListIterator<T> implements ListIterator<T> {
	/**
	 * The array over which to iterate.
	 */
	private T[] myArray;
	
	/**
	 * The current index of the array.
	 * NOTE: This is actually the index of the "next" element. Thus
	 * myArray[myIndex] == the element returned by next() and
	 * myArray[myIndex - 1] == the element returned by previous().
	 */
	private int myIndex;
	
	/**
	 * Constructs a new ArrayListIterator over the specified array.
	 * @param     argArray
	 *            The Object array over which to iterate.
	 * @throws    IllegalArgumentException
	 *            If argObjectArray is <code>null</code>.
	 */
	public ArrayListIterator(T[] argArray) {
		this(argArray, 0);
	}
	
	/**
	 * Constructs a new ArrayListIterator over the specified array beginning at
	 * the specified index.
	 * @param argArray The Object array over which to iterate.
	 * @param argIndex The starting index.
	 * @throws IllegalArgumentException If argArray is <code>null</code>
	 * @throws IndexOutOfBoundsException If argIndex is out of bounds
	 */
	public ArrayListIterator(T[] argArray, int argIndex) {
		if (argArray == null) {
			throw new IllegalArgumentException("argArray is null");
		} else if (argIndex < 0 || argIndex > argArray.length) {
			throw new IndexOutOfBoundsException("Start index (" + argIndex + ") out of bounds in " + getClass().getName() + " constructor.");
		}
		
		myArray = argArray;
		
		setIndex(argIndex);
	}
	
	/**
	 * @param     argObject
	 *            The object to be added at the current index.
	 * @throws    UnsupportedOperationException
	 *            ArrayListIterator does not support this operation.
	 */
	
	@Override
	public void add(T argObject) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @param     argIndex
	 *            The index of the element to be retrieved.
	 * @return    The element at the specified index.
	 * @throws    NoSuchElementException
	 *            If there is no element at that index.
	 */
	protected T get(int argIndex) throws NoSuchElementException {
		try {
			return myArray[argIndex];
		} catch (IndexOutOfBoundsException lclIndexOutOfBoundsException) {
			throw new NoSuchElementException();
		}
	}
	/**
	 * @return <code>true</code> if there is an element after the
	 *         current index; <code>false</code> otherwise.
	 */
	@Override
	public boolean hasNext() {
		return (myArray != null && nextIndex() < myArray.length);
	}
	
	/**
	 * @return <code>true</code> if there is an element before the
	 *         current index; <code>false</code> otherwise.
	 */
	@Override
	public boolean hasPrevious() {
		return (myArray != null && previousIndex() >= 0);
	}
	
	/**
	 * @return    The next element of the ArrayListIterator
	 * @throws    NoSuchElementException
	 *            If there is no next element.
	 */
	@Override
	public T next() throws NoSuchElementException {
		if (hasNext()) {
			int lclIndex = nextIndex();
			
			// Need to add one, since next is the "current" index
			setIndex(lclIndex + 1);
			
			return get(lclIndex);
		} else {
			throw new NoSuchElementException();
		}
	}
	
	/**
	 * Returns the index of the next element of the ArrayListIterator. Does not
	 * increment the current index.
	 * @return The index of the next element.
	 */
	@Override
	public int nextIndex() {
		return myIndex;
	}
	
	/**
	 * @return    The previous element of the ArrayListIterator
	 * @throws    NoSuchElementException
	 *            If there is no previous element.
	 */
	@Override
	public T previous() throws NoSuchElementException {
		if (hasPrevious()) {
			int lclIndex = previousIndex();
			
			setIndex(lclIndex);
			
			return get(lclIndex);
		} else {
			throw new NoSuchElementException();
		}
	}
	
	/**
	 * Returns the index of the previous element of the ArrayListIterator. Does not
	 * decrement the current index.
	 * @return The index of the previous element.
	 */
	@Override
	public int previousIndex() {
		return (myIndex - 1);
	}
	
	/**
	 * @throws    UnsupportedOperationException
	 *            ArrayListIterator does not support this operation.
	 */
	@Override
	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Throws an UnsupportedOperationException.
	 * @param     argObject
	 *            The object to be set at the current index.
	 * @throws    UnsupportedOperationException
	 *            ArrayListIterator does not support this operation.
	 */
	@Override
	public void set(Object argObject) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Sets the current index of the ArrayListIterator
	 * @param argIndex
	 *        The current index.
	 */
	protected void setIndex(int argIndex) {
		myIndex = argIndex;
	}
}
