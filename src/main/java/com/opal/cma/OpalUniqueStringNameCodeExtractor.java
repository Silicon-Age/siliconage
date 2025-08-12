package com.opal.cma;

import com.siliconage.web.form.NullSafeNameCodeExtractor;

import com.opal.IdentityUserFacing;

public abstract class OpalUniqueStringNameCodeExtractor<U extends IdentityUserFacing> extends NullSafeNameCodeExtractor<U> {
	protected OpalUniqueStringNameCodeExtractor() {
		super();
	}
	
	@Override
	public String extractCodeInternal(U argU) {
		return argU.getUniqueString();
	}
}
