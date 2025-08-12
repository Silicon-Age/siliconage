package com.opal;

import java.util.Collection;
import java.util.Set;

/**
 * @author topquark
 */
public interface Factory<U extends UserFacing> {
	public Class<U> getUserFacingInterface();
	
	public Set<U> getAll();
	default public U[] createAllArray() {
		Set<U> lclAll = getAll();
		
		return lclAll.toArray(createArray(lclAll.size()));
	}
	
	public <T extends Collection<? super U>> T acquireForQuery(T argCollection, Query argQuery);
	public U getForQuery(Query argQuery);
	public U[] createArray(int argSize);
	
	public int getFieldCount();
	
	public String getFieldName(int argFieldIndex);
	
	default public String getFieldName(String argFieldName) {
		return getFieldName(getFieldIndex(argFieldName));
	}
	
	public Class<?> getFieldType(int argFieldIndex);
	default public Class<?> getFieldType(String argFieldName) {
		return getFieldType(getFieldIndex(argFieldName));
	}
	
	public boolean getFieldNullability(int argFieldIndex);
	default public boolean getFieldNullability(String argFieldName) {
		return getFieldNullability(getFieldIndex(argFieldName));
	}
	
	public FieldValidator getFieldValidator(int argFieldIndex);
	default public FieldValidator getFieldValidator(String argFieldName) {
		return getFieldValidator(getFieldIndex(argFieldName));
	}
	
	public int getFieldIndex(String argFieldName);
}
