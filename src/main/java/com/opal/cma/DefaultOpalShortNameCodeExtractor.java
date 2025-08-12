package com.opal.cma;

import com.opal.IdentityUserFacing;
import com.opal.types.ShortNamed;

public final class DefaultOpalShortNameCodeExtractor<U extends IdentityUserFacing> extends OpalUniqueStringNameCodeExtractor<U> {
	/* This is a singleton class of which only one instance should ever exist.  Clients of this class
	should not create their own instances using a constructor, but should instead invoke the static
	method getInstance() to access the singleton instance. */
	
	/* A static reference to the only instance of this class, which is constructed on class load. */
	private static final DefaultOpalShortNameCodeExtractor<? extends IdentityUserFacing> ourInstance = new DefaultOpalShortNameCodeExtractor<>();
	
	/* A static accessor to obtain a reference to the singleton instance. */
	@SuppressWarnings("unchecked")
	public static final <U extends IdentityUserFacing> DefaultOpalShortNameCodeExtractor<U> getInstance() {
		return (DefaultOpalShortNameCodeExtractor<U>) ourInstance;
	}
	
	private DefaultOpalShortNameCodeExtractor() {
		super();
	}
	
	@Override
	public String extractNameInternal(U argU) {
		if (argU instanceof ShortNamed) {
			return ((ShortNamed) argU).getShortName();
		} else {
			throw new IllegalArgumentException(argU + " is not ShortNamed");
		}
	}
}
