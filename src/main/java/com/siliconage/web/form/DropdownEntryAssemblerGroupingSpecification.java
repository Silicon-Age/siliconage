package com.siliconage.web.form;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author jonah
 */
public class DropdownEntryAssemblerGroupingSpecification<C, K> {
	private final Function<C, K> myKeyExtractor;
	private final Comparator<K> myKeyComparator;
	private final Function<K, String> myKeyLabeler;
	
	public DropdownEntryAssemblerGroupingSpecification(Function<C, K> argKeyExtractor, Comparator<K> argKeyComparator, Function<K, String> argKeyLabeler) {
		super();
		
		// Each argument may be null
		
		myKeyExtractor = argKeyExtractor == null ? x -> null : argKeyExtractor; // null -> no grouping
		myKeyComparator = argKeyComparator; // may be null, in which case encounter order will be used
		myKeyLabeler = argKeyLabeler == null ? Object::toString : argKeyLabeler;
	}
	
	public K extractKey(C argC) {
		// argC may be null
		
		return myKeyExtractor.apply(argC); // may be null
	}
	
	public Comparator<K> getKeyComparator() {
		return myKeyComparator;
	}
	
	public String determineLabel(K argK) {
		// argK may be null
		
		return myKeyLabeler.apply(argK); // may be null
	}
	
	private static final DropdownEntryAssemblerGroupingSpecification<?, ?> NO_GROUPING = new DropdownEntryAssemblerGroupingSpecification<>(null, null, null);
	
	@SuppressWarnings("unchecked")
	public static <C> DropdownEntryAssemblerGroupingSpecification<C, ?> noGrouping() {
		return (DropdownEntryAssemblerGroupingSpecification<C, ?>) NO_GROUPING;
	}
}
