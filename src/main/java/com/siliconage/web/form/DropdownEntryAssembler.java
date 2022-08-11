package com.siliconage.web.form;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

@FunctionalInterface
public interface DropdownEntryAssembler<C> {
	public <K> List<DropdownEntry<?>> assemble(Collection<C> argChoices, Predicate<C> argSelectCondition, Predicate<C> argDisableCondition, DropdownEntryAssemblerGroupingSpecification<C, K> argGrouper);
}
