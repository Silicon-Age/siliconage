package com.opal;

/**
 * @author topquark
 */

public interface OpalFactoryPolymorphicCreator<U extends UserFacing, O extends Opal<U>, T> {
	
	/* This create(...) method is currently only used with SingleTable polymorphism. */
	public O create(T argType);

//	/* This create(...) method is currently only used with Subtable polymorphism. */
//	public O create(U argUF);
	
	/* THINK: Should we split this interface into two different ones, one for each type of Polymorphism?
	 * Can we unify their signatures so that both versions "work the same way"?  Once we figure how to
	 * do heterogeneous polymorphism (i.e., mixing both Subtable and SingleTable, will we need both
	 * of these methods on the same class?
	 */
}
