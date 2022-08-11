package com.siliconage.util;
//import java.io.Serializable;
import java.util.Comparator;

/**
 * @author topquark
 */
public abstract class NullSafeComparator<T> implements Comparator<T> /*, Serializable */ {
//	private static final long serialVersionUID = 1L;
	
	protected NullSafeComparator() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public final int compare(T argA, T argB) { // final so that subclasses can't override it but instead must override compareInternal
		return argA == null ? argB == null ? 0 : 1 : argB == null ? -1 : compareInternal(argA, argB);
	}
	
	protected abstract int compareInternal(T argA, T argB);

	/* Note that this sorts null references last. */
	
	public static final <U extends Comparable<U>> int nullSafeCompare(Comparable<U> argA, U argB) {
		return argA == null ? argB == null ? 0 : 1 : argB == null ? -1 : argA.compareTo(argB);
	}
	
	public static final int nullSafeCompareIgnoreCase(String argA, String argB) {
		return argA == null ? argB == null ? 0 : 1 : argB == null ? -1 : argA.compareToIgnoreCase(argB);
	}
}
