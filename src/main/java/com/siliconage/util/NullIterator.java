package com.siliconage.util;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator that contains no objects.
 * <BR><BR>
 * Copyright &copy; 2000, 2001 Silicon Age, Inc. All Rights Reserved.
 *
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public final class NullIterator<T> implements Iterator<T> {
	private static NullIterator<?> ourInstance = new NullIterator<>();
	
	/**
	 * Returns an instance of NullIterator.  This should be the only
	 * way of getting an instance of NullIterator.
	 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
	 * @param <T> The type of the object being iterated over, automatically produced as needed
	 * @return NullIterator
	 */
	@SuppressWarnings("unchecked")
	public static <T> NullIterator<T> getInstance() {
		return (NullIterator<T>) ourInstance;
	}
	
	/**
	 * Default NullIterator constructor.
	 * Constructor is private as this class is a singleton.
	 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
	 */
	public NullIterator() {
		super();
	}
	
	/**
	 * Returns <code>false</code> as there are no objects in this Iterator.
	 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
	 * @return boolean always <code>false</code>
	 */
	@Override
	public boolean hasNext() {
		return false;
	}
	
	/**
	 * Always throws NoSuchElementException.
	 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
	 * @return Object always <code>null</code>
	 */
	@Override
	public T next() {
		throw new NoSuchElementException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
