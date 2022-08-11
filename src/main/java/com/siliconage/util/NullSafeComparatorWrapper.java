package com.siliconage.util;
import java.util.Comparator;

/**
 * @author topquark
 */
public class NullSafeComparatorWrapper<T> extends NullSafeComparator<T> {
//	private static final long serialVersionUID = 1L;
	
	private final Comparator<T> myComparator;
	public NullSafeComparatorWrapper(Comparator<T> argC) {
		super();
		if (argC == null) {
			throw new IllegalArgumentException("argC is null");
		}
		myComparator = argC;
	}
	
	public final Comparator<T> getComparator() {
		return myComparator;
	}
	
	/* (non-Javadoc)
	 * @see com.siliconage.util.NullSafeComparator#compareInternal(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected int compareInternal(T argA, T argB) {
		return getComparator().compare(argA, argB);
	}
}
