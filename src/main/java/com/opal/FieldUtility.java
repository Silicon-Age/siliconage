package com.opal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.Objects;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.opal.annotation.Length;
import com.opal.annotation.Nullability;
import com.opal.annotation.Updatability;
import com.opal.annotation.Default;
import com.opal.annotation.CurrentDateDefault;
import com.opal.annotation.CurrentDateTimeDefault;

import com.siliconage.util.Trinary;

public abstract class FieldUtility {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(FieldUtility.class.getName());
	
	public static Class<?> getType(UserFacing argUF, String argFieldName) {
		if (argUF == null) {
			return null;
		} else {
			return getType(argUF.getClass(), argFieldName);
		}
	}
	
	public static Class<?> getType(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		Class<?> lclTypeFromAccessor = getTypeFromAccessor(argUFClass, argFieldName);
		Class<?> lclTypeFromMutator = getTypeFromMutator(argUFClass, argFieldName);
		
		if (lclTypeFromAccessor == null && lclTypeFromMutator == null) {
			ourLogger.warn("Could not determine type of \"" + argFieldName + "\" on " + argUFClass);
			return null;
		} else if (lclTypeFromAccessor != null && lclTypeFromMutator != null) {
			if (lclTypeFromAccessor == lclTypeFromMutator) {
				if (lclTypeFromAccessor == void.class) {
					throw new IllegalStateException("\"" + argFieldName + "\" on " + argUFClass + " seems to be of void type");
				} else {
					return lclTypeFromAccessor;
				}
			} else {
				throw new IllegalStateException("\"" + argFieldName + "\" on " + argUFClass + " has different types according to the accessor and mutator (respectively: " + lclTypeFromAccessor + " and " + lclTypeFromMutator + ")");
			}
		} else {
			return lclTypeFromAccessor == null ? lclTypeFromMutator : lclTypeFromAccessor; // We should use ObjectUtils.firstNonNull, but that involves creating a generic array for varargs, which is warning-worthy
		}
	}
	
	private static Class<?> getTypeFromAccessor(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		Method lclAccessor = getAccessor(argUFClass, argFieldName);
		if (lclAccessor == null) {
			return null;
		} else {
			return lclAccessor.getReturnType();
		}
	}
	
	private static Class<?> getTypeFromMutator(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		Method lclMutator = getMutator(argUFClass, argFieldName);
		if (lclMutator == null) {
			return null;
		} else {
			Class<?>[] lclParameterTypes = lclMutator.getParameterTypes();
			if (lclParameterTypes.length == 1) {
				return lclParameterTypes[0];
			} else {
				throw new IllegalStateException("Mutator for \"" + argFieldName + "\" on " + argUFClass.getName() + " takes " + lclParameterTypes.length + " parameters");
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getValue(UserFacing argUF, String argFieldName) {
		if (argUF == null) {
			return null;
		} else {
			Validate.notBlank(argFieldName);
			
			Method lclAccessor = getAccessor(argUF.getClass(), argFieldName);
			
			if (lclAccessor == null) {
				throw new IllegalStateException("Could not find accessor for \"" + argFieldName + "\" on " + argUF);
			}
			
			try {
				return (T) lclAccessor.invoke(argUF);
			} catch (InvocationTargetException | IllegalAccessException lclE) {
				throw new IllegalStateException("Could not invoke method \"" + lclAccessor + "\" on " + argUF, lclE);
			}
		}
	}
	
	public static Method getAccessor(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		for (String lclPrefix : OpalUtility.ACCESSOR_PREFIXES) {
			Method lclAccessor = MethodUtils.getMatchingAccessibleMethod(argUFClass, lclPrefix + argFieldName + "AsObject");
			if (lclAccessor != null) {
				return lclAccessor;
			}
		}
		
		for (String lclPrefix : OpalUtility.ACCESSOR_PREFIXES) {
			Method lclAccessor = MethodUtils.getMatchingAccessibleMethod(argUFClass, lclPrefix + argFieldName);
			if (lclAccessor != null) {
				return lclAccessor;
			}
		}
		
		return null;
	}
	
	public static Method getMutator(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		List<Method> lclCandidates = new ArrayList<>(2);
		
		for (Method lclM : argUFClass.getMethods()) {
			// Mutators are methods that have no return value or return the type of the object they are in (for fluent mutators) . . .
			if (lclM.getReturnType() == void.class || lclM.getReturnType() == argUFClass) {
				// whose name is "set" followed by the name of the field we want
				if (lclM.getName().equals("set" + argFieldName)) {
					// and that take exactly one argument.
					if (lclM.getParameterTypes().length == 1) {
						lclCandidates.add(lclM);
					}
				}
			}
		}
		
		if (lclCandidates.isEmpty()) {
			return null;
		} else if (lclCandidates.size() == 1) {
			return lclCandidates.get(0);
		} else {
			// Prefer object-taking mutators to primitive mutators
			
			List<Method> lclObjectMutators = lclCandidates.stream()
				.filter(argM -> argM.getParameterTypes()[0].isPrimitive() == false)
				.collect(Collectors.toList());
			if (lclObjectMutators.size() == 1) {
				return lclObjectMutators.get(0);
			} else if (lclObjectMutators.size() > 1) {
				throw new IllegalStateException("Multiple object mutators found for " + argFieldName + " in " + argUFClass + ": " + lclObjectMutators);
			}
			
			List<Method> lclPrimitiveMutators = lclCandidates.stream()
				.filter(argM -> argM.getParameterTypes()[0].isPrimitive())
				.collect(Collectors.toList());
			if (lclPrimitiveMutators.size() == 1) {
				return lclPrimitiveMutators.get(0);
			} else if (lclPrimitiveMutators.size() > 1) {
				throw new IllegalStateException("Multiple primitive mutators found for " + argFieldName + " in " + argUFClass + ": " + lclPrimitiveMutators);
			}
			
			// If we get here, more than one mutator exists but its first (and only) argument type is neither primitive nor object, which makes no sense.
			throw new IllegalStateException("Invalid mutator possibilities for " + argFieldName + " in " + argUFClass + ": " + lclCandidates);
		}
	}
	
	private static Collection<Method> getAccessorAndMutator(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		Method lclAccessor = getAccessor(argUFClass, argFieldName);
		Method lclMutator = getMutator(argUFClass, argFieldName);
		
		Collection<Method> lclMethods = new ArrayList<>(2);
		
		if (lclAccessor != null) {
			lclMethods.add(lclAccessor);
		}
		
		if (lclMutator != null) {
			lclMethods.add(lclMutator);
		}
		
		return lclMethods;
	}
	
	public static <A extends Annotation> A getFieldAnnotation(Class<? extends UserFacing> argUFClass, String argFieldName, Class<A> argAnnotationType) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		Validate.notNull(argAnnotationType);
		
		Collection<Method> lclAM = getAccessorAndMutator(argUFClass, argFieldName);
		
		if (lclAM == null || lclAM.isEmpty()) {
			return null;
		} else {
			return lclAM.stream()
				.map(argMethod -> argMethod.getAnnotation(argAnnotationType))
				.filter(Objects::nonNull)
				.findFirst().orElse(null);
		}
	}
	
	public static Trinary isNullable(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		Nullability lclN = getFieldAnnotation(argUFClass, argFieldName, Nullability.class);
		if (lclN == null) {
			return Trinary.UNKNOWN;
		} else {
			return Trinary.valueOf(lclN.nullable());
		}
	}
	
	public static Trinary isUpdatable(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		Updatability lclU = getFieldAnnotation(argUFClass, argFieldName, Updatability.class);
		if (lclU == null) {
			return Trinary.UNKNOWN;
		} else {
			return Trinary.valueOf(lclU.updatable());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> getDefault(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		{
			Default lclD = getFieldAnnotation(argUFClass, argFieldName, Default.class);
			if (lclD != null) {
				String lclDefaultValueString = lclD.value();
				if (lclDefaultValueString == null) {
					return Optional.ofNullable(null);
				} else {
					return Optional.ofNullable((T) OpalUtility.convertTo(getType(argUFClass, argFieldName), lclDefaultValueString));
				}
			}
		}
		
		{
			CurrentDateDefault lclD = getFieldAnnotation(argUFClass, argFieldName, CurrentDateDefault.class);
			if (lclD != null) {
				return Optional.ofNullable((T) LocalDateCache.today());
			}
		}
		
		{
			CurrentDateTimeDefault lclD = getFieldAnnotation(argUFClass, argFieldName, CurrentDateTimeDefault.class);
			if (lclD != null) {
				return Optional.ofNullable((T) LocalDateCache.now());
			}
		}
		
		return Optional.empty();
	}
	
	public static OptionalLong getMinimumLength(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		// Validate.isTrue(getType(argUFClass, argFieldName) == String.class, "Cannot ask for the minimum length of " + argFieldName + " on " + argUFClass + " because it is not a String");
		
		Length lclL = getFieldAnnotation(argUFClass, argFieldName, Length.class);
		if (lclL == null) {
			return OptionalLong.of(Length.DEFAULT_MINIMUM);
		} else {
			return OptionalLong.of(lclL.minimum());
		}
	}
	
	public static OptionalLong getMaximumLength(Class<? extends UserFacing> argUFClass, String argFieldName) {
		Validate.notNull(argUFClass);
		Validate.notBlank(argFieldName);
		
		// Validate.isTrue(getType(argUFClass, argFieldName) == String.class, "Cannot ask for the maximum length of " + argFieldName + " on " + argUFClass + " because it is not a String");
		
		Length lclL = getFieldAnnotation(argUFClass, argFieldName, Length.class);
		if (lclL == null) {
			return OptionalLong.of(Length.DEFAULT_MAXIMUM);
		} else {
			return OptionalLong.of(lclL.maximum());
		}
	}
	
	/* TODO: This doesn't work for PacketSubset which has createPacketArray which is paired with getPackets(); this method
	 * originally (and with good reason) assumed that createXArray would necessarily have a getXSet, but, in this case, there
	 * was a conflict between get(PacketSet) and get(Packet)Set, so the latter got manually renamed getPackets().
	 */
	public static Collection<Pair<String, Class<? extends UserFacing>>> getChildNamesAndTypes(Class<? extends UserFacing> argUFClass) {
		Validate.notNull(argUFClass);
		
		Collection<Pair<String, Class<? extends UserFacing>>> lclResults = new ArrayList<>();
		for (Method lclM : argUFClass.getMethods()) {
			String lclMethodName = lclM.getName();
			if (lclMethodName.startsWith("create") && lclMethodName.endsWith("Array") && UserFacing[].class.isAssignableFrom(lclM.getReturnType()) && lclM.getParameterCount() == 0) {
				// In other words, this is a createXArray() method that returns a UserFacing[]?  This would be harder with the Set<> child accessors because of type erasure (but see the next method).
				
				String lclChildName = StringUtils.removeStart(lclMethodName, "create");
				lclChildName = StringUtils.removeEnd(lclChildName, "Array");
				
				String lclAttemptedAccessorName = "get" + lclChildName + "Set";
				Method lclM2;
				try {
					lclM2 = argUFClass.getMethod(lclAttemptedAccessorName);
				} catch (NoSuchMethodException lclE) {
					lclM2 = null;
				}
				
				if (lclM2 == null) {
					continue;
				}
				
				Class<?> lclAttemptedAccessorReturnType = lclM2.getReturnType();
				if (Set.class.isAssignableFrom(lclAttemptedAccessorReturnType) == false) {
					continue;
				}
				
				@SuppressWarnings("unchecked")
				Class<? extends UserFacing> lclChildType = (Class<? extends UserFacing>) lclM.getReturnType().getComponentType();
				Validate.notNull(lclChildType);
				
				lclResults.add(Pair.of(lclChildName, lclChildType));
			}
		}
		
		return lclResults;
	}

	public static <P extends UserFacing, C extends UserFacing> Set<C> getChildren(P argParent, String argName) {
		return getChildren(argParent, argName, "get" + argName + "Set");
	}

	// This returns the live Set.
	public static <P extends UserFacing, C extends UserFacing> Set<C> getChildren(P argParent, String argName, String argAccessorName) {
		Validate.notNull(argParent);
		Validate.notBlank(argName);
		
		String lclChildAccessorName = argAccessorName;
		
		try {
			Method lclM = argParent.getClass().getMethod(lclChildAccessorName);
			Validate.isTrue(lclM.getParameterCount() == 0, lclChildAccessorName + " takes " + lclM.getParameterCount() + " parameter(s), but we expect none");
			Validate.isTrue(Set.class.isAssignableFrom(lclM.getReturnType()), lclChildAccessorName + " does not return a Set");
			
			Type lclReturnType = lclM.getGenericReturnType();
			Validate.isTrue(lclReturnType instanceof ParameterizedType, lclChildAccessorName + " does not return a Set<>");
			
			ParameterizedType lclParameterizedReturnType = (ParameterizedType) lclReturnType;
			Type[] lclTypeArguments = lclParameterizedReturnType.getActualTypeArguments();
			Validate.isTrue(lclTypeArguments.length == 1, lclChildAccessorName + " returns a Set with " + lclTypeArguments.length + " type parameters, but there should be one type parameter"); // I assume this is impossible
			
			Type lclChildType = lclTypeArguments[0];
			Validate.isTrue(lclChildType instanceof Class, lclChildAccessorName + " returns a Set<" + lclChildType + ">");
			
			Class<?> lclChildClass = (Class<?>) lclChildType;
			Validate.isTrue(UserFacing.class.isAssignableFrom(lclChildClass), lclChildAccessorName + " returns a Set<" + lclChildClass.getSimpleName() + ">, which parameter is not a UserFacing");
			
			// If we get here, everything should check out for the cast
			Object lclChildren = lclM.invoke(argParent);
			@SuppressWarnings("unchecked")
			Set<C> lclChildrenSet = (Set<C>) lclChildren;
			return lclChildrenSet;
		} catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException lclE) {
			throw new IllegalArgumentException(lclE);
		}
	}
}
