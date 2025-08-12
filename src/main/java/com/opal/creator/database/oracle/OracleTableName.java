package com.opal.creator.database.oracle;

import com.opal.creator.database.TableName;

import org.apache.commons.lang3.Validate;

public class OracleTableName extends TableName {
	private final String mySchemaName;
	private final String myTableName;
	
	public OracleTableName(String argSchemaName, String argTableName) {
		super();
		
		Validate.notNull(argSchemaName);
		mySchemaName = argSchemaName;
		
		Validate.notNull(argTableName);
		myTableName = argTableName;
		
		return;
	}
	
	@Override
	public boolean equals(Object argO) {
		if (argO == null) {
			return false;
		}
		
		if (this.getClass() != argO.getClass()) {
			return false;
		}
		
		OracleTableName that = (OracleTableName) argO;
		// TODO: Swap for efficiency
		return this.getSchemaName().equals(that.getSchemaName()) && this.getTableName().equals(that.getTableName());
	}
	
	@Override
	public String getFullyQualifiedTableName() {
		return getSchemaName() + '.' + getTableName();
	}
	
	public String getSchemaName() {
		return mySchemaName;
	}
	
	@Override
	public String getTableName() {
		return myTableName;
	}
	
	@Override
	public int hashCode() {
		return mySchemaName.hashCode() + myTableName.hashCode();
	}
}
