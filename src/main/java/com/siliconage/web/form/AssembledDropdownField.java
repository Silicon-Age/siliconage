package com.siliconage.web.form;

import java.util.Comparator;
import java.util.Collections;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;

public class AssembledDropdownField<T extends AssembledDropdownField<?, C>, C> extends StandardDropdownField<T, C> {
	// private static final org.apache.log4j.Logger ourLogger = org.apache.log4j.Logger.getLogger(AssembledDropdownField.class.getName());
	
	private List<C> myChoices;
	private Comparator<? super C> myComparator = null;
	private boolean myDoNotSort = false;
	private DropdownEntryAssembler<C> myAssembler = IdentityNameCodeExtractor.getInstance();
	private Predicate<C> myDisableCondition = x -> false;
	private DropdownEntryAssemblerGroupingSpecification<C, ?> myGroupingSpecification = DropdownEntryAssemblerGroupingSpecification.noGrouping();
	
	public AssembledDropdownField(String argName, Collection<C> argCurrentValues, FormValueProvider argEnteredValueProvider) {
		super(argName, argCurrentValues, argEnteredValueProvider);
	}
	
	public AssembledDropdownField(String argName, C argCurrentValue, FormValueProvider argEnteredValueProvider) {
		this(argName, Collections.singleton(argCurrentValue), argEnteredValueProvider);
	}
	
	public AssembledDropdownField(String argName, C argCurrentValue) {
		this(argName, argCurrentValue, null);
	}
	
	public AssembledDropdownField(String argName) {
		this(argName, null);
	}
	
	public T choices(Collection<C> argChoices) {
		Validate.notNull(argChoices);
		
		myChoices = new ArrayList<>(argChoices);
		
		return castThis();
	}
	
	public T choices(@SuppressWarnings("unchecked") C... argChoices) {
		return choices(Arrays.asList(argChoices));
	}
	
	// This method basically exists to be overridden, as for OpalDropdownField
	protected List<C> determineChoices() {
		Validate.notNull(myChoices);
		return myChoices;
	}
	
	public List<C> getChoices() {
		return myChoices;
	}
	
	public T comparator(Comparator<? super C> argC) {
		myComparator = Validate.notNull(argC);
		
		return castThis();
	}
	
	public Comparator<? super C> getComparator() {
		return myComparator;
	}
	
	public T setDoNotSort(boolean argV) {
		myDoNotSort = argV;
		
		return castThis();
	}
	
	public T doNotSort() {
		return setDoNotSort(true);
	}
	
	public T maySort() {
		return setDoNotSort(false);
	}
	
	public boolean isDoNotSort() {
		return myDoNotSort;
	}
	
	public DropdownEntryAssembler<C> getAssembler() {
		return myAssembler;
	}
	
	public T assembler(DropdownEntryAssembler<C> argAssembler) {
		myAssembler = Validate.notNull(argAssembler);
		
		return castThis();
	}
	
	public T namer(NameCodeExtractor<C> argNCE) {
		return assembler(argNCE);
	}
	
	public T namer(Function<? super C, String> argNamer, Function<? super C, String> argCoder) {
		Validate.notNull(argNamer);
		Validate.notNull(argCoder);
		
		return namer(new FunctionalNameCodeExtractor<C>(argNamer, argCoder));
	}
	
	public <K> T grouper(DropdownEntryAssemblerGroupingSpecification<C, K> argGroupingSpecification) {
		// argGroupingSpecification may be null
		
		myGroupingSpecification = argGroupingSpecification;
		
		return castThis();
	}
	
	public DropdownEntryAssemblerGroupingSpecification<C, ?> getGroupingSpecification() {
		return myGroupingSpecification;
	}
	
	public T disableOptionIf(Predicate<? super C> argCondition) {
		Validate.notNull(argCondition);
		
		myDisableCondition = myDisableCondition.or(argCondition);
		
		return castThis();
	}
	
	protected boolean isDisabled(C argChoice) {
		return myDisableCondition.test(argChoice);
	}
	
	protected boolean isSelected(C argChoice) {
		if (argChoice == null) {
			return false;
		}
		
		if (getEnteredValues() == null) {
			return getSavedValues().contains(argChoice);
		} else if (canPerfectlyDetermineCodes()) {
			String lclCode = tryDeterminingCode(argChoice);
			return getEnteredValues().stream().anyMatch(argEV -> argEV.equals(lclCode));
		} else {
			return getEnteredValues().stream().anyMatch(argEV -> argEV != null && (argEV.hashCode() == argChoice.hashCode() || argEV.toString().equals(argEV.toString())));
		}
	}
	
	@Override
	protected boolean isAnyOptionSelected() {
		if (getPreEntries().stream().anyMatch(argO -> argO instanceof DropdownOption && ((DropdownOption<?>) argO).isSelected())) {
			return true;
		}
		
		if (getEnteredValues() == null) {
			if (!getSavedValues().isEmpty() && !getSavedValues().stream().allMatch(x -> x == null)) {
				return true;
			}
		} else {
			if (!getEnteredValues().isEmpty()) {
				return true;
			}
		}
		
		if (getPostEntries().stream().anyMatch(argO -> argO instanceof DropdownOption && ((DropdownOption<?>) argO).isSelected())) {
			return true;
		}
		
		return false;
	}
	
	protected boolean canPerfectlyDetermineCodes() {
		return getAssembler() instanceof NameCodeExtractor;
	}
	
	protected String tryDeterminingCode(C argChoice) {
		DropdownEntryAssembler<? super C> lclAssembler = getAssembler();
		if (argChoice != null && lclAssembler instanceof NameCodeExtractor) {
			try {
				NameCodeExtractor<? super C> lclNCE = (NameCodeExtractor<? super C>) lclAssembler;
				return lclNCE.extractCode(argChoice);
			} catch (ClassCastException lclE) {
				// Fall through
			}
		}
		
		return argChoice == null ? null : argChoice.toString();
	}
	
	@Override
	public List<DropdownEntry<?>> determineMainEntries() {
		List<C> lclChoices = determineChoices();
		
		if (isDoNotSort()) {
			// Nothing
		} else if (getComparator() == null) {
			try { // See if it's Comparable
				boolean lclHasNull = lclChoices.contains(null);
				
				lclChoices.removeIf(x -> x == null);
				lclChoices.sort(null);
				
				if (lclHasNull) {
					lclChoices.add(0, null); // THINK: Can we get a reasonable way to put it at the end?
				}
			} catch (ClassCastException lclE) {
				// Leave it unsorted
			}
		} else {
			lclChoices.sort(Comparator.nullsFirst(getComparator())); // THINK: Can we get a reasonable way to choose nullsLast()?
		}
		
		return getAssembler().assemble(lclChoices, this::isSelected, this::isDisabled, getGroupingSpecification());
	}
}
