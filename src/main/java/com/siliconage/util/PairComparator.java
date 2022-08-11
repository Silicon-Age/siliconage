package com.siliconage.util;

import java.util.Comparator;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

public class PairComparator<L, R> implements Comparator<Pair<? extends L, ? extends R>> {
	private final boolean myLeftFirst;
	private final Comparator<? super L> myLeftComparator;
	private final Comparator<? super R> myRightComparator;
	
	public PairComparator(Comparator<? super L> argLeftComparator, Comparator<? super R> argRightComparator, boolean argLeftFirst) {
		super();
		
		Validate.isTrue(argLeftComparator != null || argRightComparator != null);
		
		myLeftFirst = argLeftFirst;
		myLeftComparator = argLeftComparator;
		myRightComparator = argRightComparator;
	}
	
	public PairComparator(Comparator<? super L> argLeftComparator, Comparator<? super R> argRightComparator) {
		this(argLeftComparator, argRightComparator, true);
	}
	
	public PairComparator(Comparator<? super L> argLeftComparator) {
		this(argLeftComparator, null);
	}
	
	public Comparator<? super L> getLeftComparator() {
		return myLeftComparator;
	}
	
	public Comparator<? super R> getRightComparator() {
		return myRightComparator;
	}
	
	@Override
	public int compare(Pair<? extends L, ? extends R> argA, Pair<? extends L, ? extends R> argB) {
		if (getLeftComparator() == null) {
			Validate.notNull(getRightComparator());
			return getRightComparator().compare(argA.getRight(), argB.getRight());
		} else {
			if (myLeftFirst || getRightComparator() == null) {
				int lclResult = getLeftComparator().compare(argA.getLeft(), argB.getLeft());
				if (lclResult != 0) {
					return lclResult;
				} else if (getRightComparator() == null) {
					return 0;
				} else {
					return getRightComparator().compare(argA.getRight(), argB.getRight());
				}
			} else {
				int lclResult = getRightComparator().compare(argA.getRight(), argB.getRight());
				if (lclResult != 0) {
					return lclResult;
				} else {
					return getLeftComparator().compare(argA.getLeft(), argB.getLeft());
				}
			}
		}
	}
}
