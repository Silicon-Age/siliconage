package com.opal.creator.database;

/**
 * @author topquark
 * @author jonah
 */
public final class NullDefaultValue extends DefaultValue {
	private static final NullDefaultValue ourInstance = new NullDefaultValue();
	
	public static final NullDefaultValue getInstance() {
		return ourInstance;
	}
	
	private NullDefaultValue() {
		super();
	}
	
	@Override
	public String generateAnnotation(Class<?> argType) {
		return null;
	}
	
	@Override
	public String generateDefinition(Class<?> argType, String argMemberName) {
		return null;
	}
	
	@Override
	public String generateCodeToApply(Class<?> argType, int argFieldIndex, String argDefaultValueMemberName) {
		return null;
	}
}
