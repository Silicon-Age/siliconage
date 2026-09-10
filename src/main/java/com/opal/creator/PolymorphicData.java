package com.opal.creator;

/**
 * @author topquark
 */
public sealed abstract class PolymorphicData permits SingleTablePolymorphicData, SubtablePolymorphicData {

	protected PolymorphicData() {
		super();
	}
	
	public abstract boolean requiresTypedCreate();
	
	public abstract MappedClass getUltimateConcreteTypeDeterminer();
	
}
