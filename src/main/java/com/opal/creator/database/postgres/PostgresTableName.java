package com.opal.creator.database.postgres;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.opal.creator.database.TableName;

public class PostgresTableName extends TableName {
	private final String myDatabaseName;
	private final String myTableName;
	
	public PostgresTableName(String argDatabaseName, String argTableName) {
		super();
		
//		Validate.notNull(argDatabaseName); // Can be null, indicating that one should use the default database for the connection
		if (argDatabaseName != null && argDatabaseName.equals("public")) {
			myDatabaseName = null;
		} else {
			myDatabaseName = argDatabaseName;
		}
		
		Validate.notNull(argTableName);
		String lclTableName = argTableName.toLowerCase(); // postgresql is case-insensitive
		if (argTableName.startsWith("public.")) {
			myTableName = lclTableName.substring(7); // 7 == "public.".length()
		} else {
			myTableName = lclTableName;
		}
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
		PostgresTableName lclTableName = (PostgresTableName) argObject;
		
		// System.out.println("this.getTableName() = '" + this.getTableName() + "'; that.getTableName() = '" + lclTableName.getTableName() + "'; this.getDatabaseName() = '" + this.getDatabaseName() + "'; that.getDatabaseName() = '" + lclTableName.getDatabaseName() + "'");
		
		return ((getTableName() == null && lclTableName.getTableName() == null) || StringUtils.equalsIgnoreCase(getTableName(), lclTableName.getTableName())) && StringUtils.equalsIgnoreCase(getDatabaseName(), lclTableName.getDatabaseName()); 
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
