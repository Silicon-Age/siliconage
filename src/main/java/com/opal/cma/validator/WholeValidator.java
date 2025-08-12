package com.opal.cma.validator;

import java.util.Collection;
import java.util.Map;

import com.opal.UserFacing;

/**
 * @author topquark
 */
@Deprecated
public abstract class WholeValidator<U extends UserFacing> {
	protected WholeValidator() {
		super();
	}
	
	public abstract void validate(String argPrefix, U argUserFacing, Map<String, Object> argParsedValues, Collection<String> argErrors);
}
