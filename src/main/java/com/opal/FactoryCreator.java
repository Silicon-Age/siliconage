package com.opal;

/**
 * @author topquark
 */
public interface FactoryCreator<U extends UserFacing> { // OPALFIXME
	public U create();
}
