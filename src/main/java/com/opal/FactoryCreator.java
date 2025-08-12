package com.opal;

/**
 * @author topquark
 */
public interface FactoryCreator<U extends UserFacing> {
	public U create();
}
