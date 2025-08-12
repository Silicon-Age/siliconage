package com.opal.types;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.ConstructorUtils;

public class JavaClass<T> implements StringSerializable, Comparable<JavaClass<?>> {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(JavaClass.class.getName());
	
	private final String myClassName;
	private final Class<T> myClass;

	@SuppressWarnings("unchecked")
	protected JavaClass(String argClassName) throws ClassNotFoundException {
		super();
		
		myClassName = Validate.notNull(argClassName);
		
		myClass = (Class<T>) Class.forName(argClassName); // may throw ClassNotFoundException
	}

	public String getClassName() {
		return myClassName;
	}
	
	public Class<T> getJavaClass() {
		return myClass;
	}
	
	public T newInstance(Object... argConstructorParameters) {
		try {
//			System.out.println("Creating class " + getJavaClass());
//			System.out.println("Name = " + getJavaClass().getName());
			return ConstructorUtils.invokeConstructor(getJavaClass(), argConstructorParameters);
		} catch (NoSuchMethodException lclNSME) {
			throw new IllegalStateException("Could not find appropriate constructor on " + this, lclNSME);
		} catch (IllegalAccessException lclIAE) {
			throw new IllegalStateException("Could not access appropriate constructor on " + this, lclIAE);
		} catch (InvocationTargetException lclITE) {
			throw new IllegalStateException("Could not invoke constructor on " + this, lclITE);
		} catch (InstantiationException lclIE) {
			throw new IllegalStateException("Could not instantiate " + this, lclIE);
		}
	}
	
	@Override
	public String toString() {
		return getClassName();
	}
	
	@Override
	public String toSerializedString() {
		return getClassName();
	}
	
	public static <U> JavaClass<U> fromSerializedString(String argClassName) {
		try {
			return new JavaClass<>(argClassName);
		} catch (ClassNotFoundException lclE) {
			ourLogger.warn("Couldn't find {}", argClassName);
			return null;
		}
	}
	
	@Override
	public boolean equals(Object that) {
		if (that == null) {
			return false;
		}
		if (that.getClass() != this.getClass()) {
			return false;
		}
		JavaClass<?> lclThat = (JavaClass<?>) that;
		return this.getClassName().equals(lclThat.getClassName());
	}
	
	@Override
	public int hashCode() {
		return getClassName().hashCode();
	}
	
	@Override
	public int compareTo(JavaClass<?> that) {
		return that != null ? this.getClassName().compareTo(that.getClassName()) : +1;
	}
}
