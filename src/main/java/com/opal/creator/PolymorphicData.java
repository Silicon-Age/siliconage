package com.opal.creator;

/**
 * @author topquark
 */
public abstract class PolymorphicData {

	protected PolymorphicData() {
		super();
	}
	
	public abstract boolean requiresTypedCreate();
	
	public abstract MappedClass getUltimateConcreteTypeDeterminer();
	
}
