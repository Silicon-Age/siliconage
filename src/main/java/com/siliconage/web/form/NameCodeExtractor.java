package com.siliconage.web.form;

import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

/**
 * @author topquark
 * @author jonah
 */
public abstract class NameCodeExtractor<C> implements DropdownEntryAssembler<C> {
	public abstract String extractName(C argObject);
	
	public abstract String extractCode(C argObject);
	
	@Override
	public <K> List<DropdownEntry<?>> assemble(Collection<C> argChoices, Predicate<C> argSelectCondition, Predicate<C> argDisableCondition, DropdownEntryAssemblerGroupingSpecification<C, K> argGrouper) {
		Validate.notNull(argChoices); // but it may be empty
		// argSelectCondition may be null, which will be interpreted as not selecting any
		// argDisableCondition may be null, which will be interpreted as not disabling any
		
		if (argGrouper == null) {
			return argChoices.stream()
				.map(argC -> createOption(argC, argSelectCondition, argDisableCondition))
				.collect(Collectors.toList());
		} else {
			// We really should use a Guava LinkedHashMultimap here, but the Silicon Age project can't depend on Guava.
			LinkedHashMap<K, List<DropdownEntry<?>>> lclGrouped = new LinkedHashMap<>();
			for (C lclC : argChoices) {
				K lclKey = argGrouper.extractKey(lclC); // May be null!
				
				if (!lclGrouped.containsKey(lclKey)) {
					lclGrouped.put(lclKey, new ArrayList<>());
				}
				
				lclGrouped.get(lclKey).add(createOption(lclC, argSelectCondition, argDisableCondition));
			}
			
			List<K> lclKeys = new ArrayList<>(lclGrouped.keySet());
			if (argGrouper.getKeyComparator() != null) {
				lclKeys.sort(Comparator.nullsFirst(argGrouper.getKeyComparator()));
			}
			
			List<DropdownEntry<?>> lclEs = new ArrayList<>(lclGrouped.size());
			
			for (K lclK : lclKeys) {
				if (lclK == null) {
					lclEs.addAll(lclGrouped.get(lclK));
				} else {
					lclEs.add(new DropdownOptionGroup<>(argGrouper.determineLabel(lclK), lclGrouped.get(lclK)));
				}
			}
			
			return lclEs;
		}
	}
	
	protected DropdownOption<?> createOption(C argChoice, Predicate<C> argSelectCondition, Predicate<C> argDisableCondition) {
		// All parameters may be null
		
		DropdownOption<?> lclO = new DropdownOption<>(extractName(argChoice), extractCode(argChoice));
		
		if (argDisableCondition != null && argDisableCondition.test(argChoice)) {
			lclO.disable();
		}
		
		if (argSelectCondition != null && argSelectCondition.test(argChoice)) {
			lclO.selected();
		}
		
		return lclO;
	}
}
