package com.opal.creator.database;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.opal.creator.UniqueKeyType;

public class Index {
	private final TableName myTableName;
	private final String myIndexName;
	private final boolean myUnique;
	private final String myFilter;
	private final List<String> myColumnNames = new ArrayList<>();
	private boolean myMapped = true;
	private boolean myUniqueStringKey = false;
	
	public Index(TableName argTableName, String argIndexName, boolean argUnique, String argFilter) {
		super();
		
		Validate.notNull(argTableName);
		myTableName = argTableName;
		
		Validate.notNull(argIndexName);
		
		myIndexName = argIndexName;
		
		myUnique = argUnique;
		
		myFilter = argFilter;
		
		return;
	}
	
	public List<String> getColumnNames() {
		return myColumnNames;
	}
	
	public String getIndexName() {
		return myIndexName;
	}
	
	public TableName getTableName() {
		return myTableName;
	}
	
	public boolean isUnique() {
		return myUnique;
	}
	
	@Override
	public String toString() {
		return getIndexName() + '[' + getTableName() + ']';
	}
	
	public boolean isMapped() {
		return myMapped;
	}
	
	public void setMapped(boolean argMapped) {
		myMapped = argMapped;
	}
	
	public String getFilter() {
		return myFilter;
	}

	public boolean isUniqueStringKey() {
		return myUniqueStringKey;
	}
	
	public void setUniqueStringKey(boolean argUniqueStringKey) {
		myUniqueStringKey = argUniqueStringKey;
	}
	
	protected boolean isFilteredToEliminateNulls() {
		String lclS = getFilter();
		if (lclS == null) {
			return false;
		}
		lclS = lclS.toUpperCase();
		return lclS.contains("NOT NULL"); // FIXME: An obvious heuristic that will need to be refined
	}
	
	public UniqueKeyType determineDefaultType() {
		if (isUnique()) {
			if (isFilteredToEliminateNulls()) {
				return UniqueKeyType.UNIQUE_IF_ENTIRELY_NOT_NULL;
			} else {
				return UniqueKeyType.UNIQUE;
			}
		} else {
			return UniqueKeyType.NONUNIQUE;
		}
	}
	
}
