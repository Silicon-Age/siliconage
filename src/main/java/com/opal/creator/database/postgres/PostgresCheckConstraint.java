package com.opal.creator.database.postgres;

import com.opal.creator.database.CheckConstraint;

/**
 * @author jonah
 */
public class PostgresCheckConstraint extends CheckConstraint {
	protected PostgresCheckConstraint(String argName, String argText) {
		super(argName, argText);
	}
	
}