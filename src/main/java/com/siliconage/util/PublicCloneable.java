package com.siliconage.util;

/**
 * Interface which publically exposes an object's clone() method.
 * <P>
 * Copyright &copy; 2000, 2001 Silicon Age, Inc. All Rights Reserved.
 *
 * @author	<a href="mailto:matt.mcglincy@silicon-age.com">Matt McGlincy</a>
 * @author	<a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public interface PublicCloneable extends Cloneable {
	/**
	 * Exposes the clone method so that other classes can call it.
	 * @return    A clone of the object.
	 * @exception CloneNotSupportedException
	 *            If the object does not support cloning or cannot be
	 *            cloned.
	 */
	public Object clone() throws CloneNotSupportedException;
}
