package com.opal.creator.database.mysql;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.opal.creator.database.TableName;

public class MySQLTableName extends TableName {
	private final String myDatabaseName;
	private final String myTableName;
	
	public MySQLTableName(String argDatabaseName, String argTableName) {
		super();
		
//		Validate.notNull(argDatabaseName); // Can be null, indicating that one should use the default database for the connection
		myDatabaseName = argDatabaseName;
		
		Validate.notNull(argTableName);
		myTableName = argTableName;
	}
	
	public String getDatabaseName() {
		return myDatabaseName;
	}
	
	@Override
	public String getTableName() {
		return myTableName;
	}
	
	@Override
	public boolean equals(Object argObject) {
		if (argObject == null) {
			return false;
		}
		if (this == argObject) {
			return true;
		}
		if (argObject.getClass() != this.getClass()) {
			return false;
		}
		MySQLTableName lclTableName = (MySQLTableName) argObject;
		
		return StringUtils.equals(getTableName(), lclTableName.getTableName()) && StringUtils.equals(getDatabaseName(), lclTableName.getDatabaseName()); 
	}
	
	@Override
	public String getFullyQualifiedTableName() {
		if (getDatabaseName() == null) {
			return getTableName();
		} else {
			return getDatabaseName() + '.' + getTableName();
		}
	}
	
	@Override
	public int hashCode() {
		if (getDatabaseName() == null) {
			return getTableName().hashCode();
		} else {
			return getDatabaseName().hashCode() ^ getTableName().hashCode();
		}
	}
}
