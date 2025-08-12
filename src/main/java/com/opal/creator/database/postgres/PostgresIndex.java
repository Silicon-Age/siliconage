package com.opal.creator.database.postgres;

import com.opal.creator.database.Index;

public class PostgresIndex extends Index {
	private final boolean myPrimaryKey;
	
	public PostgresIndex(PostgresTableName argTableName, String argIndexName, boolean argUnique, boolean argPrimaryKey) {
		super(argTableName, argIndexName, argUnique, null); // FIXME: We don't yet support filtered indexes
		myPrimaryKey = argPrimaryKey;
	}
	
	public boolean isPrimaryKey() {
		return myPrimaryKey;
	}
}
