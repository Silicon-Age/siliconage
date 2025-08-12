package com.opal.creator.database.mysql;

import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.DefaultValue;

public class MySQLColumn extends DatabaseColumn {
	private final boolean myAutoIncrement;
	private final boolean mySigned;
	
	public MySQLColumn(MySQLTableName argTableName, String argName, String argDataType, int argLength, int argPrecision, int argScale, boolean argWideCharacters, String argDomainName, boolean argNullable, DefaultValue argDefault, boolean argAutoIncrement, boolean argSigned) {
		super(argTableName, argName, argDataType, argLength, argPrecision, argScale, argWideCharacters, argDomainName, argNullable, argDefault);
		
		myAutoIncrement = argAutoIncrement;
		mySigned = argSigned;
	}

	public boolean isAutoIncrement() {
		return myAutoIncrement;
	}
	
	public boolean isSigned() {
		return mySigned;
	}
	
	public boolean isUnsigned() {
		return !mySigned;
	}
	
	@Override
	public boolean hasDatabaseGeneratedNumber() {
		return isAutoIncrement() || isSequenced();
	}
}
