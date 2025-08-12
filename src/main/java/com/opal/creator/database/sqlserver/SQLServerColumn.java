package com.opal.creator.database.sqlserver;

import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.DefaultValue;
import com.opal.creator.database.TableName;

public class SQLServerColumn extends DatabaseColumn {
	private final boolean myIdentity;
	
	public SQLServerColumn(TableName argTableName, String argName, String argDataType, int argLength, int argPrecision, int argScale, boolean argWideCharacters, boolean argNullable, String argDomainName, DefaultValue argDefault, boolean argIdentity) {
		super(argTableName, argName, argDataType, argLength, argPrecision, argScale, argWideCharacters, argDomainName, argNullable, argDefault);
		
		myIdentity = argIdentity;
	}
	
	public boolean isIdentity() {
		return myIdentity;
	}
	
	@Override
	public boolean hasDatabaseGeneratedNumber() {
		return isIdentity();
	}
	
}
