package com.siliconage.web.form;

public class IdentityNameCodeExtractor<T> extends NullSafeNameCodeExtractor<T> {
	/* This is a singleton class of which only one instance should ever exist.  Clients of this class
	should not create their own instances using a constructor, but should instead invoke the static
	method getInstance() to access the singleton instance. */
	
	/* A static reference to the only instance of this class, which is constructed on class load. */
	private static final IdentityNameCodeExtractor<?> ourInstance = new IdentityNameCodeExtractor<>();
	
	/* A static accessor to obtain a reference to the singleton instance. */

	@SuppressWarnings("unchecked")
	public static final <T> IdentityNameCodeExtractor<T> getInstance() {
		return (IdentityNameCodeExtractor<T>) ourInstance;
	}
	
	@Override
	protected String extractNameInternal(T argT) {
		return String.valueOf(argT);
	}

	@Override
	protected String extractCodeInternal(T argT) {
		return String.valueOf(argT);
	}
}
