package com.siliconage.web.form;

import java.util.function.Function;

import org.apache.commons.lang3.Validate;

import com.siliconage.util.Trinary;

public class TrinaryField<T extends TrinaryField<T>> extends AssembledDropdownField<T, Trinary> {
	public TrinaryField(String argName, Trinary argCurrentValue, FormValueProvider argEnteredValueProvider) {
		super(argName, argCurrentValue, argEnteredValueProvider);
		
		choices(Trinary.values());
		comparator(Trinary.StandardComparator.getInstance());
		namer(DefaultTrinaryNameCodeExtractor.getInstance());
	}
	
	public TrinaryField(String argName, Trinary argCurrentValue) {
		this(argName, argCurrentValue, null);
	}
	
	public TrinaryField(String argName) {
		this(argName, null, null);
	}
	
	public T namer(Function<Trinary, String> argNamer) {
		Validate.notNull(argNamer);
		
		return namer(new FunctionalNameCodeExtractor<>(argNamer, DefaultTrinaryNameCodeExtractor.getInstance()::extractCode));
	}
	
	public T namer(String... argLabels) {
		Validate.notNull(argLabels);
		Validate.isTrue(argLabels.length == Trinary.values().length, "List of names must have " + Trinary.values().length + " elements");
		
		return namer(x -> argLabels[x.ordinal()]);
	}
}
