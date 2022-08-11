package com.siliconage.util;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * @author topquark
 */
public class Pool<E> {
	private final ArrayList<E> myObjects;
	private final Factory<E> myFactory;
	private final Object myFactoryParameters;
	private final int myMaximumSize;
	
	private int myThreadsWaiting;
	private int myCreated = 0;
	private int myCheckedOut = 0;
	private int myReturned = 0;
	
	protected Pool() {
		super();
		myObjects = null;
		myFactory = null;
		myFactoryParameters = null;
		myMaximumSize = 0;
	}
	
	public Pool(Factory<E> argFactory) {
		super();
		myObjects = new ArrayList<>();
		myFactory = argFactory;
		myFactoryParameters = null;
		myMaximumSize = Integer.MAX_VALUE;
	}
	
	public Pool(Factory<E> argFactory, int argMaximumSize) {
		super();
		myObjects = new ArrayList<>();
		if (argFactory == null) {
			throw new IllegalArgumentException("argFactory is null");
		}
		myFactory = argFactory;
		myFactoryParameters = null;
		if (argMaximumSize < 1) {
			throw new IllegalArgumentException("argMaximumSize < 1");
		}
		myMaximumSize = argMaximumSize;
	}
	
	public Pool(Factory<E> argFactory, Object argParameters) {
		super();
		myObjects = new ArrayList<>();
		myFactory = argFactory;
		if (argFactory == null) {
			throw new IllegalArgumentException("argFactory is null");
		}
		myFactoryParameters = argParameters;
		myMaximumSize = Integer.MAX_VALUE;
	}
	
	public E get() throws FactoryException {
		return get(0);
	}
	
	public synchronized E get(int argMillisToWait) throws FactoryException {
		int lclSize;
		while ((lclSize = myObjects.size()) == 0) {
//			ourLogger.debug(this + " was out of pooled objects.");
			if (myCreated < myMaximumSize) {
//				ourLogger.debug("We had only created " + myCreated + " of " + myMaximumSize + ", so we created a new one.");
				myCreated++;
				return myFactory != null ? myFactoryParameters == null ? getFactory().create() : getFactory().create(getFactoryParameters()) : null;
			} else {
				try {
//					ourLogger.debug("We have already created " + myCreated + " so now we need to wait.");
					myThreadsWaiting++;
					
//					long lclStart = System.currentTimeMillis();
					wait(argMillisToWait);
//					long lclEnd = System.currentTimeMillis();
//					ourLogger.debug("Waited " + (lclEnd - lclStart) + " ms for a pooled object.");
					// THINK: How do we know that too much time has elapsed?  We can't just check the
					// while() condition to know that the time expired because we might have been
					// woken up by a notifyAll() when the object was grabbed by another thread
					
//					if (myObjects.size() == 0) {
//						throw new PooledObjectUnavailableException();
//					}
				} catch (InterruptedException lclE) {
					throw new FactoryException("Interrupted while waiting for a pooled object to be returned.", lclE);
				} finally {
					myThreadsWaiting--;
				}
			} 
		} 
//		ourLogger.debug(this + " had " + lclSize + " objects, one of which it is returning.");
		myCheckedOut++;
		return myObjects.remove(lclSize-1);
	}
	
	public synchronized void put(E argObject) {
		if (argObject == null) {
			throw new IllegalArgumentException("argObject is null");
		}
		myObjects.add(argObject);
		if (myObjects.size() > myMaximumSize) {
			throw new IllegalStateException("Returning " + argObject + " produced a total of " + myObjects.size() + " in " + this);
		}
		myReturned++;
//		ourLogger.debug(this + " had an object returned giving it a total of " + myObjects.size());
		notifyAll();
	}
	
	public Factory<E> getFactory() {
		return myFactory;
	}
	
	public Object getFactoryParameters() {
		return myFactoryParameters;
	}
	
	public int getCreated() {
		return myCreated;
	}
	
	public int getCheckedOut() {
		return myCheckedOut;
	}
	
	public int getReturned() {
		return myReturned;
	}
	
	public int getMaximumSize() {
		return myMaximumSize;
	}
	
	public synchronized int getSize() {
		return myObjects.size();
	}
	
	public synchronized int getThreadsWaiting() {
		return myThreadsWaiting;
	}
	
	public void report(PrintWriter argPW) {
		if (argPW != null) {
			argPW.println("Created = " + getCreated() + " Checked Out = " + getCheckedOut() + " Returned = " + getReturned() +
				" Sessions = " + myObjects.size() + " Threads Waiting = " + myThreadsWaiting);
		}
	}
	
	public void report(PrintStream argPS) {
		if (argPS != null) {
			argPS.println("Created = " + getCreated() + " Checked Out = " + getCheckedOut() + " Returned = " + getReturned() +
				" Sessions = " + myObjects.size() + " Threads Waiting = " + myThreadsWaiting);
		}
	}
}
