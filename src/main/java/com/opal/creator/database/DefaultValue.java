package com.opal.creator.database;

/**
 * @author topquark
 * @author jonah
 */
public abstract class DefaultValue {
	protected DefaultValue() {
		super();
	}
	
	public abstract String generateAnnotation(Class<?> argType);
	
	public abstract String generateDefinition(Class<?> argType, String argMemberName);
	
	public abstract String generateCodeToApply(Class<?> argType, int argFieldIndex, String argDefaultValueMemberName);
}
