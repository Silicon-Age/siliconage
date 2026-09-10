package com.siliconage.util;

public abstract class TypeUtility {
	
	public static String getPrimitiveAccessor(Class<?> argClass) {
		if (argClass == null) {
			throw new IllegalArgumentException("argClass is null");
		}
		
		if (argClass == Integer.class) {
			return "intValue";
		} else if (argClass == Float.class) {
			return "floatValue";
		} else if (argClass == Double.class) {
			return "doubleValue";
		} else if (argClass == Long.class) {
			return "longValue";
		} else if (argClass == Short.class) {
			return "shortValue";
		} else if (argClass == Boolean.class) {
			return "booleanValue";
		} else if (argClass == Byte.class) {
			return "byteValue";
		} else if (argClass == Character.class) {
			return "charValue";
		} else {
			throw new IllegalArgumentException(argClass.getName() + " is not a wrapper for a primitive type.");
		}
	}
	
}
