package com.opal.creator.database.sqlserver;

import com.opal.creator.database.Index;

public class SQLServerIndex extends Index {
	private final boolean myPrimaryKey;
	
	public SQLServerIndex(SQLServerTableName argTableName, String argIndexName, boolean argUnique, boolean argPrimaryKey, String argFilter) {
		super(argTableName, argIndexName, argUnique, argFilter);
		
		myPrimaryKey = argPrimaryKey;
	}
	
	public boolean isPrimaryKey() {
		return myPrimaryKey;
	}
}