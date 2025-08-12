package com.opal;

/**
 * @author topquark
 */

public interface OpalFactoryCreator<U extends UserFacing, O extends Opal<U>> {
	public O create();
}
