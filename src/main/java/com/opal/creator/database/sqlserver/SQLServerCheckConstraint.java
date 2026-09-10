package com.opal.creator.database.sqlserver;

import com.opal.creator.database.CheckConstraint;

/**
 * @author topquark
 */
public class SQLServerCheckConstraint extends CheckConstraint {
	
	protected SQLServerCheckConstraint(String argName, String argText) {
		super(argName, argText);
	}
	
}
