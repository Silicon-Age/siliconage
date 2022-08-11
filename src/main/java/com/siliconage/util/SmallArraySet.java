package com.siliconage.util;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;

public class SmallArraySet<T> implements Set<T> {
	private T[] myItems;
	private int mySize = 0;
	private final Class<T> myClass;
	
	private static final int DEFAULT_SIZE = 2;
	
	public SmallArraySet(Class<T> argClass) {
		super();
		myClass = argClass;
	}
	
	@Override
	public int size() {
		return mySize;
	}
	
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@Override
	public boolean contains(Object argT) {
		if (argT == null) {
			return false;
		}
		for(int lclI = 0; lclI < mySize; ++lclI) {
			if (argT.equals(myItems[lclI])) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Iterator<T> iterator() {
		return new Iterator<>() {
			private int myIndex = 0;
			
			@Override
			public boolean hasNext() {
				return myIndex < size();
			}
			
			@Override
			public T next() throws NoSuchElementException {
				try {
					return myItems[myIndex++];
				} catch (ArrayIndexOutOfBoundsException lclE) {
					throw new NoSuchElementException();
				}
			}
			
			@Override
			public void remove() {
				SmallArraySet.this.remove(myIndex);
			}
		};
	}
	
	@Override
	public Object[] toArray() {
		if (mySize == 0) {
			return ArrayUtils.EMPTY_OBJECT_ARRAY;
		} else {
			Object[] lclArray = new Object[mySize];
			System.arraycopy(myItems, 0, lclArray, 0, mySize);
			return lclArray;
		}
	}
	
	@Override
	public <U> U[] toArray(U[] argArray) {
		if (mySize > 0) {
			System.arraycopy(myItems, 0, argArray, 0, size()); /* FIXME: Non-conformant with too-small arguments */
		}
		return argArray;
	}
	
	private T[] createArray(int argSize) {
		assert argSize >= 0;
		@SuppressWarnings("unchecked")
		T[] lclArray = (T[]) Array.newInstance(getComponentClass(), argSize); // Only named so I can SuppressWarnings
		return lclArray;
	}
	
	protected Class<T> getComponentClass() {
		return myClass;
	}
	
	@Override
	public boolean add(T argT) {
		if (argT == null) {
			throw new IllegalArgumentException("SmallArraySet does not support null objects.");
		}
		if (myItems == null) {
			myItems = createArray(DEFAULT_SIZE);
			myItems[mySize++] = argT;
			return true;
		} else {
			for (int lclI = 0; lclI < mySize; ++lclI) {
				if (argT.equals(myItems[lclI])) {
					return false;
				}
			}
			if (mySize == myItems.length) {
				T[] lclLargerArray = createArray(myItems.length <<1);
				System.arraycopy(myItems, 0, lclLargerArray, 0, mySize);
				myItems = lclLargerArray;
			}
			myItems[mySize++] = argT;
			return true;
		}
	}
	
	@Override
	public boolean remove(Object argT) {
		if (argT == null) {
			throw new IllegalArgumentException("SmallArraySet does not support null objects.");
		}
		if (myItems == null || mySize == 0) {
			return false;
		}
		for (int lclI = 0; lclI < mySize; ++lclI) {
			if (argT.equals(myItems[lclI])) {
				myItems[lclI] = myItems[--mySize];
				return true; /* FIXME: Shrink? */
			}
		}
		return false;
	}
	
	@Override
	public boolean containsAll(Collection<?> argC) {
		for (Object lclO : argC) {
			if (!contains(lclO)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean addAll(Collection<? extends T> argT) {
		boolean lclResult = false;
		for (T lclT : argT) {
			lclResult |= add(lclT);
		}
		return lclResult;
	}
	
	@Override
	public boolean retainAll(Collection<?> argC) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public boolean removeAll(Collection<?> argC) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void clear() {
		myItems = null;
		mySize = 0;
	}
}
