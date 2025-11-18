package com.opal;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.opal.cma.validator.FieldValidator;

public interface OpalField<U extends UserFacing, T> {

	public int getIndex();
	
	public String getName();
	
	public Class<T> getType();
	
	public boolean isMutable();
	
	default boolean isNotMutable() {
		return isMutable() == false;
	}
	
	public boolean isNullable();
	
	default boolean isNotNullable() {
		return isNullable() == false;
	}
	
	public Supplier<T> getDefaultSupplier();
	
	public FieldValidator getValidator();
	
	public Function<U, T> getObjectAccessor();
	
	public BiFunction<U, T, ?> getObjectMutator();
	
	public Object getSourceMetadata();

	default T get(U argUF) {
		if (argUF == null) {
			throw new IllegalArgumentException("argUF is null");
		}
		
		return getObjectAccessor().apply(argUF);
	}
	
	default U set(U argUF, T argNewValue) {
		if (argUF == null) {
			throw new IllegalArgumentException("argUF is null");
		}

		var lclMutator = getObjectMutator();
		if (lclMutator == null) {
			throw new UnsupportedOperationException("Cannot call set on a non-updateable OpalField.");
		}
		
		lclMutator.apply(argUF, argNewValue);
		
		return argUF; // Fluent
	}
	// THINK: set-with-conversion?
	
	default DatabaseQuery query(T argSearchValue) {		
		String lclColumnName = (String) getSourceMetadata();
		String lclSQL;
		if (argSearchValue != null) {
			lclSQL = lclColumnName + " = ?";
			Object lclConvertedSearchValue = argSearchValue; // FIXME: What's the proper Opal call for this?
			return new ImplicitTableDatabaseQuery(lclSQL, lclConvertedSearchValue);
		} else {
			lclSQL = lclColumnName + " IS NULL";
			return new ImplicitTableDatabaseQuery(lclSQL);
		}
	}
	
	// Works for null
	default Predicate<U> equalTo(T argSearchValue) {
		return x -> Objects.equals(this.get(x), argSearchValue);
	}
	
	default Predicate<U> matches(Predicate<T> argValuePredicate) {
		return x -> argValuePredicate.test(this.get(x));
	}
}
	
