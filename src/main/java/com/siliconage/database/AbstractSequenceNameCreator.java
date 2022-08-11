package com.siliconage.database;

public abstract class AbstractSequenceNameCreator {
	protected AbstractSequenceNameCreator() {
		super();
	}
	
	public String makeSequenceName(@SuppressWarnings("unused") String argTableName, String argColumnName) {
		if (argColumnName == null) {
			return null;
		}
		return argColumnName + "_sq";
	}
}
