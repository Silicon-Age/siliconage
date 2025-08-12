package com.opal.creator;

import com.opal.creator.database.Key;

public class MultipleLookUpSpecification {
	
	private final MappedClass myMappedClass;
	private final Key myKey; // This should probably not be aware of database-specific classes.
	
	public MultipleLookUpSpecification(MappedClass argMC, Key argKey) {
		super();
		
		myMappedClass = argMC;
		myKey = argKey;
	}
	
	public MappedClass getMappedClass() {
		return myMappedClass;
	}
	
	public Key getKey() {
		return myKey;
	}
	
	public String getFactoryMethodName() {
		StringBuilder lclSB = new StringBuilder("for");
		
		for (String lclCN : getKey().getColumnNames()) {
			ClassMember lclCM = getMappedClass().getClassMemberByColumnName(lclCN);
			lclSB.append(lclCM.getBaseMemberName());
		}
		
		return lclSB.toString();
	}
	
	public String getFactoryMethodArguments() {
		StringBuilder lclSB = new StringBuilder();
		
		boolean lclFirst = true;
		for (String lclCN : getKey().getColumnNames()) {
			ClassMember lclCM = getMappedClass().getClassMemberByColumnName(lclCN);
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(", ");
			}
			lclSB.append(lclCM.getMemberType().getName());
			lclSB.append(' ');
			lclSB.append(lclCM.getObjectMutatorArgumentName());
		}
		
		return lclSB.toString();
	}
	
	public String getQuerySQLClause() {
		StringBuilder lclSB = new StringBuilder();
		
		boolean lclFirst = true;
		for (String lclCN : getKey().getColumnNames()) {
			ClassMember lclCM = getMappedClass().getClassMemberByColumnName(lclCN);
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(" AND ");
			}
			lclSB.append(lclCM.getDatabaseColumn().getName());
			lclSB.append(" = ?");
		}
		
		return lclSB.toString();
	}
	
	public String getQueryParameters() {
		StringBuilder lclSB = new StringBuilder();
		
		boolean lclFirst = true;
		for (String lclCN : getKey().getColumnNames()) {
			ClassMember lclCM = getMappedClass().getClassMemberByColumnName(lclCN);
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(", ");
			}
			lclSB.append(lclCM.getObjectMutatorArgumentName());
		}
		
		return lclSB.toString();
	}
	
}
