package com.opal.creator.database.sqlserver;

import com.opal.creator.database.TableName;

import java.util.Objects;

public class SQLServerTableName extends TableName {
	private final String myDatabaseName;
	private final String myOwnerName;
	private final String myTableName;
	
	public SQLServerTableName(String argDatabaseName, String argOwnerName, String argTableName) {
		super();
		
		myDatabaseName = Objects.requireNonNull(argDatabaseName);
		myOwnerName = Objects.requireNonNull(argOwnerName);
		myTableName = Objects.requireNonNull(argTableName);
		
		return;
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
		
		SQLServerTableName that = (SQLServerTableName) argObject;
		
		return ((this.getDatabaseName().equals(that.getDatabaseName())) &&
			(this.getOwnerName().equals(that.getOwnerName())) &&
			(this.getTableName().equals(that.getTableName()))
		);
	}
	public String getDatabaseName() {
		return myDatabaseName;
	}
	
	@Override
	public String getFullyQualifiedTableName() {
		return getDatabaseName() + '.' + getOwnerName() + '.' + getTableName();
	}
	
	public String getOwnerName() {
		return myOwnerName;
	}
	
	@Override
	public String getTableName() {
		return myTableName;
	}
	
	@Override
	public int hashCode() {
		return getDatabaseName().hashCode() + getOwnerName().hashCode() + getTableName().hashCode();
	}
}
