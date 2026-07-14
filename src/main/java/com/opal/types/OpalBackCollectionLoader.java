package com.opal.types;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import com.opal.Opal;

public class OpalBackCollectionLoader<C extends Opal<?>, P extends Opal<?>> {

	private final Function<P, Set<C>> myLoader;
	private final ToIntFunction<P> myCounter;
	private final BiConsumer<C, P> mySafeChildMutator;
	private final BiConsumer<C, P> myUnsafeChildMutator;
	private final Function<C, P> myChildAccessor;
	private final Supplier<Set<C>> myDefaultSetSupplier; // No-arg ctor
	private final Function<Collection<C>, Set<C>> myCopySetSupplier; // Set<C> ctor; not really a Supplier
	private final boolean myRemovalAllowed;

	public OpalBackCollectionLoader(Function<P, Set<C>> argLoader, ToIntFunction<P> argCounter, BiConsumer<C, P> argSafeChildMutator, BiConsumer<C, P> argUnsafeChildMutator, Function<C, P> argChildAccessor, Supplier<Set<C>> argDefaultSetSupplier, Function<Collection<C>, Set<C>> argCopySetSupplier, boolean argRemovalAllowed) {
		super();
		
		myLoader = Objects.requireNonNull(argLoader);
		myCounter = Objects.requireNonNull(argCounter);
		mySafeChildMutator = Objects.requireNonNull(argSafeChildMutator);
		myUnsafeChildMutator = Objects.requireNonNull(argUnsafeChildMutator);
		myChildAccessor = Objects.requireNonNull(argChildAccessor);
		myDefaultSetSupplier = Objects.requireNonNull(argDefaultSetSupplier);
		myCopySetSupplier = Objects.requireNonNull(argCopySetSupplier);
		myRemovalAllowed = argRemovalAllowed;
	}
	
	public Function<P, Set<C>> getLoader() {
		return myLoader;
	}
	
	public ToIntFunction<P> getCounter() {
		return myCounter;
	}
	
	public BiConsumer<C, P> getSafeChildMutator() {
		return mySafeChildMutator;
	}
	
	public BiConsumer<C, P> getUnsafeChildMutator() {
		return myUnsafeChildMutator;
	}
	
	public Function<C, P> getChildAccessor() {
		return myChildAccessor;
	}
	
	public Supplier<Set<C>> getDefaultSetSupplier() {
		return myDefaultSetSupplier;
	}
	
	public Function<Collection<C>, Set<C>> getCopySetSupplier() {
		return myCopySetSupplier;
	}
	
	public boolean removalAllowed() {
		return myRemovalAllowed;
	}

}
