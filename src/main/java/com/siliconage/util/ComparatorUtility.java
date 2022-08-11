package com.siliconage.util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Class that provides static utility methods for using Comparators, especially
 * with regards to Collections.
 * <P>
 * Copyright &copy; 2001 Silicon Age, Inc. All Rights Reserved.
 *
 * @author	<a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
 * @author	<a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public abstract class ComparatorUtility {
	/**
	 * @param <E> The type of the objects to be compared
	 * @param argFirstObject The first object to compare
	 * @param argSecondObject The second object to compare
	 * @param argComparators A <code>Collection</code> of <code>Comparator&lt;E&gt;</code>s
	 * to use to compare <code>argFirstObject</code> and <code>argSecondObject</code> 
	 * in <code>argComparators</code>'s order of iteration
	 * @return The result of the first <code>Comparator&lt;E&gt;</code> in <code>argComparators</code> 
	 * that gives a nonzero result on <code>argFirstObject</code> and <code>argSecondObject</code>,
	 * or <code>0</code> if all the <code>Comparator&lt;E&gt;</code>s in <code>argCollection</code> give <code>0</code>.
	 */
	public static <E> int compare(E argFirstObject, E argSecondObject, Collection<Comparator<E>> argComparators) {
		for (Comparator<E> lclC : argComparators) {
			int lclValue = lclC.compare(argFirstObject, argSecondObject);
			if (lclValue != 0) {
				return lclValue;
			}
		}
		return 0;
	}
	
	/**
	 * Sorts the elements in the source Collection into the target Collection
	 * using the specified Comparator.
	 * <P>
	 * NOTE: This method attempts to add items in order to the end of the
	 * target Collection. If the target Collection performs its own internal
	 * sorting, then this method will not work.
	 * @param <E>
	 *        The type of the objects in the collections
	 * @param argSourceCollection
	 *        The Collection whose elements are to be sorted
	 * @param argTargetCollection
	 *        The Collection to which the sorted elements are to be added
	 * @param argComparator
	 *        The Comparator to be used
	 */
	public static <E> void sort(Collection<E> argSourceCollection, Collection<E> argTargetCollection, Comparator<E> argComparator) {
		ArrayList<E> lclList = new ArrayList<>(argSourceCollection.size());
		
		lclList.addAll(argSourceCollection);
		
		Collections.sort(lclList, argComparator);
		
		argTargetCollection.addAll(lclList);
	}
	
	/**
	 * Returns a Collection containing all the elements in the specified
	 * Collection sorted by the specified Comparator.
	 * @param <E>
	 *        The type of the objects in the collections
	 * @param  argCollection
	 *         The Collection to be filtered.
	 * @param  argComparator
	 *         The Comparator to be used.
	 * @return A sorted Collection.
	 */
	public static <E> Collection<E> sort(Collection<E> argCollection, Comparator<E> argComparator) {
		ArrayList<E> lclSortedList = new ArrayList<>();
		
		sort(argCollection, lclSortedList, argComparator);
		
		return lclSortedList;
	}
}
