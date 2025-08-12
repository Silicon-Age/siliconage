package com.opal.cma;

import com.opal.IdentityUserFacing;

public class NullValidator<U extends IdentityUserFacing> extends Validator<U> {

	/* This is a singleton class of which only one instance should ever exist.  Clients of this class
	should not create their own instances using a constructor, but should instead invoke the static
	method getInstance() to access the singleton instance. */

	/* A static reference to the only instance of this class, which is constructed on class load. */

	private static final NullValidator<?> ourInstance = new NullValidator<>();

	/* A static accessor to obtain a reference to the singleton instance. */

	@SuppressWarnings("unchecked")
	public static final <U extends IdentityUserFacing> NullValidator<U> getInstance() {
		return (NullValidator<U>) ourInstance;
	}

	private NullValidator() {
		super();
	}

}
