package com.opal;

public interface IdentityFactory<U extends IdentityUserFacing/*<U>*/> extends Factory<U> { // OPALFIXME
	public U forUniqueString(String argS);
}
