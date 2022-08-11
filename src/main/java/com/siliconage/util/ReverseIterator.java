package com.siliconage.util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Class which allows reverse iteration over a Collection or Iterator.
 * <P>
 * Note that this implementation is not backed by the Collection or Iterator and
 * that the remove method is unsupported.
 * <P>
 * Copyright &copy; 2001 Silicon Age, Inc. All Rights Reserved.
 *
 * @author	<a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
 * @author	<a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class ReverseIterator<E> implements Iterator<E> {
	/**
	 * The backing ListIterator used for the reverse iteration.
	 */
	private ListIterator<E> myListIterator;
	
	/**
	 * Constructs a new ReverseIterator over the Collection.
	 * @param argCollection
	 *        The Collection over which to be reversely iterated.
	 */
	public ReverseIterator(Collection<E> argCollection) {
		if (argCollection instanceof List) {
			initializeList((List<E>) argCollection);
		} else {
			initializeCollection(argCollection);
		}
	}
	
	/**
	 * Constructs a new ReverseIterator over the Iterator.
	 * @param argIterator
	 *        The Iterator over which to be reversely iterated.
	 */
	public ReverseIterator(Iterator<E> argIterator) {
		if (argIterator instanceof ListIterator) {
			initializeListIterator((ListIterator<E>) argIterator);
		} else {
			initializeIterator(argIterator);
		}
	}
	
	protected ListIterator<E> getListIterator() {
		return myListIterator;
	}
	
	@Override
	public boolean hasNext() {
		return getListIterator().hasPrevious();
	}
	// NOTE: At first, the initializeX methods were all called simply
	// initialize. However, there was some problem with the initialize in
	// initialize(List) calling initialize(Iterator) instead of
	// initialize(ListIterator).
	
	protected void initializeCollection(Collection<E> argCollection) {
		initializeList(new ArrayList<>(argCollection)); /* Warning okay */
	}
	
	protected void initializeIterator(Iterator<E> argIterator) {
		List<E> lclList = new ArrayList<>(); /* Warning okay */
		
		while (argIterator.hasNext()) {
			lclList.add(argIterator.next());
		}
		
		initializeList(lclList);
	}
	
	protected void initializeList(List<E> argList) {
		initializeListIterator(argList.listIterator(argList.size()));
	}
	
	protected void initializeListIterator(ListIterator<E> argListIterator) {
		while (argListIterator.hasNext()) {
			argListIterator.next();
		}
		
		setListIterator(argListIterator);
	}
	
	@Override
	public E next() {
		return getListIterator().previous();
	}
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	protected void setListIterator(ListIterator<E> argListIterator) {
		if (argListIterator == null) {
			throw new IllegalArgumentException("argListIterator is null");
		}
		myListIterator = argListIterator;
	}
}
