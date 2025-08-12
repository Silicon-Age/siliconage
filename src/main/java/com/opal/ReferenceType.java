package com.opal;

public enum ReferenceType {
	HARD(null),
	SOFT(java.lang.ref.SoftReference.class),
	WEAK(java.lang.ref.WeakReference.class);
	
	private Class<?> myReferenceClass;
	
	private ReferenceType(Class<?> argReferenceClass) { // argReferenceClass might be null
		myReferenceClass = argReferenceClass;
	}
	
	public Class<?> getReferenceClass() {
		return myReferenceClass;
	}
	
}
