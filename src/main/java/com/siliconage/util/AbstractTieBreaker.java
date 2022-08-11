package com.siliconage.util;
import java.io.Serializable;
import java.util.Comparator;

/**
 * A TieBreaker contains a Comparator that is inconsistent with equals,
 * but acts as a Comparator consistent with equals by correctly handling
 * the case where the member Comparator's compare() method returns 0.
 * Correct handling is determined by the implementing subclasses.
 * <br><br>
 * A class like this has two advantages:  First, it allows something like
 * sorting by city to work on a sorted set, without requiring that the
 * function that carries out the sort contain some kludge to be
 * consistent with equals.  Second -- but this might be more trouble than
 * it's worth -- one can change this Comparator's member Comparator to
 * get around cases like a SortedSet where the Comparator is set at runtime.
 * <BR><BR>
 * Copyright &copy; 2001 Silicon Age, Inc. All Rights Reserved.
 * @author  <a href="mailto:matt.bruce@silicon-age.com">Matt Bruce</a>
 * @author  <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public abstract class AbstractTieBreaker<T> implements Comparator<T>, Serializable {
	private static final long serialVersionUID = 1L;
	
	private final Comparator<T> myComparator;
	
	/**
	 * Constructs a TieBreaker instance with the specific Comparator.
	 * <i>Argument must not be <code>null</code>.</i>
	 * @param  argComparator  this new TieBreaker's comparator.
	 * @throws IllegalArgumentException if argComparator is 
	 * <code>null</code>
	 */
	public AbstractTieBreaker (Comparator<T> argComparator) {
		super();
		assert argComparator != null;
		myComparator = argComparator;
	}
	
	/**
	 * Compares the specified objects.  Returns the value of comparing those
	 * objects with the contained comparator.  Subclasses should override this
	 * method, but can call super to get the initial comparison out of the way.
	 * @return  a negative integer, zero, or a positive integer as the first 
	 * argument is less than, equal to, or greater than the second.
	 * @param   argFirstObject	the first Object to be compared
	 * @param   argSecondObject   the second Object to be compared
	 */
	@Override
	public int compare (T argFirstObject, T argSecondObject) {
		return getComparator().compare (argFirstObject, argSecondObject);
	}
	
	/**
	 * Returns this TieBreaker's Comparator.
	 * <i>Guaranteed non-null return.</i>
	 * @return   this TieBreaker's Comparator.
	 */
	public Comparator<T> getComparator() {
		return myComparator;
	}
	
//	/**
//	 * Sets this TieBreaker's Comparator.
//	 * <i>Argument must not be <code>null</code>.</i>
//	 * 
//	 * @param   argComparator   this TieBreaker's new Comparator
//	 * @throws IllegalArgumentException if argComparator is 
//	 * <code>null</code>
//	 */
//	public void setComparator(Comparator<T> argComparator) {
//		// sanity check: argument must not be null
//		if (argComparator == null) {
//			throw new IllegalArgumentException (getClass().getName() + ".setComparator() reports: argument must not be null.");
//		}
//		
//		myComparator = argComparator;
//	}
}
