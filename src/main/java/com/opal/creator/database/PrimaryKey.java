package com.opal.creator.database;

public class PrimaryKey extends Key {
	public PrimaryKey(TableName argTableName, String argName) {
		super(argTableName, argName, true /* primary keys are always required */);
	}
}
