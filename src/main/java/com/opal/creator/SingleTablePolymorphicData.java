package com.opal.creator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author topquark
 */
public class SingleTablePolymorphicData extends PolymorphicData {
	private final ArrayList<String> myDereferenceMethods = new ArrayList<>();
	private final ArrayList<String> myDereferenceNames = new ArrayList<>();
	private final ArrayList<MappedForeignKey> myDereferenceKeys = new ArrayList<>();
	
	private String myColumnName;
	private String myMethod;
	
	public SingleTablePolymorphicData() {
		super();
	}

	public String getColumnName() {
		return myColumnName;
	}

	public void setColumnName(String argColumnName) {
		myColumnName = argColumnName;
	}

	public String getMethod() {
		return myMethod;
	}
	
	public void setMethod(String argMethod) {
		myMethod = argMethod;
	}
	
	public List<String> getDereferenceMethods() {
		return myDereferenceMethods;
	}
	
	public List<String> getDereferenceNames() {
		return myDereferenceNames;
	}
	
	public List<MappedForeignKey> getDereferenceKeys() {
		return myDereferenceKeys;
	}
	
	@Override
	public boolean requiresTypedCreate() {
		return true;
	}
	
	@Override
	public MappedClass getUltimateConcreteTypeDeterminer() {
		return getDereferenceKeys().get(getDereferenceKeys().size() - 1).getTargetMappedClass();
	}
}
