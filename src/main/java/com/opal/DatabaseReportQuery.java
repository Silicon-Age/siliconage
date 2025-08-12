package com.opal;

import org.apache.commons.lang3.Validate;

/**
 * @author topquark
 */
public class DatabaseReportQuery {
	private final String mySQL;
	private final String[] myColumnNames;
	
	public DatabaseReportQuery(String argSQL, String[] argColumnNames) {
		Validate.notNull(argSQL);
		// argColumnNames may be null
		
		mySQL = argSQL;
		myColumnNames = argColumnNames;
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
