package com.opal;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author topquark
 */
public abstract class AbstractDatabaseQuery extends Query {
	private final String mySQL;
	private final Object[] myParameters;
	
	protected AbstractDatabaseQuery(String argSQL) {
		this(argSQL, (Object[]) null);
	}
	
	protected AbstractDatabaseQuery(String argSQL, Object... argParameters) {
		super();
		
		mySQL = Objects.requireNonNull(argSQL);
		
		myParameters = adjustParameters(argParameters); // Can be null 
	}
	
	private static Object[] adjustParameters(Object[] params) {
		if (params == null) {
			return null;
		}
		Object[] fixed = new Object[params.length];
		for (int i = 0; i < params.length; ++i) {
			Object o = params[i];
			Object fixedParam;
			if (o instanceof IdentityUserFacing iuf) {
				String us = iuf.getUniqueString();
				fixedParam = Integer.valueOf(us); // Could NFE				
			} else {
				fixedParam = o;
			}
			fixed[i] = fixedParam;
		}
		return fixed;
	}
				
	
	protected AbstractDatabaseQuery(String argSQL, List<Object> argParameters) {
		this(argSQL, argParameters == null ? null : argParameters.toArray(Object[]::new));
	}
	
	public String getSQL() {
		return mySQL;
	}
	
	public Object[] getParameters() { // Unsafe to return our internal array.
		return myParameters;
	}
	
	@Override
	public String toString() {
		return getSQL() + (getParameters() != null ? " [" + StringUtils.join(getParameters(), '|') + ']' : "");
	}
	
	public abstract boolean areColumnsInCanonicalOrder();
}
