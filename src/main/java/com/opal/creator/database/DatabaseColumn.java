package com.opal.creator.database;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public class DatabaseColumn {
	
	private final TableName myTableName;
	private final String myName;
	private final String myDataType;
	private final int myLength;
	private final int myPrecision;
	private final int myScale;
	private final boolean myWideCharacters; // Only relevant for string-y types
	private final String myDomainName;
	private final boolean myNullable;
	private final DefaultValue myDefault;
	private boolean mySequenced = false;
	private boolean myComplicatedDefault = false;
	
	private final ArrayList<CheckConstraint> myCheckConstraints = new ArrayList<>();
	
	/* TODO: Move Sequence stuff to OracleColumn? */
	
	public DatabaseColumn(TableName argTableName, String argName, String argDataType, int argLength, int argPrecision, int argScale, boolean argWideCharacters, String argDomainName, boolean argNullable, DefaultValue argDefault) {
		super();
		
		myTableName = Validate.notNull(argTableName);
		myName = Validate.notNull(argName);
		myDataType = Validate.notNull(argDataType);
		myLength = argLength;
		myPrecision = argPrecision;
		myScale = argScale;
		myWideCharacters = argWideCharacters;
		myDomainName = argDomainName != null ? argDomainName : argDataType;
		myNullable = argNullable;
		myDefault = argDefault;
	}
	
	public java.lang.String getDataType() {
		return myDataType;
	}
	
	public DefaultValue getDefault() {
		return myDefault;
	}
	
	public int getLength() {
		return myLength;
	}
	
	public String getName() {
		return myName;
	}
	
	public int getPrecision() {
		return myPrecision;
	}
	
	public int getScale() {
		return myScale;
	}
	
	public boolean isWideCharacters() {
		return myWideCharacters;
	}
	
	public String getDomainName() {
		return myDomainName;
	}
	
	/* FIXME:  This should be moved into a new OracleColumn class */
	public String getSequenceName() {
		if (!isSequenced()) {
			throw new IllegalStateException("Cannot call getSequenceName() on " + this + " because it is not sequenced.");
		}
		return getName() + "_SQ"; // Magic string!
	}
	
	public TableName getTableName() {
		return myTableName;
	}
	
	public boolean isNullable() {
		return myNullable;
	}
	
	/* FIXME:  This should be moved into a new OracleColumn class */
	public boolean isSequenced() {
		return mySequenced;
	}
	
	public void setSequenced(boolean argSequenced) {
		mySequenced = argSequenced;
	}
	
	@Override
	public final boolean equals(Object argObject) {
		if (argObject == null) {
			return false;
		}
		if (argObject.getClass() != this.getClass()) {
			return false;
		}
		DatabaseColumn that = (DatabaseColumn) argObject;
		
		boolean lclResult = this.getTableName().equals(that.getTableName()) && this.getName().equals(that.getName());
		return lclResult;
	}
	
	@Override
	public int hashCode() {
		return getTableName().hashCode() ^ getName().hashCode();
	}
	
	public boolean hasDatabaseGeneratedNumber() {
		return isSequenced();
	}

	public boolean hasComplicatedDefault() {
		return myComplicatedDefault;
	}
	
	public void setComplicatedDefault(boolean argComplicatedDefault) {
		myComplicatedDefault = argComplicatedDefault;
	}
	
	public List<CheckConstraint> getCheckConstraints() {
		return myCheckConstraints;
	}
	
	public void addCheckConstraint(CheckConstraint argCC) {
		Validate.notNull(argCC);
		getCheckConstraints().add(argCC);
	}
	
	@Override
	public String toString() {
		return getTableName().getFullyQualifiedTableName() + '/' + getName();
	}
}
