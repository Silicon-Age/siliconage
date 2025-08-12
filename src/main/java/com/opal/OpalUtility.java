package com.opal;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

import com.opal.annotation.RequiresActiveTransaction;
import com.opal.types.StringSerializable;
import com.opal.types.UTCDateTime;
import com.siliconage.util.Trinary;

public abstract class OpalUtility {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalUtility.class.getName());
	
	private static final String SQL_TIMESTAMP_FORMAT_NANOS = "yyyy-MM-dd HH:mm:ss.SSSSSSS"; // Actually, hundreds of nanos
	private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER_NANOS = DateTimeFormatter.ofPattern(SQL_TIMESTAMP_FORMAT_NANOS);
	private static final String SQL_TIMESTAMP_FORMAT_SECONDS = "yyyy-MM-dd HH:mm:ss";
	private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER_SECONDS = DateTimeFormatter.ofPattern(SQL_TIMESTAMP_FORMAT_SECONDS);
	private static final String SQL_TIMESTAMP_FORMAT_MINUTES = "yyyy-MM-dd HH:mm";
	private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER_MINUTES = DateTimeFormatter.ofPattern(SQL_TIMESTAMP_FORMAT_MINUTES);
	
	/* package */ static final String[] BOOLEAN_PREFIXES_WITH_UNDERSCORES_UPPERCASE = {"IS_", "ARE_", "HAS_", "ALLOWS_", "MAY_", "MUST_", "CAN_" }; // TODO: display, should?
	/* package */ static final String[] ACCESSOR_PREFIXES = {"get", "is", "are", "has", "allows", "may", "must", "can" };
	
	public static final String getJavadocAuthorLine() {
		return "OPAL database table to Java class mapping system, version 0.3";
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T convertTo(Class<T> argToClass, Object argValue) {
		if (argValue == null) {
			if (argToClass == Trinary.class) {
				return (T) Trinary.UNKNOWN;
			} else {
				return null;
			}
		}
		
		Class<?> lclFromClass = argValue.getClass();
		if (argToClass == lclFromClass) {
			/* This stupid-looking block of code is because the JTDS driver seems to create new Integers instead of
			 * using Integer.valueOf()
			 */
			if (argToClass == Integer.class) {
				return (T) Integer.valueOf(((Integer) argValue).intValue());
			} else {
				return (T) argValue;
			}
		}
		
		// ourLogger.debug("Tricky conversion from " + lclFromClass + " to " + argToClass);
		
		if (argToClass == Float.class) {
			if (lclFromClass == String.class) {
				return (T) Float.valueOf((String) argValue);
			} else {
				return (T) Float.valueOf(((Number) argValue).floatValue());
			}
		} else if (argToClass == Integer.class) {
			if (lclFromClass == String.class) {
				return (T) Integer.valueOf((String) argValue);
			} else {
				return (T) Integer.valueOf(((Number) argValue).intValue());
			}
		} else if (argToClass == Long.class) {
			if (lclFromClass == String.class) {
				return (T) Long.valueOf((String) argValue);
			} else {
				return (T) Long.valueOf(((Number) argValue).longValue());
			}
		} else if (argToClass == Double.class) {
			if (lclFromClass == String.class) {
				return (T) Double.valueOf((String) argValue);
			} else {
				return (T) Double.valueOf(((Number) argValue).doubleValue());
			}
		} else if (argToClass == Boolean.class) {
//			System.out.println("Converting [" + argValue.toString() + "] of class " + argValue.getClass().getName() + " to Boolean.");
//			if (argValue instanceof Number lclNumber1) {
			if (argValue instanceof Number) { // FIXME: Use pattern-matching
				Number lclNumber1 = (Number) argValue;
				return (T) (lclNumber1.intValue() != 0 ? Boolean.TRUE: Boolean.FALSE);
//			} else if (argValue instanceof Trinary lclTrinary) {
			} else if (argValue instanceof Trinary) { // FIXME: Use pattern-matching
				Trinary lclTrinary = (Trinary) argValue;
				return (T) lclTrinary.asBoolean(null);
			} else {
				String lclS = String.valueOf(argValue);
				if (Trinary.UNKNOWN.name().equals(lclS)) {
					return null;
				} else {
					return (T) Boolean.valueOf(lclS);
				}
			}
		} else if (argToClass == Trinary.class) {
//			if (argValue instanceof Number lclNumber2) {
			if (argValue instanceof Number) { // FIXME: Use pattern-matching
				Number lclNumber2 = (Number) (argValue);
				return (T) (lclNumber2.intValue() != 0 ? Trinary.TRUE : Trinary.FALSE);
//			} else if (argValue instanceof Boolean lclBoolean) {
			} else if (argValue instanceof Boolean) {
				Boolean lclBoolean = (Boolean) argValue;
				return (T) (lclBoolean.booleanValue() ? Trinary.TRUE : Trinary.FALSE);
			} else {
				String lclS = String.valueOf(argValue);
				return (T) Trinary.valueOf(lclS);
			}
		} else if (argToClass == Short.class) {
			if (lclFromClass == String.class) {
				return (T) Short.valueOf((String) argValue);
			} else {
				return (T) Short.valueOf(((Number) argValue).shortValue());
			}
		} else if (argToClass == Byte.class) {
			if (lclFromClass == String.class) {
				return (T) Byte.valueOf((String) argValue);
			} else {
				return (T) Byte.valueOf(((Number) argValue).byteValue());
			}
		} else if (argToClass == Character.class) {
			return (T) convertStringToCharacter(String.valueOf(argValue));
		} else if (argToClass == Timestamp.class) {
			if (lclFromClass == java.sql.Date.class) {
				return (T) new Timestamp(((java.sql.Date) argValue).getTime());
			} else if (lclFromClass == String.class) {
				String lclValue = ((String) argValue).trim();
				if (lclValue.length() < "0000-01-01 00:00:00.000000000".length()) {
					lclValue = lclValue + "0000-01-01 00:00:00.000000000".substring(lclValue.length());
				}
				return (T) Timestamp.valueOf(lclValue);
			} else {
				return (T) Timestamp.valueOf(String.valueOf(argValue));
			}
		} else if (argToClass == LocalTime.class) {
			if (lclFromClass == java.sql.Time.class) {
				return (T) ((java.sql.Time) argValue).toLocalTime();
			} else if (lclFromClass == String.class) {
				return (T) LocalTime.parse((String) argValue, DateTimeFormatter.ISO_LOCAL_TIME);
			} else {
				return (T) LocalTime.parse(String.valueOf(argValue),  DateTimeFormatter.ISO_LOCAL_TIME);
			}
		} else if (argToClass == LocalDate.class) {
			if (lclFromClass == java.sql.Date.class) {
				return (T) LocalDateCache.cache((java.sql.Date) argValue);
			} else if (lclFromClass == java.sql.Timestamp.class) {
				return (T) LocalDateCache.cache((java.sql.Timestamp) argValue);
			} else if (lclFromClass == String.class) {
				return (T) LocalDate.parse((String) argValue, DateTimeFormatter.ISO_LOCAL_DATE);
			} else if (lclFromClass == LocalDateTime.class) {
				return (T) ((LocalDateTime) argValue).toLocalDate();
			} else { // THINK: Do we need to include Date or Instant here?
				return (T) LocalDateCache.cache(LocalDate.parse(String.valueOf(argValue), DateTimeFormatter.ISO_LOCAL_DATE));
			}
		} else if (argToClass == LocalDateTime.class) {
			if (lclFromClass == java.sql.Date.class) {
				return (T) LocalDateTime.of(LocalDateCache.cache((java.sql.Date) argValue), LocalTime.MIDNIGHT);
			} else if (lclFromClass == java.sql.Timestamp.class) {
				return (T) LocalDateCache.cacheDateTime((java.sql.Timestamp) argValue);
			} else if (lclFromClass == java.time.OffsetDateTime.class) {
				throw new IllegalStateException();
			} else if (lclFromClass == String.class) {
				String lclValue = (String) argValue;
				if (lclValue.contains("T")) {
					return (T) LocalDateCache.cacheDateTime(LocalDateTime.parse(lclValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
				} else if (lclValue.contains(" ")) {
					if (lclValue.length() > SQL_TIMESTAMP_FORMAT_SECONDS.length()) {
						return (T) LocalDateCache.cacheDateTime(LocalDateTime.parse(lclValue, SQL_TIMESTAMP_FORMATTER_NANOS));
					} else if (lclValue.length() > SQL_TIMESTAMP_FORMAT_MINUTES.length()) {
						return (T) LocalDateCache.cacheDateTime(LocalDateTime.parse(lclValue, SQL_TIMESTAMP_FORMATTER_SECONDS));
					} else {
						return (T) LocalDateCache.cacheDateTime(LocalDateTime.parse(lclValue, SQL_TIMESTAMP_FORMATTER_MINUTES));
					}
				} else {
					return (T) LocalDateCache.cacheDateTime(LocalDate.parse(lclValue, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.MIDNIGHT));
				}
			} else if (lclFromClass == LocalDate.class) {
				return (T) LocalDateTime.of((LocalDate) argValue, LocalTime.MIDNIGHT);
			} else { // THINK: Do we need to include Date or Instant here?
				return (T) LocalDateCache.cacheDateTime(LocalDateTime.parse(String.valueOf(argValue), SQL_TIMESTAMP_FORMATTER_SECONDS));
			}
		} else if (argToClass == UTCDateTime.class) {
			if (lclFromClass == java.sql.Date.class) {
				return (T) UTCDateTime.of(LocalDateTime.of(LocalDateCache.cache((java.sql.Date) argValue), LocalTime.MIDNIGHT));
			} else if (lclFromClass == java.sql.Timestamp.class) {
				return (T) UTCDateTime.of(LocalDateCache.cacheDateTime((java.sql.Timestamp) argValue));
			} else if (lclFromClass == java.time.LocalDate.class) {
				return (T) UTCDateTime.of(LocalDateTime.of((LocalDate) argValue, LocalTime.MIDNIGHT));
			} else if (lclFromClass == java.time.LocalDateTime.class) {
				return (T) UTCDateTime.of((LocalDateTime) argValue);
			} else if (lclFromClass == String.class) {
				String lclValue = (String) argValue;
				if (lclValue.contains("T")) {
					return (T) UTCDateTime.of(LocalDateCache.cacheDateTime(LocalDateTime.parse(lclValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
				} else {
					int lclFirstSpaceIndex = lclValue.indexOf(' ');
					if (lclFirstSpaceIndex >= 0) {
						int lclSecondSpaceIndex = lclValue.lastIndexOf(' ');
						LocalDateTime lclLDT;
//						ZoneOffset lclZO;
						if (lclFirstSpaceIndex == lclSecondSpaceIndex) {
							String lclDateTimeString = lclValue;
							if (lclDateTimeString.length() > SQL_TIMESTAMP_FORMAT_SECONDS.length()) {
								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_NANOS));
							} else if (lclDateTimeString.length() > SQL_TIMESTAMP_FORMAT_MINUTES.length()) {
								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_SECONDS));
							} else {
								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_MINUTES));
							}
//							lclZO = ZoneOffset.UTC;
						} else {
							throw new IllegalStateException();
//							Validate.isTrue(lclSecondSpaceIndex >= 0);
//							Validate.isTrue(lclSecondSpaceIndex > lclFirstSpaceIndex);
//							String lclDateTimeString = lclValue.substring(0, lclSecondSpaceIndex);
//							if (lclDateTimeString.length() > SQL_TIMESTAMP_FORMAT_SECONDS.length()) {
//								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_NANOS));
//							} else if (lclDateTimeString.length() > SQL_TIMESTAMP_FORMAT_MINUTES.length()) {
//								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_SECONDS));
//							} else {
//								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_MINUTES));
//							}
//							String lclZoneOffsetString = lclValue.substring(lclSecondSpaceIndex + 1); // Conceivably an AIOOBException for misformatted data
//							lclZO = ZoneOffset.of(lclZoneOffsetString);
						}
						return (T) UTCDateTime.of(lclLDT/*, lclZO */); 
					} else { /* No space, so it must just be a local date (but no time or offset) */
						return (T) UTCDateTime.of(LocalDateCache.cacheDateTime(LocalDate.parse(lclValue, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.MIDNIGHT)));
					}
				}
			} else if (lclFromClass == LocalDate.class) {
				return (T) UTCDateTime.of(LocalDateTime.of((LocalDate) argValue, LocalTime.MIDNIGHT));
			} else { // THINK: Do we need to include Date or Instant here?
				return (T) UTCDateTime.of(LocalDateCache.cacheDateTime(LocalDateTime.parse(String.valueOf(argValue), SQL_TIMESTAMP_FORMATTER_SECONDS)));
			}
		} else if (argToClass == OffsetDateTime.class) {
			if (lclFromClass == java.sql.Date.class) {
				return (T) OffsetDateTime.of(LocalDateTime.of(LocalDateCache.cache((java.sql.Date) argValue), LocalTime.MIDNIGHT), ZoneOffset.UTC);
			} else if (lclFromClass == java.sql.Timestamp.class) {
				return (T) OffsetDateTime.of(LocalDateCache.cacheDateTime((java.sql.Timestamp) argValue), ZoneOffset.UTC);
			} else if (lclFromClass == java.time.LocalDate.class) {
				return (T) OffsetDateTime.of(LocalDateTime.of((LocalDate) argValue, LocalTime.MIDNIGHT), ZoneOffset.UTC);
			} else if (lclFromClass == java.time.LocalDateTime.class) {
				return (T) OffsetDateTime.of((LocalDateTime) argValue, ZoneOffset.UTC);
			} else if (lclFromClass == String.class) {
				String lclValue = (String) argValue;
				if (lclValue.contains("T")) {
					return (T) OffsetDateTime.of(LocalDateCache.cacheDateTime(LocalDateTime.parse(lclValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME)), ZoneOffset.UTC);
				} else {
					int lclFirstSpaceIndex = lclValue.indexOf(' ');
					if (lclFirstSpaceIndex >= 0) {
						int lclSecondSpaceIndex = lclValue.lastIndexOf(' ');
						LocalDateTime lclLDT;
						ZoneOffset lclZO;
						if (lclFirstSpaceIndex == lclSecondSpaceIndex) {
							String lclDateTimeString = lclValue;
							if (lclDateTimeString.length() > SQL_TIMESTAMP_FORMAT_SECONDS.length()) {
								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_NANOS));
							} else if (lclDateTimeString.length() > SQL_TIMESTAMP_FORMAT_MINUTES.length()) {
								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_SECONDS));
							} else {
								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_MINUTES));
							}
							lclZO = ZoneOffset.UTC;
						} else {
							Validate.isTrue(lclSecondSpaceIndex >= 0);
							Validate.isTrue(lclSecondSpaceIndex > lclFirstSpaceIndex);
							String lclDateTimeString = lclValue.substring(0, lclSecondSpaceIndex);
							if (lclDateTimeString.length() > SQL_TIMESTAMP_FORMAT_SECONDS.length()) {
								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_NANOS));
							} else if (lclDateTimeString.length() > SQL_TIMESTAMP_FORMAT_MINUTES.length()) {
								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_SECONDS));
							} else {
								lclLDT = LocalDateCache.cacheDateTime(LocalDateTime.parse(lclDateTimeString, SQL_TIMESTAMP_FORMATTER_MINUTES));
							}
							String lclZoneOffsetString = lclValue.substring(lclSecondSpaceIndex + 1); // Conceivably an AIOOBException for misformatted data
							lclZO = ZoneOffset.of(lclZoneOffsetString);
						}
						return (T) OffsetDateTime.of(lclLDT, lclZO); 
					} else { /* No space, so it must just be a local date (but no time or offset) */
						return (T) OffsetDateTime.of(LocalDateCache.cacheDateTime(LocalDate.parse(lclValue, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.MIDNIGHT)), ZoneOffset.UTC);
					}
				}
			} else if (lclFromClass == LocalDate.class) {
				return (T) OffsetDateTime.of(LocalDateTime.of((LocalDate) argValue, LocalTime.MIDNIGHT), ZoneOffset.UTC);
			} else { // THINK: Do we need to include Date or Instant here?
				return (T) OffsetDateTime.of(LocalDateCache.cacheDateTime(LocalDateTime.parse(String.valueOf(argValue), SQL_TIMESTAMP_FORMATTER_SECONDS)), ZoneOffset.UTC);
			}
		} else if (argToClass == String.class) {
			if (lclFromClass == java.time.LocalDate.class) {
				return (T) ((LocalDate) argValue).format(DateTimeFormatter.ISO_LOCAL_DATE);
			} else if (lclFromClass == java.time.LocalDateTime.class) {
				return (T) ((LocalDateTime) argValue).format(SQL_TIMESTAMP_FORMATTER_SECONDS);
			} else if (Clob.class.isAssignableFrom(lclFromClass)) {
				Clob lclClob = (Clob) argValue;
				try {long lclLength = lclClob.length();
					if (lclLength > Integer.MAX_VALUE) {
						throw new IllegalStateException("A CLOB with length of " + lclLength + " cannot be represented as a String.");
					}
					return (T) lclClob.getSubString(1, (int) lclLength); // Positions are 1-based
				} catch (SQLException lclE) {
					throw new IllegalStateException("Exception thrown when accessing length and/or content of CLOB while converting it to a String", lclE);
				}
			} else {  
				return (T) String.valueOf(argValue);
			}
		} else if (argToClass == BigDecimal.class) {
			if (lclFromClass == Double.class) {
				return (T) BigDecimal.valueOf((double) argValue);
			} else if (lclFromClass == Long.class) {
				return (T) BigDecimal.valueOf((long) argValue);
			} else if (lclFromClass == String.class) {
				return (T) new BigDecimal((String) argValue);
			} else if (lclFromClass == BigInteger.class) {
				return (T) new BigDecimal((BigInteger) argValue);
			} else if (lclFromClass == Integer.class) {
				return (T) new BigDecimal((int) argValue);
			} else {
				throw new IllegalStateException("Could not convert class " + lclFromClass.getName() + " to a BigDecimal");
			}
//		} else if (argToClass == Trinary.class) {
//			if (lclFromClass == Boolean.class) {
//				return (T) Trinary.valueOf((Boolean) argValue); // Will never return UNKNOWN as null inputs are handled above
//			} else {
//				return (T) Trinary.valueOf(argValue.toString());
//			}
		} else if (StringSerializable.class.isAssignableFrom(argToClass)) {
			String lclValue = argValue.toString();
			try {
				Method lclM = argToClass.getMethod("fromSerializedString", String.class);
				if (lclM == null) {
					throw new IllegalStateException("fromSerializedString(String) does not exist.");
				}
				if (Modifier.isStatic(lclM.getModifiers()) == false) {
					throw new IllegalStateException("fromSerializedString is not static");
				}
				if (argToClass.isAssignableFrom(lclM.getReturnType()) == false) {
					throw new IllegalStateException("fromSerializedString does not return the proper type");
				}
				try {
					return (T) lclM.invoke(null, lclValue);
				} catch (IllegalAccessException | InvocationTargetException lclE) {
					throw new IllegalStateException("Attempting to convert the value \"" + lclValue + "\" using the fromSerializedString factory method on " + argToClass.getName() + " produced an Exception.", lclE);
				}
			} catch (NoSuchMethodException lclE) {
				throw new IllegalStateException("Could not find fromSerializedString method on " + argToClass.getName() + ".");
			}
		} else if (argToClass == Class.class) {
			String lclS = String.valueOf(argValue);
			Class<?> lclC;
			try {
				lclC = Class.forName(lclS);
			} catch (ClassNotFoundException lclE) {
				throw new RuntimeException("Could not find class \"" + lclS + "\".", lclE);
			}
			return (T) lclC;
		} else {
			throw new IllegalStateException("Cannot convert " + lclFromClass + " to " + argToClass);
		}
	}
	
	public static String convertUnderscoreIdentifierToJavaIdentifier(String argS, boolean argInitialCap) {
		StringBuilder lclSB = new StringBuilder(32);
		
		int lclLen = argS.length();
		char[] lclA = new char[lclLen];
		argS.getChars(0, lclLen, lclA, 0);
		
		boolean lclNextUppercase = argInitialCap;
		for(int lclI = 0; lclI < lclLen; ++lclI) {
			char lclC = lclA[lclI];
			if (lclC == '_') {
				lclNextUppercase = true;
			} else {
				lclSB.append(lclNextUppercase ? Character.toUpperCase(lclC) : Character.toLowerCase(lclC));
				lclNextUppercase = false;
			}
		}
		
		return lclSB.toString();
	}
	
	public static String convertUnderscoreIdentifierToJavaIdentifier(String argS) {
		return convertUnderscoreIdentifierToJavaIdentifier(argS, true);
	}
	
	/* THINK:  Perhaps this should go into a Creator-specific utility package */
	public static String getCodeToConvertToObject(Class<?> argType, String argVariableName) {
		if (argType.isPrimitive()) {
			Class<?> lclWrapper = ClassUtils.primitiveToWrapper(argType);
			return lclWrapper.getName() + ".valueOf(" + argVariableName + ')';
		} else {
			return argVariableName;
		}
	}
	
	public static Character convertStringToCharacter(String argString) {
		if (argString == null) {
			return null;
		} else if (argString.length() == 0) {
			return null;
		} else {
			if (argString.length() > 1) {
				ourLogger.warn("Converting string to character with string longer than 1 character: \"" + argString + "\"");
			}
			return Character.valueOf(argString.charAt(0));
		}
	}
	
	public static boolean doesNameSuggestBoolean(String argName) {
		String lclName = argName.toUpperCase();
		for (String lclPrefix : BOOLEAN_PREFIXES_WITH_UNDERSCORES_UPPERCASE) {
			if (lclName.startsWith(lclPrefix)) {
				return true;
			}
		}
		return false;
	}
	
	public static String generateTypeName(Class<?> argClass) {
		return generateTypeName(argClass, true);
	}
	
	public static String generateTypeName(Class<?> argClass, boolean argGeneric) {
		Validate.notNull(argClass);
		
		/* TODO: This won't work for multidimensional arrays */
		if (argClass.isArray()) {
			return argClass.getComponentType().getName() + "[]";
		} else if (argClass == Class.class) {
			if (argGeneric) {
				return argClass.getName() + "<?>";
			} else {
				return argClass.getName();
			}
		} else {
			return argClass.getName();
		}
	}
	
	public static String getCodeToConvert(Class<?> argTo, Class<?> argFrom, String argVariableName, boolean argTrimStrings) {
		Validate.notNull(argTo);
		Validate.notNull(argFrom);
		Validate.notNull(argVariableName);
		
		String lclVariableTrimmed = argTrimStrings ? "org.apache.commons.lang3.StringUtils.trimToNull(" + argVariableName + ")" : argVariableName;
		
		if (argTo == argFrom) {
			return argVariableName;
		}
		if (argTo == Short.class) {
			if (argFrom == String.class) {
				return "java.lang.Short.valueOf(" + lclVariableTrimmed + ")";
			}
		}
		if (argTo == Integer.class) {
			if (argFrom == String.class) {
				return "java.lang.Integer.valueOf(" + lclVariableTrimmed + ")";
			}
		}
		if (argTo == Long.class) {
			if (argFrom == String.class) {
				return "java.lang.Long.valueOf(" + lclVariableTrimmed + ")";
			}
		}
		if (argTo == Timestamp.class) {
			if (argFrom == String.class) {
				return "java.sql.Timestamp.valueOf(" + lclVariableTrimmed + ")";
			}
		}
		if (argTo == LocalDate.class) {
			if (argFrom == String.class) {
				return "java.time.LocalDate.parse(" + lclVariableTrimmed + ", java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)";
			} else if (argFrom == LocalDateTime.class) {
				return argVariableName + " == null ? null : " + argVariableName + ".toLocalDate()";
			}
		}
		if (argTo == String.class) {
			if (argFrom == LocalDate.class) {
				return argVariableName + ".format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)";
			} else {
				return "java.lang.String.valueOf(" + lclVariableTrimmed + ")";
			}
		}
		throw new IllegalArgumentException("Cannot produce code to convert from " + argFrom + " to " +argTo);
	}
	
	public static String generateJavaDeclaration(Type argType) {
		Validate.notNull(argType);
		
		if (argType instanceof Class) { // FIXME: Use pattern-matching (switch pattern-matching when available
			Class<?> lclC = (Class<?>) argType;
			String lclName = lclC.getName().replace('$', '.');
			if (lclC.isArray()) {
				return generateJavaDeclaration(lclC.getComponentType()) + "[]";
			} else {
				Package lclPackage = lclC.getPackage();
				if (lclPackage != null && "java.lang".equals(lclPackage.getName())) {
					return lclName.substring(lclName.lastIndexOf('.') + 1);
				} else {
					return lclName;
				}
			}
		} else if (argType instanceof ParameterizedType) {
			ParameterizedType lclPT = (ParameterizedType) argType;
			StringBuilder lclSB = new StringBuilder();
			if (lclPT.getOwnerType() != null) {
				lclSB.append(generateJavaDeclaration(lclPT.getOwnerType()));
			}
			lclSB.append(generateJavaDeclaration(lclPT.getRawType()));
			Type[] lclArguments = lclPT.getActualTypeArguments();
			if (lclArguments != null && lclArguments.length > 0) {
				lclSB.append('<');
				for (int lclI = 0; lclI < lclArguments.length; ++lclI) {
					if (lclI > 0) {
						lclSB.append(", ");
					}
					lclSB.append(generateJavaDeclaration(lclArguments[lclI]));
				}
				lclSB.append('>');
			}
			return lclSB.toString();
		} else if (argType instanceof GenericArrayType) {
			GenericArrayType lclGA = (GenericArrayType) argType;
			return generateJavaDeclaration(lclGA.getGenericComponentType()) + "[]";
		} else {
			throw new IllegalStateException("Can't handle a Type of " + argType + ".");
		}
	}
	
	@RequiresActiveTransaction
	public static <C extends UserFacing, P extends UserFacing> void attachChild(C argChild, P argParent, String argRolePrefixedBackCollectionName) {
		Validate.notNull(argChild);
		Validate.notNull(argParent);
		Validate.notEmpty(argRolePrefixedBackCollectionName); // This is the name from the parent to the children, like "ChaperoneTeam" for Contact.getChaperoneTeamSet(), *not* "ChaperoneContact" for Team.getChaperoneContact().
		
		Method lclCollectionAccessor = null;
		String lclIdealName = "get" + argRolePrefixedBackCollectionName + "Set";
		for (Method lclM : argParent.getClass().getMethods()) {
			if (lclM.getParameterTypes().length > 0) {
				continue;
			}
			if (Set.class.isAssignableFrom(lclM.getReturnType()) == false) {
				continue;
			}
			String lclMName = lclM.getName();
			if (lclIdealName.equals(lclMName)) {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Found method of name \"" + lclMName + "\" exactly matching ideal name \"" + lclIdealName + "\" for child " + argChild + ", parent " + argParent + ", role = \"" + argRolePrefixedBackCollectionName + "\".");
				}
				lclCollectionAccessor = lclM;
				break;
			}
			if (lclMName.startsWith("get") && lclMName.contains(argRolePrefixedBackCollectionName)) {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Found method of name \"" + lclMName + "\" partly matching ideal name \"" + lclIdealName + "\" for child " + argChild + ", parent " + argParent + ", role = \"" + argRolePrefixedBackCollectionName + "\".");
				}
				if ((lclCollectionAccessor == null) || (lclMName.length() < lclCollectionAccessor.getName().length())) {
					lclCollectionAccessor = lclM; // But don't break!
				}
			}
		}
		if (lclCollectionAccessor == null) {
			throw new IllegalStateException("Could not find method to attach child to parent.");
		}
		try {
			@SuppressWarnings("unchecked")
			Set<C> lclChildSet = (Set<C>) lclCollectionAccessor.invoke(argParent);
			lclChildSet.add(argChild);
		} catch (InvocationTargetException | IllegalAccessException lclE) {
			throw new IllegalStateException("Could not attach child to parent.", lclE);
		}
	}
}
