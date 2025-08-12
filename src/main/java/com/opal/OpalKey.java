package com.opal;

/* This SuppressWarnings annotation is for the type variable O, which I can't seem to get rid without causing
 * compilation errors.
 */
public abstract class OpalKey<O extends Opal<? extends UserFacing>> {

//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalKey.class.getName());
	
	protected OpalKey() {
		super();
	}
	
}
