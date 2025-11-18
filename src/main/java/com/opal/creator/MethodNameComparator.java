package com.opal.creator;

import java.lang.reflect.Method;

import com.siliconage.util.NullSafeComparator;

public final class MethodNameComparator extends NullSafeComparator<Method> {
	
	/* This is a singleton class:  Only one instance may exist at one time.  That requirement is enforced
	by the presence of a private constructor.  To access the instance, use the static getInstance() method.
	The single object is created with lazy instantiation.
	 */
	
	/* The single instance of this class. */
	private static final MethodNameComparator ourInstance = new MethodNameComparator();
	
	/* The public accessor for that single instance */
	public static MethodNameComparator getInstance() {
		return ourInstance;
	}
	
	/* The private constructor that prevents anybody else from creating objects of this class. */
	private MethodNameComparator() {
		super();
	}
	
	@Override
	protected int compareInternal(Method argA, Method argB) {
		/* Sort the Methods by their names. If the names compare as equal, use 
		 * the toGenericString method to come up with some kind of unique,
		 * semi-meaningful representation, and sort based on that.
		 */
		
		int lclNameCompare = argA.getName().compareTo(argB.getName());
		
		if (lclNameCompare != 0) {
			return lclNameCompare;
		}
		
		return argA.toGenericString().compareTo(argB.toGenericString());
	}
}
