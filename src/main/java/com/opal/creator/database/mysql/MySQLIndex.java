package com.opal.creator.database.mysql;

import com.opal.creator.database.Index;

public class MySQLIndex extends Index {
	private final boolean myPrimaryKey;
	
	public MySQLIndex(MySQLTableName argTableName, String argIndexName, boolean argUnique, boolean argPrimaryKey) {
		super(argTableName, argIndexName, argUnique, null); // null = MySQL opals don't yet support filtered indexes
		myPrimaryKey = argPrimaryKey;
	}
	
	public boolean isPrimaryKey() {
		return myPrimaryKey;
	}
}
