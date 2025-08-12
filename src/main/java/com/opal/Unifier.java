package com.opal;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Comparator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.OptionalLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class Unifier<U extends IdentityUserFacing> {
	// private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(Unifier.class.getName());
	
	private final Factory<U> myFactory;
	private final U myGood;
	private final Collection<U> myBads;
	
	@SafeVarargs
	public Unifier(Factory<U> argFactory, U argGood, U... argBads) {
		this(argFactory, argGood, Arrays.asList(argBads));
	}
	
	public Unifier(Factory<U> argFactory, U argGood, Collection<U> argBads) {
		super();
		
		myFactory = Validate.notNull(argFactory);
		myGood = Validate.notNull(argGood);
		myBads = new ArrayList<>(Validate.notEmpty(argBads)); // defensive copy
	}
	
	public Factory<U> getFactory() {
		return myFactory;
	}
	
	public U getGood() {
		return myGood;
	}
	
	public Collection<U> getBads() {
		return myBads;
	}
	
	public Unifier<U> firstNonNull(String argFieldName) {
		Validate.notEmpty(argFieldName);
		
		getGood().setField(argFieldName, ObjectUtils.firstNonNull(getGood().getField(argFieldName), getFirstNonNullFromBads(argFieldName)));
		
		return this;
	}
	
	public Unifier<U> firstNonNull(String... argFieldNames) {
		Validate.notEmpty(argFieldNames);
		
		for (String lclFieldName : argFieldNames) {
			firstNonNull(lclFieldName);
		}
		
		return this;
	}
	
	protected Object getFirstNonNullFromBads(String argFieldName) {
		Validate.notEmpty(argFieldName);
		
		for (U lclU : getBads()) {
			Object lclFieldValue = lclU.getField(argFieldName);
			if (lclFieldValue != null) {
				return lclFieldValue;
			}
		}
		return null;
	}
	
	public <R> Unifier<U> firstNonNullReference(Class<R> argType) {
		return firstNonNullReference(argType.getSimpleName(), argType);
	}
	
	public <R> Unifier<U> firstNonNullReference(String argReferenceName, Class<R> argType) {
		return firstReferenceSatisfying(argReferenceName, argType, Objects::nonNull, null);
	}
	
	@SuppressWarnings("unchecked")
	public <R> Unifier<U> firstReferenceSatisfying(String argReferenceName, Class<R> argType, Predicate<R> argCondition, R argValueToUseIfNoneMatch) {
		Validate.notEmpty(argReferenceName);
		Validate.notNull(argType);
		Validate.notNull(argCondition);
		// argValueToUseIfNoneMatch may be null
		
		Method lclAccessor;
		R lclReference = null;
		Method lclMutator;
		try {
			lclAccessor = getGood().getClass().getMethod("get" + argReferenceName);
		} catch (NoSuchMethodException lclNSME) {
			throw new IllegalArgumentException("Could not find accessor for " + argReferenceName + " on " + getGood(), lclNSME);
		}
		Validate.isTrue(lclAccessor.getReturnType() == argType, "Accessor returns " + lclAccessor.getReturnType() + " but we're expecting " + argType);
		
		try {
			lclReference = (R) lclAccessor.invoke(getGood());
		} catch (ClassCastException lclCCE) {
			throw new IllegalArgumentException("Accessor did not return " + argType, lclCCE);
		} catch (InvocationTargetException lclITE) {
			throw new IllegalStateException("Could not get existing " + argReferenceName + " for " + getGood(), lclITE);
		} catch (IllegalAccessException lclIAE) {
			throw new IllegalArgumentException("Could not access existing " + argReferenceName + " for " + getGood(), lclIAE);
		}
		
		if (!argCondition.test(lclReference)) {
			try {
				lclMutator = getGood().getClass().getMethod("set" + argReferenceName, argType);
			} catch (NoSuchMethodException lclNSME) {
				throw new IllegalArgumentException("Could not find mutator for " + argReferenceName + " on " + getGood(), lclNSME);
			}
			
			for (U lclBad : getBads()) {
				R lclBadRef = null;
				try {
					lclBadRef = (R) lclAccessor.invoke(lclBad);
				} catch (ClassCastException lclCCE) {
					throw new IllegalArgumentException("Accessor did not return " + argType, lclCCE);
				} catch (InvocationTargetException lclITE) {
					throw new IllegalStateException("Could not get existing " + argReferenceName + " for " + lclBad, lclITE);
				} catch (IllegalAccessException lclIAE) {
					throw new IllegalArgumentException("Could not access existing " + argReferenceName + " for " + lclBad, lclIAE);
				}
				
				if (argCondition.test(lclBadRef)) {
					try {
						lclMutator.invoke(getGood(), lclBadRef); // which may still be null
						return this;
					} catch (InvocationTargetException lclITE) {
						throw new IllegalStateException("Could not set " + argReferenceName + " on " + getGood() + " to " + lclBadRef, lclITE);
					} catch (IllegalAccessException lclIAE) {
						throw new IllegalStateException("Could not access mutator to set " + argReferenceName + " on " + getGood() + " to " + lclBadRef, lclIAE);
					}
				}
			}
			
			// If we get here, none of them matched
			try {
				lclMutator.invoke(getGood(), argValueToUseIfNoneMatch);
				return this;
			} catch (InvocationTargetException lclITE) {
				throw new IllegalStateException("Could not set " + argReferenceName + " on " + getGood() + " to " + argValueToUseIfNoneMatch, lclITE);
			} catch (IllegalAccessException lclIAE) {
				throw new IllegalStateException("Could not access mutator to set " + argReferenceName + " on " + getGood() + " to " + argValueToUseIfNoneMatch, lclIAE);
			}
		}
		
		return this;
	}
	
	public Unifier<U> concatenate(String argFieldName, String argSeparator) {
		Validate.notEmpty(argFieldName);
		String lclSeparator = StringUtils.trimToEmpty(argSeparator);
		
		Validate.isTrue(getGood().getFieldType(argFieldName) == String.class, "Cannot concatenate non-String fields");
		
		List<String> lclValues = new ArrayList<>(1 + getBads().size());
		
		String lclGoodValue = (String) getGood().getField(argFieldName); // which may be null
		if (lclGoodValue != null) {
			lclValues.add(lclGoodValue);
		}
		lclValues.addAll(
			getBads().stream()
				.map(argBad -> argBad.getField(argFieldName) == null ? null : (String) argBad.getField(argFieldName))
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList())
		);
		
		String lclConcatenated = StringUtils.trimToNull(StringUtils.join(lclValues, lclSeparator));
		
		OptionalLong lclMaxLengthOpt = FieldUtility.getMaximumLength(getGood().getClass(), argFieldName);
		if (lclMaxLengthOpt.isPresent()) {
			long lclMaxLength = lclMaxLengthOpt.getAsLong();
			
			if (lclConcatenated != null && lclConcatenated.length() > lclMaxLength) {
				getGood().setField(argFieldName, lclConcatenated.substring(0, Math.min(Integer.MAX_VALUE, (int) lclMaxLength)));
			} else {
				getGood().setField(argFieldName, lclConcatenated);
			}
		} else {
			getGood().setField(argFieldName, lclConcatenated);
		}
		
		return this;
	}
	
	public Unifier<U> addInts(String argFieldName) {
		return add(argFieldName, 0);
	}
	
	public Unifier<U> add(String argFieldName, int argValueForNulls) {
		Validate.notEmpty(argFieldName);
		
		Validate.isTrue(getGood().getFieldType(argFieldName) == Integer.class, "This method is for adding ints/Integers");
		
		int lclSum;
		
		Integer lclGoodValue = (Integer) getGood().getField(argFieldName);
		int lclGoodIntValue = lclGoodValue == null ? argValueForNulls : lclGoodValue.intValue();
		
		long lclBadSum = getBads().stream()
				.map(argBad -> (Integer) argBad.getField(argFieldName))
				.mapToInt(argValue -> argValue == null ? argValueForNulls : argValue.intValue())
				.sum();
		
		long lclSumGoodAndBad = lclBadSum + lclGoodIntValue;
		
		if (lclSumGoodAndBad > Integer.MAX_VALUE) {
			lclSum = Integer.MAX_VALUE;
		} else {
			lclSum = (int) lclSumGoodAndBad;
		}
		
		getGood().setField(argFieldName, Integer.valueOf(lclSum));
		
		return this;
	}
	
	public Unifier<U> addLongs(String argFieldName) {
		return add(argFieldName, 0L);
	}
	
	public Unifier<U> add(String argFieldName, long argValueForNulls) {
		Validate.notEmpty(argFieldName);
		
		Validate.isTrue(getGood().getFieldType(argFieldName) == Long.class, "This method is for adding longs/Longs");
		
		Long lclGoodValue = (Long) getGood().getField(argFieldName);
		long lclGoodPrimitiveValue = lclGoodValue == null ? argValueForNulls : lclGoodValue.longValue();
		
		long lclBadSum = getBads().stream()
				.map(argBad -> (Long) argBad.getField(argFieldName))
				.mapToLong(argValue -> argValue == null ? argValueForNulls : argValue.longValue())
				.sum();
		
		getGood().setField(argFieldName, Long.valueOf(lclGoodPrimitiveValue + lclBadSum));
		
		return this;
	}
	
	public Unifier<U> addDoubles(String argFieldName) {
		return add(argFieldName, 0.0d);
	}
	
	public Unifier<U> add(String argFieldName, double argValueForNulls) {
		Validate.notEmpty(argFieldName);
		
		Validate.isTrue(getGood().getFieldType(argFieldName) == Double.class, "This method is for adding doubles/Doubles");
		
		Double lclGoodValue = (Double) getGood().getField(argFieldName);
		double lclGoodPrimitiveValue = lclGoodValue == null ? argValueForNulls : lclGoodValue.doubleValue();
		
		double lclBadSum = getBads().stream()
				.map(argBad -> (Double) argBad.getField(argFieldName))
				.mapToDouble(argValue -> argValue == null ? argValueForNulls : argValue.doubleValue())
				.sum();
		
		getGood().setField(argFieldName, Double.valueOf(lclGoodPrimitiveValue + lclBadSum));
		
		return this;
	}
	
	public Unifier<U> add(String argFieldName, float argValueForNulls) {
		Validate.notEmpty(argFieldName);
		
		Validate.isTrue(getGood().getFieldType(argFieldName) == Float.class, "This method is for adding floats/Floats");
		
		Float lclGoodValue = (Float) getGood().getField(argFieldName);
		float lclGoodPrimitiveValue = lclGoodValue == null ? argValueForNulls : lclGoodValue.floatValue();
		
		float lclBadSum = (float) getBads().stream() // FIXME: risk of overflow
				.map(argBad -> (Float) argBad.getField(argFieldName))
				.mapToDouble(argValue -> argValue == null ? argValueForNulls : argValue.doubleValue()) // There is no mapToFloat
				.sum();
		
		getGood().setField(argFieldName, Float.valueOf(lclGoodPrimitiveValue + lclBadSum));
		
		return this;
	}
	
	public Unifier<U> addFloats(String argFieldName) {
		return add(argFieldName, 0.0f);
	}
	
	public Unifier<U> maxOfInts(String argFieldName) {
		return max(argFieldName, 0);
	}
	
	public Unifier<U> max(String argFieldName, int argDefault) {
		Validate.notEmpty(argFieldName);
		
		Integer lclGoodValue = (Integer) getGood().getField(argFieldName);
		int lclGoodPrimitiveValue = lclGoodValue == null ? argDefault : lclGoodValue.intValue();
		
		int lclMaxOfBads = getBads().stream()
				.map(argBad -> (Integer) argBad.getField(argFieldName))
				.mapToInt(argValue -> argValue == null ? argDefault : argValue.intValue())
				.max()
				.orElse(lclGoodPrimitiveValue);
		
		getGood().setField(argFieldName, Integer.valueOf(Math.max(lclMaxOfBads, lclGoodPrimitiveValue)));
		
		return this;
	}
	
	public Unifier<U> maxOfLongs(String argFieldName) {
		return max(argFieldName, 0L);
	}
	
	public Unifier<U> max(String argFieldName, long argDefault) {
		Validate.notEmpty(argFieldName);
		
		Long lclGoodValue = (Long) getGood().getField(argFieldName);
		long lclGoodPrimitiveValue = lclGoodValue == null ? argDefault : lclGoodValue.longValue();
		
		long lclMaxOfBads = getBads().stream()
			.map(argBad -> (Long) argBad.getField(argFieldName))
			.mapToLong(argValue -> argValue == null ? argDefault : argValue.longValue())
			.max()
			.orElse(lclGoodPrimitiveValue);
		
		getGood().setField(argFieldName, Long.valueOf(Math.max(lclMaxOfBads, lclGoodPrimitiveValue)));
		
		return this;
	}
	
	public Unifier<U> maxOfDoubles(String argFieldName) {
		return max(argFieldName, 0.0d);
	}
	
	public Unifier<U> max(String argFieldName, double argDefault) {
		Validate.notEmpty(argFieldName);
		
		Double lclGoodValue = (Double) getGood().getField(argFieldName);
		double lclGoodPrimitiveValue = lclGoodValue == null ? argDefault : lclGoodValue.doubleValue();
		
		double lclMaxOfBads = getBads().stream()
			.map(argBad -> (Double) argBad.getField(argFieldName))
			.mapToDouble(argValue -> argValue == null ? argDefault : argValue.doubleValue())
			.max()
			.orElse(lclGoodPrimitiveValue);
		
		getGood().setField(argFieldName, Double.valueOf(Math.max(lclMaxOfBads, lclGoodPrimitiveValue)));
		
		return this;
	}
	
	public Unifier<U> and(String argFieldName) {
		Validate.notEmpty(argFieldName);
		
		Validate.isTrue(getGood().getFieldType(argFieldName) == Boolean.class, "Cannot perform 'and' on non-Boolean fields");
		
		Collection<U> lclBads = getBads();
		int lclCount = 1 + lclBads.size();
		int lclTrues = 0;
		int lclNulls = 0;
		int lclFalses = 0;
		
		Boolean lclGoodValue = (Boolean) getGood().getField(argFieldName);
		if (lclGoodValue == null) {
			++lclNulls;
		} else if (lclGoodValue.equals(Boolean.TRUE)) {
			++lclTrues;
		} else {
			Validate.isTrue(lclGoodValue.equals(Boolean.FALSE));
			++lclFalses;
		}
		
		for (U lclBad : lclBads) {
			Boolean lclBadValue = (Boolean) lclBad.getField(argFieldName);
			if (lclBadValue == null) {
				++lclNulls;
			} else if (lclBadValue.equals(Boolean.TRUE)) {
				++lclTrues;
			} else {
				Validate.isTrue(lclBadValue.equals(Boolean.FALSE));
				++lclFalses;
			}
		}
		
		Validate.isTrue(lclTrues + lclNulls + lclFalses == lclCount);
		
		if (lclTrues == lclCount) {
			getGood().setField(argFieldName, Boolean.TRUE);
		} else if (lclFalses > 0) {
			getGood().setField(argFieldName, Boolean.FALSE);
		} else {
			getGood().setField(argFieldName, null);
		}
		
		return this;
	}
	
	public Unifier<U> or(String argFieldName) {
		Validate.notEmpty(argFieldName);
		
		Validate.isTrue(getGood().getFieldType(argFieldName) == Boolean.class, "Cannot perform 'or' on non-Boolean fields");
		
		Collection<U> lclBads = getBads();
		int lclCount = 1 + lclBads.size();
		int lclTrues = 0;
		int lclNulls = 0;
		int lclFalses = 0;
		
		Boolean lclGoodValue = (Boolean) getGood().getField(argFieldName);
		if (lclGoodValue == null) {
			++lclNulls;
		} else if (lclGoodValue.equals(Boolean.TRUE)) {
			++lclTrues;
		} else {
			Validate.isTrue(lclGoodValue.equals(Boolean.FALSE));
			++lclFalses;
		}
		
		for (U lclBad : lclBads) {
			Boolean lclBadValue = (Boolean) lclBad.getField(argFieldName);
			if (lclBadValue == null) {
				++lclNulls;
			} else if (lclBadValue.equals(Boolean.TRUE)) {
				++lclTrues;
			} else {
				Validate.isTrue(lclBadValue.equals(Boolean.FALSE));
				++lclFalses;
			}
		}
		
		Validate.isTrue(lclTrues + lclNulls + lclFalses == lclCount);
		
		if (lclTrues > 0) {
			getGood().setField(argFieldName, Boolean.TRUE);
		} else if (lclNulls == 0) {
			getGood().setField(argFieldName, Boolean.FALSE);
		} else {
			getGood().setField(argFieldName, null);
		}
		
		return this;
	}
	
	public Unifier<U> earliestNonNull(String argFieldName) {
		Validate.notEmpty(argFieldName);
		
		if (getGood().getFieldType(argFieldName) == LocalDate.class) {
			LocalDate lclGoodValue = (LocalDate) getGood().getField(argFieldName);
			
			Stream<LocalDate> lclBadValues = getBads().stream()
				.map(argBad -> (LocalDate) argBad.getField(argFieldName));
			
			Stream<LocalDate> lclAllValues = Stream.concat(lclBadValues, Stream.of(lclGoodValue));
			
			LocalDate lclEarliest = lclAllValues
				.filter(Objects::nonNull)
				.sorted()
				.findFirst().orElse(null);
			
			getGood().setField(argFieldName, lclEarliest); // which could still be null, if every value was null
		} else if (getGood().getFieldType(argFieldName) == LocalDateTime.class) {
			LocalDateTime lclGoodValue = (LocalDateTime) getGood().getField(argFieldName);
			
			Stream<LocalDateTime> lclBadValues = getBads().stream()
				.map(argBad -> (LocalDateTime) argBad.getField(argFieldName));
			
			Stream<LocalDateTime> lclAllValues = Stream.concat(lclBadValues, Stream.of(lclGoodValue));
			
			LocalDateTime lclEarliest = lclAllValues
				.filter(Objects::nonNull)
				.sorted()
				.findFirst().orElse(null);
			
			getGood().setField(argFieldName, lclEarliest); // which could still be null, if every value was null
		} else {
			throw new IllegalArgumentException("Can only perform 'earliest' on LocalDate or LocalDateTime fields");
		}
		
		return this;
	}
	
	public Unifier<U> earliestOrNull(String argFieldName) {
		Validate.notEmpty(argFieldName);
		
		if (getGood().getFieldType(argFieldName) == LocalDate.class) {
			LocalDate lclGoodValue = (LocalDate) getGood().getField(argFieldName);
			if (lclGoodValue == null) {
				return this;
			}
			
			List<LocalDate> lclBadValues = getBads().stream()
				.map(argBad -> (LocalDate) argBad.getField(argFieldName))
				.collect(Collectors.toList());
			
			if (lclBadValues.contains(null)) {
				getGood().setField(argFieldName, null);
				return this;
			}
			
			return earliestNonNull(argFieldName);
		} else if (getGood().getFieldType(argFieldName) == LocalDateTime.class) {
			LocalDateTime lclGoodValue = (LocalDateTime) getGood().getField(argFieldName);
			if (lclGoodValue == null) {
				return this;
			}
			
			List<LocalDateTime> lclBadValues = getBads().stream()
				.map(argBad -> (LocalDateTime) argBad.getField(argFieldName))
				.collect(Collectors.toList());
			
			if (lclBadValues.contains(null)) {
				getGood().setField(argFieldName, null);
				return this;
			}
			
			return earliestNonNull(argFieldName);
		} else {
			throw new IllegalArgumentException("Can only perform 'earliest' on LocalDate or LocalDateTime fields");
		}
	}
	
	public Unifier<U> latestNonNull(String argFieldName) {
		Validate.notEmpty(argFieldName);
		
		if (getGood().getFieldType(argFieldName) == LocalDate.class) {
			LocalDate lclGoodValue = (LocalDate) getGood().getField(argFieldName);
			
			Stream<LocalDate> lclBadValues = getBads().stream()
				.map(argBad -> (LocalDate) argBad.getField(argFieldName));
			
			Stream<LocalDate> lclAllValues = Stream.concat(lclBadValues, Stream.of(lclGoodValue));
			
			LocalDate lclLatest = lclAllValues
				.filter(Objects::nonNull)
				.sorted(Comparator.<LocalDate>naturalOrder().reversed())
				.findFirst().orElse(null);
			
			getGood().setField(argFieldName, lclLatest); // which could still be null, if every value was null
		} else if (getGood().getFieldType(argFieldName) == LocalDateTime.class) {
			LocalDateTime lclGoodValue = (LocalDateTime) getGood().getField(argFieldName);
			
			Stream<LocalDateTime> lclBadValues = getBads().stream()
				.map(argBad -> (LocalDateTime) argBad.getField(argFieldName));
			
			Stream<LocalDateTime> lclAllValues = Stream.concat(lclBadValues, Stream.of(lclGoodValue));
			
			LocalDateTime lclLatest = lclAllValues
				.filter(Objects::nonNull)
				.sorted(Comparator.<LocalDateTime>naturalOrder().reversed())
				.findFirst().orElse(null);
			
			getGood().setField(argFieldName, lclLatest); // which could still be null, if every value was null
		} else {
			throw new IllegalArgumentException("Can only perform 'latest' on LocalDate or LocalDateTime fields");
		}
		
		return this;
	}
	
	public Unifier<U> latestOrNull(String argFieldName) {
		Validate.notEmpty(argFieldName);
		
		if (getGood().getFieldType(argFieldName) == LocalDate.class) {
			LocalDate lclGoodValue = (LocalDate) getGood().getField(argFieldName);
			if (lclGoodValue == null) {
				return this;
			}
			
			List<LocalDate> lclBadValues = getBads().stream()
				.map(argBad -> (LocalDate) argBad.getField(argFieldName))
				.collect(Collectors.toList());
			
			if (lclBadValues.contains(null)) {
				getGood().setField(argFieldName, null);
				return this;
			}
			
			return latestNonNull(argFieldName);
		} else if (getGood().getFieldType(argFieldName) == LocalDateTime.class) {
			LocalDateTime lclGoodValue = (LocalDateTime) getGood().getField(argFieldName);
			if (lclGoodValue == null) {
				return this;
			}
			
			List<LocalDateTime> lclBadValues = getBads().stream()
				.map(argBad -> (LocalDateTime) argBad.getField(argFieldName))
				.collect(Collectors.toList());
			
			if (lclBadValues.contains(null)) {
				getGood().setField(argFieldName, null);
				return this;
			}
			
			return latestNonNull(argFieldName);
		} else {
			throw new IllegalArgumentException("Can only perform 'latest' on LocalDate or LocalDateTime fields");
		}
	}
	
	public <C extends UserFacing> Unifier<U> mergeChildren(Class<C> argChildType) {
		return mergeChildren("", argChildType);
	}
	
	@SuppressWarnings("unchecked")
	public <C extends UserFacing> Unifier<U> mergeChildren(String argRolePrefix, Class<C> argChildType) {
		String lclRolePrefix = StringUtils.trimToEmpty(argRolePrefix);
		
		String lclPrefixedBackCollectionName = lclRolePrefix + argChildType.getSimpleName();
		
		for (U lclBad : getBads()) {
			Set<C> lclChildren;
			try {
				Method lclCollectionAccessor = lclBad.getClass().getMethod("get" + lclPrefixedBackCollectionName + "Set");
				
				lclChildren = (Set<C>) lclCollectionAccessor.invoke(lclBad);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException lclE) {
				throw new IllegalStateException("Could not get " + lclPrefixedBackCollectionName + " children of " + lclBad, lclE);
			}
			
			for (C lclChild : lclChildren) {
				OpalUtility.attachChild(lclChild, getGood(), lclPrefixedBackCollectionName);
			}
		}
		
		return this;
	}
	
	public void complete() {
		getBads().stream().forEach(IdentityUserFacing::unlink);
	}
}
