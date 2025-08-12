package com.opal.creator.database;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class Key {
	private final TableName myTableName;
	private final String myName;
	private boolean myRequired;
	
	private final List<String> myColumnNames = new ArrayList<>();
	
	public Key(TableName argTableName, String argName, boolean argRequired) {
		super();
		
		Validate.notNull(argTableName);
		myTableName = argTableName;
		
		Validate.notNull(argName);
		myName = argName;
		
		myRequired = argRequired;
	}
	
	public List<String> getColumnNames() {
		return myColumnNames;
	}
	
	public String getName() {
		return myName;
	}
	
	public TableName getTableName() {
		return myTableName;
	}
	
	public boolean isRequired() {
		return myRequired;
	}
	
	public void setRequired(boolean argRequired) {
		myRequired = argRequired;
	}
	
	@Override
	public String toString() {
		return getTableName().toString() + '[' + StringUtils.join(getColumnNames(), ", ") + ']';
	}
}
