package com.opal.cma;

import com.opal.IdentityUserFacing;
import com.opal.types.Named;

public final class DefaultOpalNameCodeExtractor<U extends IdentityUserFacing> extends OpalUniqueStringNameCodeExtractor<U> {
	/* This is a singleton class of which only one instance should ever exist.  Clients of this class
	should not create their own instances using a constructor, but should instead invoke the static
	method getInstance() to access the singleton instance. */
	
	/* A static reference to the only instance of this class, which is constructed on class load. */
	private static final DefaultOpalNameCodeExtractor<? extends IdentityUserFacing> ourInstance = new DefaultOpalNameCodeExtractor<>();
	
	/* A static accessor to obtain a reference to the singleton instance. */
	@SuppressWarnings("unchecked")
	public static final <U extends IdentityUserFacing> DefaultOpalNameCodeExtractor<U> getInstance() {
		return (DefaultOpalNameCodeExtractor<U>) ourInstance;
	}
	
	private DefaultOpalNameCodeExtractor() {
		super();
	}
	
	@Override
	public String extractNameInternal(U argU) {
		if (argU instanceof Named) {
			return ((Named) argU).getName();
		} else {
			throw new IllegalArgumentException(argU + " is not Named");
		}
	}
}
