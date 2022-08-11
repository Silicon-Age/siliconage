package com.siliconage.util;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class Fast3Set<T> extends AbstractSet<T> {
	
	private static final org.apache.log4j.Logger ourLogger = org.apache.log4j.Logger.getLogger(Fast3Set.class.getName());

//	private static int ourCount;
//	private static int ourEnlarged;
	
	private static final Constructor<?> ourEnlargementSetConstructor;
	private static final Constructor<?> ourEnlargementSetCapacityConstructor;
	
	static {
		Class<?> lclC;
		try {
			lclC = Class.forName("gnu.trove.set.hash.THashSet");
		} catch (ClassNotFoundException lclE) {
			lclC = HashSet.class;
		}
		assert lclC != null;
		{
			Constructor<?> lclConstructor;
			try {
				lclConstructor = lclC.getConstructor();
			} catch (NoSuchMethodException lclE) {
				lclConstructor = null;
			}
			ourEnlargementSetConstructor = lclConstructor;
			assert ourEnlargementSetConstructor != null;
		}
		{
			Constructor<?> lclConstructor;
			try {
				lclConstructor = lclC.getConstructor(int.class);
			} catch (NoSuchMethodException lclE) {
				lclConstructor = null;
			}
			ourEnlargementSetCapacityConstructor = lclConstructor;
			assert ourEnlargementSetCapacityConstructor != null;
		}
	}
	
	private T myFirst;
	private T mySecond;
	private T myThird;
	private int mySize;
	private Set<T> mySet;
	
	public Fast3Set() {
		super();
//		++ourCount;
	}
	
	public Fast3Set(int argCapacity) {
		this();
		
		if (argCapacity < 0) {
			throw new IllegalArgumentException("argCapacity = " + argCapacity);
		}
		
		if (argCapacity > 3) {
//			++ourEnlarged;
			mySet = createEnlargedSet(argCapacity);
			mySize = -1;
		}
	}
	
	public Fast3Set(Collection<T> argC) {
		this(argC == null ? 3 : argC.size());
		
		if (argC != null) {
			addAll(argC);
		}
	}
	
	protected Set<T> createEnlargedSet(int argCapacity) {
		try {
			@SuppressWarnings("unchecked")
			Set<T> lclReturn = (Set<T>) ourEnlargementSetCapacityConstructor.newInstance(argCapacity);
			return lclReturn;
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException lclE) {
			ourLogger.error("Could not create new enlarged backing set with given capacity for Fast3Set", lclE);
			return new HashSet<>(argCapacity);
		}
	}
	
	protected Set<T> createEnlargedSet() {
		try {
			@SuppressWarnings("unchecked")
			Set<T> lclReturn = (Set<T>) ourEnlargementSetConstructor.newInstance();
			return lclReturn;
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException lclE) {
			ourLogger.error("Could not create new enlarged backing set with default capacity for Fast3Set", lclE);
			return new HashSet<>();
		}
	}
	
	protected Set<T> getSet() {
		return mySet;
	}
	
	protected boolean isEnlarged() {
		return mySet != null;
	}
	
	@Override
	public boolean contains(Object argO) {
		if (isEnlarged()) {
			assert mySize == -1;
			return getSet().contains(argO);
		} else {
			assert mySize >= 0;
			assert mySize <= 3;
			switch (mySize) {
			case 3:
				if (myThird.equals(argO)) {
					return true;
				} /* Fallthrough! */
			case 2:
				if (mySecond.equals(argO)) {
					return true;
				} /* Fallthrough! */
			case 1:
				if (myFirst.equals(argO)) {
					return true;
				} /* Fallthrough! */
			case 0:
				return false;
			default:
				throw new IllegalStateException("mySize = " + mySize);
			}
		}
	}
	
	@Override
	public boolean add(T argT) {
		if (argT == null) {
			throw new IllegalArgumentException("Fast3Set does not allow null elements");
		}
		if (isEnlarged()) {
			assert mySize == -1;
			return getSet().add(argT);
		} else if (mySize == 3) {
			mySet = createEnlargedSet();
			mySet.add(myFirst); myFirst = null;
			mySet.add(mySecond); mySecond = null;
			mySet.add(myThird); myThird = null;
			mySize = -1;
//			++ourEnlarged;
//			ourLogger.warn("Fast3Set Instances = " + ourCount + " Enlarged = " + ourEnlarged);
			return mySet.add(argT);
		} else {
			assert mySize >= 0;
			assert mySize < 3;
			switch (mySize) {
			case 0:
				myFirst = argT;
				mySize++;
				return true;
			case 1:
				if (myFirst.equals(argT)) {
					return false;
				} else {
					mySecond = argT;
					mySize++;
					return true;
				}
			case 2:
				if (myFirst.equals(argT)) {
					return false;
				} else if (mySecond.equals(argT)) {
					return false;
				} else {
					myThird = argT;
					mySize++;
					return true;
				}
			default:
				throw new IllegalStateException("mySize = " + mySize);
			}
		}
	}
	
	@Override
	public boolean remove(Object argT) {
		if (argT == null) {
			return false;
		} else if (isEnlarged()) {
			assert mySize == -1;
			return getSet().remove(argT);
		} else if (mySize == 0) {
			return false;
		} else {
			assert mySize > 0;
			assert mySize <= 3;
			final T lclReplacement;
			switch (mySize) {
			case 3: lclReplacement = myThird; break;
			case 2: lclReplacement = mySecond; break;
			case 1: lclReplacement = null; break;
			default: throw new IllegalStateException();
			}
			
			switch (mySize) {
			case 3:
				if (myThird.equals(argT)) {
					myThird = null;
					--mySize;
					return true;
				}
				/* Fallthrough! */
			case 2:
				if (mySecond.equals(argT)) {
					mySecond = lclReplacement;
					--mySize;
					return true;
				}
				/* Fallthrough! */
			case 1:
				if (myFirst.equals(argT)) {
					myFirst = lclReplacement;
					--mySize;
					return true;
				}
				break;
			default:
				throw new IllegalStateException();
			}
			return false;
		}
	}
	
	@Override
	public Iterator<T> iterator() {
		if (isEnlarged()) {
			assert mySize == -1;
			return getSet().iterator();
		} else {
			assert mySize >= 0;
			assert mySize <= 3;
			return new Fast3SetIterator();
		}
	}
	
	@Override
	public int size() {
		if (isEnlarged()) {
			assert mySize == -1;
			return getSet().size();
		} else {
			assert mySize >= 0;
			assert mySize <= 3;
			return mySize;
		}
	}
	
	@Override
	public void clear() {
		if (isEnlarged()) {
			assert mySize == -1;
			mySet = null;
		} else {
			myFirst = null;
			mySecond = null;
			myThird = null;
		}
		mySize = 0;
	}
	
	protected class Fast3SetIterator implements Iterator<T> {
		private int myIndex;
		private boolean myRemoved;
		
		protected Fast3SetIterator() {
			super();
		}
		
		@Override
		public boolean hasNext() {
			assert !isEnlarged();
			if (myRemoved) {
				return myIndex <= mySize;
			} else {
				return myIndex < mySize;
			}
		}
		
		@Override
		public T next() {
			assert !isEnlarged();
			assert hasNext();
			
			if (myRemoved) {
				myRemoved = false;
			} else {
				++myIndex;
			}
			
			switch (myIndex) {
			case 1:
				assert myFirst != null;
				return myFirst;
			case 2:
				assert mySecond != null;
				return mySecond;
			case 3:
				assert myThird != null;
				return myThird;
			default:
				throw new NoSuchElementException("myIndex = " + myIndex);
			}
		}
		
		@Override
		public void remove() {
			assert !isEnlarged();
			
			if (myRemoved) {
				throw new IllegalStateException("remove() called twice in a row");
			}
			
			assert mySize > 0;
			assert myIndex > 0;
			
			if (myIndex < mySize) {
				final T lclReplacement;
				switch (mySize) {
				case 2: lclReplacement = mySecond; mySecond = null; break; 
				case 3: lclReplacement = myThird; myThird = null; break;
				default: throw new IllegalStateException("Finding replacement; myIndex = " + myIndex + " mySize = " + mySize);
				}
				
				switch (myIndex) {
				case 1: myFirst = lclReplacement; break;
				case 2: mySecond = lclReplacement; break;
				case 3: myThird = null; break;
				default: throw new IllegalStateException("Using replacement; myIndex = " + myIndex + " mySize = " + mySize);
				}
			}
			
			--mySize;
			myRemoved = true;
		}
	}
	
	public static void main(String[] argS) {
		/* Test the Fast3Set */
		
		Fast3Set<Integer> lclS = new Fast3Set<>();
		assert lclS.size() == 0;
		assert lclS.isEmpty();
		assert !lclS.isEnlarged();
		
		lclS.add(Integer.valueOf(1));
		assert lclS.size() == 1;
		assert !lclS.isEnlarged();
		assert !lclS.isEmpty();
		
		lclS.add(Integer.valueOf(2));
		assert lclS.size() == 2;
		assert !lclS.isEnlarged();
		
		lclS.add(Integer.valueOf(1));
		assert lclS.size() == 2;
		assert !lclS.isEnlarged();
		
		lclS.add(Integer.valueOf(2));
		assert lclS.size() == 2;
		assert !lclS.isEnlarged();
		
		lclS.add(Integer.valueOf(3));
		assert lclS.size() == 3;
		assert !lclS.isEnlarged();
		
		lclS.add(Integer.valueOf(4));
		assert lclS.size() == 4;
		assert lclS.isEnlarged();
		assert lclS.mySize == -1;
		
		lclS.remove(Integer.valueOf(1));
		assert lclS.size() == 3;
		assert lclS.isEnlarged();
		
		assert lclS.remove(Integer.valueOf(5)) == false;
		
		lclS.clear();
		assert lclS.size() == 0;
		assert !lclS.isEnlarged();
		
		lclS.add(Integer.valueOf(2));
		assert lclS.remove(Integer.valueOf(3)) == false;
		assert lclS.mySize == 1;
		assert lclS.size() == 1;
		
		assert lclS.remove(Integer.valueOf(2));
		assert lclS.mySize == 0;
		assert lclS.size() == 0;
		
		lclS.add(Integer.valueOf(1));
		lclS.add(Integer.valueOf(3));
		lclS.add(Integer.valueOf(5));
		
		assert lclS.contains(Integer.valueOf(1));
		assert lclS.contains(Integer.valueOf(3));
		assert lclS.contains(Integer.valueOf(5));
		assert !lclS.contains(Integer.valueOf(0));
		assert !lclS.contains(Integer.valueOf(2));
		assert !lclS.contains(Integer.valueOf(100));
		
		assert lclS.remove(Integer.valueOf(1));
		assert lclS.remove(Integer.valueOf(1)) == false;
		assert lclS.contains(Integer.valueOf(3));
		assert lclS.contains(Integer.valueOf(5));
		assert !lclS.contains(Integer.valueOf(1));
		
		assert lclS.remove(Integer.valueOf(3));
		assert lclS.remove(Integer.valueOf(3)) == false;
		assert lclS.contains(Integer.valueOf(5));
		assert !lclS.contains(Integer.valueOf(3));
		assert !lclS.contains(Integer.valueOf(1));
		
		assert lclS.remove(Integer.valueOf(5));
		assert lclS.remove(Integer.valueOf(5)) == false;
		assert !lclS.contains(Integer.valueOf(5));
		assert !lclS.contains(Integer.valueOf(3));
		assert !lclS.contains(Integer.valueOf(1));
		
		assert lclS.size() == 0;
		assert lclS.mySize == 0;
		
		lclS.add(Integer.valueOf(1));
		lclS.add(Integer.valueOf(100));
		lclS.add(Integer.valueOf(200));
		
		Iterator<Integer> lclI = lclS.iterator();
		assert lclI.hasNext();
		assert lclI.next().equals(Integer.valueOf(1));
		assert lclI.hasNext();
		assert lclI.next().equals(Integer.valueOf(100));
		assert lclI.hasNext();
		assert lclI.next().equals(Integer.valueOf(200));
		assert !lclI.hasNext();
		
		Iterator<Integer> lclJ = lclS.iterator();
		assert lclJ.hasNext();
		assert lclJ.next().equals(Integer.valueOf(1));
		assert lclJ.hasNext();
		assert lclJ.next().equals(Integer.valueOf(100));
		assert lclJ.hasNext();
		assert lclJ.next().equals(Integer.valueOf(200));
		assert !lclJ.hasNext();
		
		Iterator<Integer> lclR = lclS.iterator();
		assert lclR.hasNext();
		assert lclR.next().equals(Integer.valueOf(1));
		lclR.remove();
		
		assert lclS.size() == 2;
		assert lclS.mySize == 2;
		assert lclR.hasNext();
		assert lclR.next().equals(Integer.valueOf(200));
		lclR.remove();
		
		assert lclS.size() == 1;
		assert lclS.mySize == 1;
		assert lclR.hasNext();
		assert lclR.next().equals(Integer.valueOf(100));
		lclR.remove();
		
		assert lclS.size() == 0;
		assert lclS.mySize == 0;
		assert !lclR.hasNext();
	}
	
	protected void dump() {
		System.out.println("mySize = " + mySize); // NOPMD by Jonah Greenthal on 9/20/14 11:20 PM
		System.out.println("myFirst = " + myFirst); // NOPMD by Jonah Greenthal on 9/20/14 11:22 PM
		System.out.println("mySecond = " + mySecond); // NOPMD by Jonah Greenthal on 9/20/14 11:22 PM
		System.out.println("myThird = " + myThird); // NOPMD by Jonah Greenthal on 9/20/14 11:22 PM
	}
}
