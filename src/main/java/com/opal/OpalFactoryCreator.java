package com.opal;

/**
 * @author topquark
 */

public interface OpalFactoryCreator<U extends UserFacing/*<U>*/, O extends Opal<U>> { // OPALFIXME
	public O create();
}
