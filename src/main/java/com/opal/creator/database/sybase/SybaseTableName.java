package com.opal.creator.database.sybase;

import com.opal.creator.database.sqlserver.SQLServerTableName;

public class SybaseTableName extends SQLServerTableName {
	public SybaseTableName(String argDatabaseName, String argOwnerName, String argTableName) {
		super(argDatabaseName, argOwnerName, argTableName);
	}
	
	@Override
	public boolean equals(Object argObject) {
		if (argObject == null) {
			return false;
		}
		if (this == argObject) {
			return true;
		}
		if (this.getClass() != argObject.getClass()) {
			return false;
		}
		
		SybaseTableName that = (SybaseTableName) argObject;
		
		return ((this.getDatabaseName().equals(that.getDatabaseName())) &&
			(this.getOwnerName().equals(that.getOwnerName())) &&
			(this.getTableName().equals(that.getTableName()))
		);
	}
}
