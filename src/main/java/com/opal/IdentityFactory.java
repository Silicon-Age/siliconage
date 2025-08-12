package com.opal;

public interface IdentityFactory<U extends IdentityUserFacing> extends Factory<U> {
	public U forUniqueString(String argS);
}
