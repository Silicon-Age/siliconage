package com.opal.creator.database.sybase;

import com.opal.creator.database.DefaultValue;
import com.opal.creator.database.TableName;
import com.opal.creator.database.sqlserver.SQLServerColumn;

public class SybaseColumn extends SQLServerColumn {
	public SybaseColumn(TableName argTableName, String argName, String argDataType, int argLength, int argPrecision, int argScale, boolean argWideCharacter, boolean argNullable, DefaultValue argDefault, boolean argIdentity) {
		super(
			argTableName,
			argName,
			argDataType,
			argLength,
			argPrecision,
			argScale,
			argWideCharacter,
			argNullable,
			null, // Missing domain (i.e., user-defined type)
			argDefault,
			argIdentity
		);
	}
}
