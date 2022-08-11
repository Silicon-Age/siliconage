package com.siliconage.util;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Contains methods for cloning objects via reflection.
 * <P>
 * Copyright &copy; 2001 Silicon Age, Inc. All Rights Reserved.
 *
 * @author	<a href="mailto:camfield@silicon-age.com">Chris Mayfield</a>
 * @author	<a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public final class CloneUtility {
	/**
	 * Default CloneUtility constructor.
	 */
	private CloneUtility() {
		super();
	}
	/**
	 * Clones the specified object, by reflection if necessary.
	 * @param     argObject
	 *            The Object to be cloned.
	 * @return    A clone of the Object.
	 * @exception CloneNotSupportedException
	 *            If the Object cannot be cloned.
	 */
	public static Object clone(Object argObject) throws CloneNotSupportedException {
		if (argObject == null) {
			throw new CloneNotSupportedException("Cannot clone a null object");
		}
		
		// Check to see if it implements the PublicCloneable interface
		if (argObject instanceof PublicCloneable) {
			return ((PublicCloneable) argObject).clone();
		}
		
		Exception lclException = null;
		
		Object lclClone = null;
		
		try {
			Method lclCloneMethod = argObject.getClass().getMethod("clone", (Class[]) null);
			
			lclClone = lclCloneMethod.invoke(argObject, (Object[]) null);
		} catch (NoSuchMethodException lclNoSuchMethodException) {
			// Thrown by call to Class.getMethod
			// Method doesn't exist
			lclException = lclNoSuchMethodException;
		} catch (SecurityException lclSecurityException) {
			// Thrown by call to Class.getMethod
			// clone method is protected
			lclException = lclSecurityException;
		} catch (IllegalAccessException lclIllegalAccessException) {
			// Thrown by call to Method.invoke
			// The method is otherwise inaccessible
			lclException = lclIllegalAccessException;
		} catch (IllegalArgumentException lclIllegalArgumentException) {
			// Thrown by call to Method.invoke
			// Object is null or some other error occured within
			// invoke (as opposed to the execution of the reflected
			// method).
			lclException = lclIllegalArgumentException;
		} catch (InvocationTargetException lclInvocationTargetException) {
			// Thrown by call to Method.invoke
			// Exception in the execution of the method
			// This might be a CloneNotSupportedException
			lclException = lclInvocationTargetException;
		}
		
		if (lclException != null) {
			throw new CloneNotSupportedException(
				"Unable to clone Object:\n[" + lclException.getMessage() + ']');
		}
		
		return lclClone;
	}
	
	public static <E> Collection<E> cloneCollection(Collection<E> argCollection) {
		if (argCollection == null) {
			return null;
		}
		Collection<E> lclNew = cloneEmptyCollection(argCollection);
		lclNew.addAll(argCollection);
		return lclNew;
	}
	
	public static <E> Collection<E> cloneEmptyCollection(Collection<E> argCollection) {
		if (argCollection == null) {
			return null;
		}
		try {
			/* TODO:  How can I do this without an unchecked warning ? */
			return argCollection.getClass().getDeclaredConstructor().newInstance();
		} catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException lclE) {
			throw new RuntimeException("Could not instantiate new Collection of type " + argCollection.getClass(), lclE);
		}
	}
}
