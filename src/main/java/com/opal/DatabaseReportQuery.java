package com.opal;

import java.util.Objects;

/**
 * @author topquark
 */
public class DatabaseReportQuery {
	private final String mySQL;
	private final String[] myColumnNames;
	
	public DatabaseReportQuery(String argSQL, String[] argColumnNames) {
		mySQL = Objects.requireNonNull(argSQL);
		myColumnNames = argColumnNames; // might be null
	}
	
	public String getSQL() {
		return mySQL;
	}
	
	public String[] getColumnNames() {
		return myColumnNames;
	}
	
	public boolean getDefaultColumnNames() {
		return myColumnNames == null;
	}
}
