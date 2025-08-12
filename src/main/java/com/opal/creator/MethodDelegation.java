package com.opal.creator;

import java.lang.reflect.Type;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

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
		
		Validate.notNull(argClassName);
		myClassName = argClassName;
		
		Validate.notNull(argMethodName);
		myMethodName = argMethodName;
		
		myLocalMethodName = StringUtils.defaultString(argLocalMethodName, argMethodName);
		
		Validate.notNull(argReturnType);
		myReturnType = argReturnType;
		
		myParameters = argParameters; // Might be null
		
		myExceptions = argExceptions; // Might be null
		
		return;
	}
	
//	protected MethodDelegation(String argClassName, String argMethodName, String argReturnTypeName) {
//		this(argClassName, argMethodName, argMethodName, argReturnTypeName);
//	}
	
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
