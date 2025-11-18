package com.opal;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.opal.cma.validator.FieldValidator;

public /* value */ class OpalBaseField<U extends UserFacing, T> implements OpalField<U, T> {

	private final int myIndex;
	private final String myName;
	private final Class<T> myType;
	private final boolean myMutable;
	private final boolean myNullable;
	private final Supplier<T> myDefaultSupplier;
	private final FieldValidator myValidator; // FIXME: Should be parameterized
	// Normalizer?
	private final Function<U, T> myObjectAccessor;
	private final BiFunction<U, T, ?> myObjectMutator; // FIXME: Explain ?
	private final Object mySourceMetadata;
	
	public OpalBaseField( // TODO: Can we make this private?
			int argIndex,
			String argName,
			Class<T> argType,
			boolean argMutable,
			boolean argNullable,
			Supplier<T> argDefaultSupplier,
			FieldValidator argValidator,
			Function<U, T> argObjectAccessor,
			BiFunction<U, T, ?> argObjectMutator,
			Object argSourceMetadata) {
		
		super();
		if (argIndex < 0) {
			throw new IllegalArgumentException("argIndex < 0");
		}
		myIndex = argIndex;
		if (argName == null || argName.length() == 0 || (argName.equals(argName.trim()) == false)) {
			throw new IllegalArgumentException("Illegal name");
		}
		myName = argName;
		if (argType == null) {
			throw new IllegalArgumentException("argType == null");
		}
		// TODO: Verify that type is immutable/value-based/whatever.
		myType = argType;
		myMutable = argMutable;
		myNullable = argNullable;
		myDefaultSupplier = argDefaultSupplier; // Might be null
		myValidator = argValidator; // Might be null
		if (argObjectAccessor == null) {
			throw new IllegalArgumentException("argObjectAccessor == null");
		}
		myObjectAccessor = argObjectAccessor;
		myObjectMutator = argObjectMutator; // Might be null for ImmutableOpals
		
		mySourceMetadata = argSourceMetadata; // TODO: Nullable?  Change type.
	}

	/* For non-modifiable fields. */
	public OpalBaseField( // TODO: Can we make this private?
			int argIndex,
			String argName,
			Class<T> argType,
			boolean argNullable,
			Supplier<T> argDefaultSupplier,
			FieldValidator argValidator,
			Function<U, T> argObjectAccessor,
			Object argSourceMetadata) {
		
		this(argIndex, argName, argType, false, argNullable, argDefaultSupplier, argValidator, argObjectAccessor, null, argSourceMetadata);
	}

	public final int getIndex() {
		return myIndex;
	}
	
	public final String getName() {
		return myName;
	}
	
	public final Class<T> getType() {
		return myType;
	}
	
	public final boolean isMutable() {
		return myMutable;
	}
	
	public final boolean isNullable() {
		return myNullable;
	}
	
	public final Supplier<T> getDefaultSupplier() {
		return myDefaultSupplier;
	}
	
	public final FieldValidator getValidator() {
		return myValidator;
	}
	
	public final Function<U, T> getObjectAccessor() {
		return myObjectAccessor;
	}
	
	public final BiFunction<U, T, ?> getObjectMutator() {
		return myObjectMutator;
	}
	
	// THINK: setConvert?
	
	public final Object getSourceMetadata() {
		return mySourceMetadata;
	}
}
	
