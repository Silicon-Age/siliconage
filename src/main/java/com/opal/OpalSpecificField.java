package com.opal;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.opal.cma.validator.FieldValidator;

public abstract class OpalSpecificField <U extends UserFacing, T> implements OpalField<U, T> {
	
	private final OpalBaseField<U, T> myBaseField;
	
	protected OpalSpecificField(OpalBaseField<U, T> argBaseField) {
		super();

		if (argBaseField == null) {
			throw new IllegalArgumentException("argBaseField is null");
		}
		myBaseField = argBaseField;
	}
	
	protected OpalBaseField<U, T> getBaseField() {
		return myBaseField;
	}

	@Override
	public final int getIndex() {
		return getBaseField().getIndex();
	}

	@Override
	public final String getName() {
		return getBaseField().getName();
	}

	@Override
	public final Class<T> getType() {
		return getBaseField().getType();
	}

	@Override
	public final boolean isMutable() {
		return getBaseField().isMutable();
	}

	@Override
	public final boolean isNullable() {
		return getBaseField().isNullable();
	}

	@Override
	public final Supplier<T> getDefaultSupplier() {
		return getBaseField().getDefaultSupplier();
	}

	@Override
	public final FieldValidator getValidator() {
		return getBaseField().getValidator();
	}

	@Override
	public final Function<U, T> getObjectAccessor() {
		return getBaseField().getObjectAccessor();
	}

	@Override
	public final BiFunction<U, T, ?> getObjectMutator() {
		return getBaseField().getObjectMutator();
	}

	@Override
	public final Object getSourceMetadata() {
		return getBaseField().getSourceMetadata();
	}
}
