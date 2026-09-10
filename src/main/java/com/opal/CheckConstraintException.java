package com.opal;

/**
 * @author topquark
 */

/* This (Runtime) Exception is thrown when an Opal's mutator is called with a value that violates a database-level
 * CHECK constraint that applies only to the column (regardless of whether it was defined on the column or on the
 * table.
 * 
 * At present, this message is generic.  It can be displayed to the user, but it'll really only say that the value
 * was invalid (and reference the name of the Opal field, which may not match the labeling on the form).
 * 
 * That said, throwing these Exceptions earlier prevents from generating database errors (and thus prevents cache
 * corruption).
 */
public class CheckConstraintException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;
	
	public CheckConstraintException() {
		super();
	}
	
	public CheckConstraintException(String argMessage) {
		super(argMessage);
	}
}
