package com.opal.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.Validate;

public class SingletonJavaClass<T> extends JavaClass<T> {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(SingletonJavaClass.class.getName());
	
	private final Method myInstanceAccessor;
	
	protected SingletonJavaClass(String argClassName) throws ClassNotFoundException {
		super(argClassName);
		
		myInstanceAccessor = findInstanceAccessor(null);
	}
	
	protected SingletonJavaClass(String argClassName, String argInstanceAccessorName) throws ClassNotFoundException {
		super(argClassName);
		
		myInstanceAccessor = findInstanceAccessor(Validate.notNull(argInstanceAccessorName, "Could not find instance accessor '" + argInstanceAccessorName + "' for " + argClassName));
		
		return;
	}

	protected Method findInstanceAccessor(String argName) { // null argName means not specified
		Class<T> lclClass = getJavaClass();
		if (lclClass == null) {
			return null;
		}
		boolean lclFound = false;
		Method lclFoundMethod = null;
		for (Method lclM : lclClass.getMethods()) {
			if (argName != null) {
				if (lclM.getName().equals(argName) == false) {
					continue;
				}
			}
			if (Modifier.isStatic(lclM.getModifiers()) == false) {
				continue;
			}
			if (lclM.getReturnType() != lclClass) { /* Note that returning the supertype is not good enough. */
				continue;
			}
			if (lclM.getParameterCount() > 0) {
				continue;
			}
			if (lclFound) {
				throw new IllegalStateException("Found two instance accessor methods");
			}
			lclFound = true;
			lclFoundMethod = lclM;
		}
		
		if (lclFound == false) {
			throw new IllegalStateException("Did not find an instance accessor method in " + lclClass);
		}
		return lclFoundMethod;
	}
	
	public Method getInstanceAccessor() {
		return myInstanceAccessor;
	}
	
	@SuppressWarnings("unchecked")
	public T getInstance() {
		Validate.notNull(getJavaClass(), "Underlying class is null");
		
		Method lclM = Validate.notNull(getInstanceAccessor(), "Could not find instance accessor");
		
		try {
			return (T) lclM.invoke(null);
		} catch (InvocationTargetException | IllegalAccessException lclE) {
			throw new IllegalStateException("Asking for the instance of SingletonJavaClass " + this + " threw an Exception.", lclE);
		}
	}
	
	public static <U> SingletonJavaClass<U> fromSerializedString(String argClassName) {
		try {
			return new SingletonJavaClass<>(argClassName);
		} catch (ClassNotFoundException lclE) {
			ourLogger.warn("Couldn't find {}", argClassName);
			return null;
		}
	}
	
	@Override
	public boolean equals(Object argThat) {
		if (argThat instanceof SingletonJavaClass == false) {
			return false;
		}
		
		@SuppressWarnings("unchecked")
		SingletonJavaClass<T> that = (SingletonJavaClass<T>) argThat;
		return super.equals(that) && this.getInstanceAccessor().equals(that.getInstanceAccessor());
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() & getInstanceAccessor().hashCode();
	}
}
