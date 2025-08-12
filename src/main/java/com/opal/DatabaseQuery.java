package com.opal;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;

/**
 * @author topquark
 */
public class DatabaseQuery extends Query {
	private final String mySQL;
	private final Object[] myParameters;
	
	public DatabaseQuery(String argSQL) {
		this(argSQL, (Object[]) null);
	}
	
	public DatabaseQuery(String argSQL, Object... argParameters) {
		super();
		
		Validate.notNull(argSQL);
		mySQL = argSQL;
		
		myParameters = argParameters; // Can be null 
	}
	
	public DatabaseQuery(String argSQL, List<Object> argParameters) {
		this(argSQL, argParameters == null ? null : argParameters.toArray(new Object[argParameters.size()]));
	}
	
	public String getSQL() {
		return mySQL;
	}
	
	public Object[] getParameters() {
		return myParameters;
	}
	
	@Override
	public String toString() {
		return getSQL() + (getParameters() != null ? " [" + StringUtils.join(getParameters(), '|') + ']' : "");
	}
	
	public boolean areColumnsInCanonicalOrder() {
		return false;
	}
}
