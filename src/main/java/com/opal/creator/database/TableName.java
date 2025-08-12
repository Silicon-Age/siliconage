package com.opal.creator.database;

public abstract class TableName {

	protected TableName() {
		super();
	}
	
	@Override
	public abstract boolean equals(Object argObject); /* Force subclasses to override this. */
	
	public abstract String getFullyQualifiedTableName();
	
	public abstract String getTableName();
	
	@Override
	public abstract int hashCode();
	
	@Override
	public String toString() {
		return getFullyQualifiedTableName();
	}
}
