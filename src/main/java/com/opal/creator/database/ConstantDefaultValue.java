package com.opal.creator.database;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.Validate;

import com.opal.OpalUtility;

import com.opal.annotation.Default;
import com.siliconage.util.Trinary;

/**
 * @author topquark
 * @author jonah
 */
public class ConstantDefaultValue extends DefaultValue {
	private final Object myValue;
	
	public ConstantDefaultValue(Object argValue) {
		super();
		
		myValue = argValue;
	}
	
	public Object getValueForClass(Class<?> argClass) {
		return OpalUtility.convertTo(argClass, getValue());
	}
	
	public Object getValue() {
		return myValue;
	}
	
	@Override
	public String toString() {
		if (myValue == null) {
			return "[null]";
		} else if (myValue.getClass() == java.lang.String.class) {
			return "[" + myValue + "]";
		} else {
			return "[" + myValue.getClass() + "/" + myValue.toString() + "]";
		}
	}
	
	@Override
	public String generateAnnotation(Class<?> argType) {
		if (getValue() == null) {
			return null;
		} else {
			return "@" + Default.class.getName() + "(value = \"" + StringEscapeUtils.escapeJava(getValue().toString()) + "\")";
		}
	}
	
	@Override
	public String generateDefinition(Class<?> argType, String argMemberName) {
		Validate.notNull(argType);
		Validate.notBlank(argMemberName);
		
		StringBuilder lclSB = new StringBuilder(80);
		lclSB.append("\tpublic static final " + argType.getName() + " " + argMemberName + " = ");
		if (argType == Integer.class) {
			// THINK: Why is this branch different from all other branches?
			int lclInt;
			if (getValue() instanceof Number) {
				lclInt = ((Number) getValue()).intValue();
			} else {
				try {
					lclInt = Integer.parseInt(getValue().toString());
				} catch (NumberFormatException lclE) {
					throw new IllegalStateException("Cannot generate proper default value.");
				}
			}
			lclSB.append(Integer.class.getName() + ".valueOf(" + lclInt + ")");
		} else if (argType == Long.class) {
			lclSB.append(Long.class.getName() + ".valueOf(" + ((Number) getValue()).longValue() + "L)");
		} else if (argType == Float.class) {
			lclSB.append(Float.class.getName() + ".valueOf(" + ((Number) getValue()).floatValue() + "F)");
		} else if (argType == Double.class) {
			lclSB.append(Double.class.getName() + ".valueOf(" + ((Number) getValue()).doubleValue() + "D)");
		} else if (argType == Byte.class) {
			lclSB.append(Byte.class.getName() + ".valueOf(" + ((Number) getValue()).byteValue() + ")");
		} else if (argType == Short.class) {
			lclSB.append(Short.class.getName() + ".valueOf( (short) " + ((Number) getValue()).shortValue() + ")");
		} else if (argType == BigDecimal.class) {
			lclSB.append(BigDecimal.class.getName() + ".valueOf(" + ((Number) getValue()).doubleValue() + ")");
		} else if (argType == Boolean.class) {
			if (toBoolean(getValue()) == true) {
				lclSB.append(Boolean.class.getName() + ".TRUE");
			} else {
				lclSB.append(Boolean.class.getName() + ".FALSE");
			}
		} else if (argType == Trinary.class) {
			lclSB.append(Trinary.class.getName() + "." + toTrinary(getValue()));
		} else if (argType == Character.class) {
			lclSB.append(Character.class.getName() + ".valueOf('" + getValue().toString().charAt(0) + "')");
		} else if (argType == String.class) {
			lclSB.append('\"' + StringEscapeUtils.escapeJava(getValue().toString()) + '\"');
		} else if (argType == Class.class) {
			lclSB.append(getValue().toString() + ".class");
		} else {
			throw new RuntimeException("Cannot generate proper definition constructor for default value " + this + " with class " + argType.getName() + ".");
		}
		lclSB.append(';');
		return lclSB.toString();
	}
	
	@Override
	public String generateCodeToApply(Class<?> argType, int argFieldIndex, String argDefaultValueMemberName) {
		Validate.isTrue(argFieldIndex >= 0);
		Validate.notBlank(argDefaultValueMemberName);
		
		return "\t\tgetNewValues()[" + argFieldIndex + "] = " + argDefaultValueMemberName + ';';
	}
	
	private static final Collection<String> BOOLEAN_TRUE_STRINGS = Arrays.asList("YES", "Y", "TRUE", "T", "1");
	
	private static boolean toBoolean(Object argO) {
		if (argO == null) {
			return false;
		}
		
		String lclS = StringUtils.trimToEmpty(argO.toString()).toUpperCase();
		return BOOLEAN_TRUE_STRINGS.contains(lclS);
	}

	private static Trinary toTrinary(Object argO) {
		if (argO == null) {
			return Trinary.UNKNOWN;
		}
		return Trinary.valueOf(toBoolean(argO)); 
	}
}
