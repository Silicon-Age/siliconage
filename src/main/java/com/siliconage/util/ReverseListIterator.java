package com.siliconage.util;
import java.util.List;
import java.util.ListIterator;

public class ReverseListIterator<E> implements ListIterator<E> {
	private final ListIterator<E> myListIterator;
	
	public ReverseListIterator(ListIterator<E> argListIterator) {
		super();
		assert argListIterator != null;
		myListIterator = argListIterator;
	}
	
	public ReverseListIterator(List<E> argList) {
		this(argList.listIterator(argList.size()));
	}
	
	@Override
	public boolean hasNext() {
		return myListIterator.hasPrevious();
	}
	
	@Override
	public E next() {
		return myListIterator.previous();
	}
	
	@Override
	public boolean hasPrevious() {
		return myListIterator.hasNext();
	}
	
	@Override
	public E previous() {
		return myListIterator.next();
	}
	
	@Override
	public int nextIndex() {
		return myListIterator.previousIndex();
	}
	
	@Override
	public int previousIndex() {
		return myListIterator.nextIndex();
	}
	
	@Override
	public void remove() {
		myListIterator.remove();
	}
	
	@Override
	public void set(E argObject) {
		myListIterator.set(argObject);
	}
	
	@Override
	public void add(E argObject) {
		myListIterator.add(argObject);
	}
}
