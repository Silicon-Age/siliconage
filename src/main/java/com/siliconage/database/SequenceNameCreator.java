package com.siliconage.database;

public class SequenceNameCreator {
	private static SequenceNameCreator ourInstance;
	private static final String SEQUENCE_SUFFIX = "_sq";
	
	protected SequenceNameCreator() {
		super();
	}
	
	public static synchronized SequenceNameCreator getInstance() {
		if (ourInstance == null) {
			ourInstance = new SequenceNameCreator();
		}
		return ourInstance;
	}
	public String makeSequenceName(@SuppressWarnings("unused") String argTableName, String argColumnName) {
		if (argColumnName == null) {
			return null;
		}
		return argColumnName + SEQUENCE_SUFFIX;
	}
}
