package com.opal;

import java.util.List;

/**
 * @author topquark
 */
public class DatabaseQuery extends AbstractDatabaseQuery {
	
	public DatabaseQuery(String argSQL) {
		super(argSQL);
	}
	
	public DatabaseQuery(String argSQL, Object... argParameters) {
		super(argSQL, argParameters);
	}
	
	public DatabaseQuery(String argSQL, List<Object> argParameters) {
		super(argSQL, argParameters);
	}

	@Override
	public boolean areColumnsInCanonicalOrder() {
		return false;
	}
}
