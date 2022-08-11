package com.siliconage.util;
import java.util.Comparator;

/**
 * Extends AbstractTieBreaker to compare objects by their hash code.
 * <BR><BR>
 * Copyright &copy; 2001 Silicon Age, Inc. All Rights Reserved.
 * @author  <a href="mailto:matt.bruce@silicon-age.com">Matt Bruce</a>
 * @author  <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class HashCodeTieBreaker<E> extends AbstractTieBreaker<E> {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructs a HashCodeTieBreaker with the specified comparator.
	 * <i>Argument must not be <code>null</code>.</i>
	 * @param  argComparator  this new TieBreaker's comparator.
	 * @throws IllegalArgumentException if argComparator is 
	 * <code>null</code>
	 */
	public HashCodeTieBreaker (Comparator<E> argComparator) {
		super(argComparator);
	}
	
	/**
	 * Compares the specified Objects based on the criteria specified by the
	 * member comparator, with the Objects' hash codes as a tiebreaker.
	 * @return  a negative integer, zero, or a positive integer as the first 
	 * argument is less than, equal to, or greater than the second.
	 * @param   argFirstObject the first Object to be compared
	 * @param   argSecondObject the second Object to be compared
	 */
	@Override
	public int compare(E argFirstObject, E argSecondObject) {
		int lclRetVal = super.compare(argFirstObject, argSecondObject);
		
		if (lclRetVal == 0) {
			lclRetVal = argSecondObject.hashCode() - argFirstObject.hashCode();
		}
		
		return lclRetVal;
	}
}
