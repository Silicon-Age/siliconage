package com.opal.creator;

import java.lang.reflect.Type;
import java.util.Objects;

public class MethodDelegation {
	private final String myClassName;
	private final String myMethodName;
	private final Type myReturnType;
	private final String myLocalMethodName;
	private final Type[] myParameters;
	private final Type[] myExceptions;
	
	protected MethodDelegation(String argClassName, String argMethodName, String argLocalMethodName, Type argReturnType) {
		this(argClassName, argMethodName, argLocalMethodName, argReturnType, null, null);
	}
	
	protected MethodDelegation(String argClassName, String argMethodName, String argLocalMethodName, Type argReturnType, Type[] argParameters) {
		this (argClassName, argMethodName, argLocalMethodName, argReturnType, argParameters, null);
	}
	
	protected MethodDelegation(String argClassName, String argMethodName, String argLocalMethodName, Type argReturnType, Type[] argParameters, Type[] argExceptions) {
		super();
		
		Objects.requireNonNull(argClassName);
		myClassName = argClassName;
		
		Objects.requireNonNull(argMethodName);
		myMethodName = argMethodName;
		
		myLocalMethodName = Objects.requireNonNullElse(argLocalMethodName, argMethodName); // I hate this method name.
		
		Objects.requireNonNull(argReturnType);
		myReturnType = argReturnType;
		
		myParameters = argParameters; // Might be null
		
		myExceptions = argExceptions; // Might be null
		
		return;
	}
	
	public String getClassName() {
		return myClassName;
	}
	
	public String getMethodName() {
		return myMethodName;
	}
	
	public Type getReturnType() {
		return myReturnType;
	}
	
	public String getLocalMethodName() {
		return myLocalMethodName;
	}
	
	public Type[] getParameters() {
		return myParameters;
	}
	
	public Type[] getExceptions() {
		return myExceptions;
	}
}
