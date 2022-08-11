package com.siliconage.util;

public abstract class TypeUtility {
	
//	public static Class<?> getObjectType(Class<?> argClass) {
//		return ClassUtils.primitiveToWrapper(argClass);
////		Class<?> lclClass = getWrapperTypeOrNull(argClass);
////		return lclClass == null ? argClass : lclClass;
//	}
	
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
	
//	public static Class<?> getPrimitiveTypeOrNull(Class<?> argClass) {
//		if (argClass == null) {
//			throw new IllegalArgumentException("argClass is null");
//		}
//		
//		if (argClass == Integer.class) {
//			return int.class;
//		} else if (argClass == Float.class) {
//			return float.class;
//		} else if (argClass == Double.class) {
//			return double.class;
//		} else if (argClass == Long.class) {
//			return long.class;
//		} else if (argClass == Short.class) {
//			return short.class;
//		} else if (argClass == Boolean.class) {
//			return boolean.class;
//		} else if (argClass == Byte.class) {
//			return byte.class;
//		} else if (argClass == Character.class) {
//			return char.class;
//		} else {
//			return null;
//		}
//	}
	
//	public static Class<?> getWrapperTypeOrNull(Class<?> argClass) {
//		if (argClass == null) {
//			throw new IllegalArgumentException("argClass is null");
//		}
//		
//		if (argClass == int.class) {
//			return Integer.class;
//		} else if (argClass == float.class) {
//			return Float.class;
//		} else if (argClass == double.class) {
//			return Double.class;
//		} else if (argClass == long.class) {
//			return Long.class;
//		} else if (argClass == short.class) {
//			return Short.class;
//		} else if (argClass == boolean.class) {
//			return Boolean.class;
//		} else if (argClass == byte.class) {
//			return Byte.class;
//		} else if (argClass == char.class) {
//			return Character.class;
//		} else {
//			return null;
//		}
//	}
}
