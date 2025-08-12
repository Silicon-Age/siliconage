package com.opal;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public abstract class TreeAdapter<T> {

	protected TreeAdapter() {
		super();
	}
	
	public abstract T getParent(T argChild);

	public abstract Set<T> getChildSet(T argParent);
	
	public List<T> getOrderedChildList(T argParent) {
		List<T> lclChildren = new java.util.ArrayList<>(getChildSet(argParent));
		lclChildren.sort(null); // Relies on intrinsic ordering
		return lclChildren;
	}
	
	public ListIterator<T> childIterator(T argParent) {
		return getOrderedChildList(argParent).listIterator();
	}
}
