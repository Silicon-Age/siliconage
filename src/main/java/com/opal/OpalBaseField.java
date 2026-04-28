package com.opal;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public /* value */ class OpalBaseField<U extends UserFacing/*<U>*/, T> implements OpalField<U, T> { // OPALFIXME

	private final int myIndex;
	private final String myName;
	private final Class<T> myType;
	private final boolean myMutable;
	private final boolean myNullable;
	private final Supplier<T> myDefaultSupplier;
	private final FieldValidator myValidator; // FIXME: Should be parameterized
	// Normalizer?
	private final Function<U, T> myObjectAccessor;
	private final BiConsumer<U, T> myObjectMutator; // FIXME: Explain ?
	private final Object mySourceMetadata;
	
	public OpalBaseField( // FIXME: Can we make this private?
			int argIndex,
			String argName,
			Class<T> argType,
			boolean argMutable,
			boolean argNullable,
			Supplier<T> argDefaultSupplier,
			FieldValidator argValidator,
			Function<U, T> argObjectAccessor,
			BiConsumer<U, T> argObjectMutator, // Ignores fluency
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

//	public OpalBaseField( // FIXME: Can we make this private?
//			int argIndex,
//			String argName,
//			Class<T> argType,
//			boolean argMutable,
//			boolean argNullable,
//			Supplier<T> argDefaultSupplier,
//			FieldValidator argValidator,
//			Function<U, T> argObjectAccessor,
//			BiConsumer<U, T> argObjectMutator,
//			Object argSourceMetadata) {
//		
//		this(argIndex,
//				argName,
//				argType,
//				argMutable,
//				argNullable,
//				argDefaultSupplier,
//				argValidator,
//				argObjectAccessor,
//				(x, y) -> { argObjectMutator.accept(x, y); return x; },
//				argSourceMetadata
//				);
//		
//	}

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

	@Override
	public final int getIndex() {
		return myIndex;
	}
	
	@Override
	public final String getName() {
		return myName;
	}
	
	@Override
	public final Class<T> getType() {
		return myType;
	}
	
	@Override
	public final boolean isMutable() {
		return myMutable;
	}
	
	@Override
	public final boolean isNullable() {
		return myNullable;
	}
	
	@Override
	public final Supplier<T> getDefaultSupplier() {
		return myDefaultSupplier;
	}
	
	@Override
	public final FieldValidator getValidator() {
		return myValidator;
	}
	
	@Override
	public final Function<U, T> getObjectAccessor() {
		return myObjectAccessor;
	}
	
	@Override
	public final BiConsumer<U, T> getObjectMutator() {
		return myObjectMutator;
	}
	
	// THINK: setConvert?
	
	@Override
	public final Object getSourceMetadata() {
		return mySourceMetadata;
	}
}
	
