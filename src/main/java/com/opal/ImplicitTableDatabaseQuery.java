package com.opal;

import java.util.List;

/**
 * @author topquark
 */
public class ImplicitTableDatabaseQuery extends DatabaseQuery {
	/**
	 * @param argSQL The SQL statement to execute, without the SELECT or FROM clauses
	 */
	public ImplicitTableDatabaseQuery(String argSQL) {
		super(argSQL);
	}
	
	/**
	 * @param argSQL The SQL statement to execute, without the SELECT or FROM clauses
	 * @param argParameters The parameters for executing argSQL as a prepared statement
	 */
	public ImplicitTableDatabaseQuery(String argSQL, Object... argParameters) {
		super(argSQL, argParameters);
	}
	
	/**
	 * @param argSQL The SQL statement to execute, without the SELECT or FROM clauses
	 * @param argParameters The parameters for executing argSQL as a prepared statement
	 */
	public ImplicitTableDatabaseQuery(String argSQL, List<Object> argParameters) {
		super(argSQL, argParameters);
	}
	
	@Override
	public boolean areColumnsInCanonicalOrder() {
		return true;
	}
}
