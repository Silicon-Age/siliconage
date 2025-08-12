package com.opal.creator.database.sybase;

import com.opal.creator.database.sqlserver.SQLServerIndex;

public class SybaseIndex extends SQLServerIndex {
	
	public SybaseIndex(SybaseTableName argTableName, String argIndexName, boolean argUnique, boolean argPrimaryKey) {
		super(argTableName, argIndexName, argUnique, argPrimaryKey, null); /* null = no support for filtered indexes */
	}
}
