package com.opal;

/**
 * @author topquark
 */
public interface FactoryPolymorphicCreator<U extends UserFacing, T> { // OPALFIXME
	public U create(T argT);
}
