package com.opal;

/**
 * @author topquark
 */
public interface FactoryPolymorphicCreator<U extends UserFacing, T> {
	public U create(T argT);
}
