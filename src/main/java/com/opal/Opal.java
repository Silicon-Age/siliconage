package com.opal;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;

import org.apache.commons.lang3.Validate;

public interface Opal<U extends UserFacing> {

	//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(Opal.class.getName());
	
	public U getUserFacing();
	
	public Object getField(int argFieldIndex);
	
	default int getFieldIndex(String argFieldName) {
		Validate.notNull(argFieldName);
		String[] lclFieldNames = getFieldNames();
		for (int lclI = 0; lclI < lclFieldNames.length; ++lclI) {
			if (argFieldName.equals(lclFieldNames[lclI])) {
				return lclI;
			}
		}
		throw new IllegalArgumentException("\"" + argFieldName + "\" is not a valid field name.");
	}

	public String[] getFieldNames();
	public Class<?>[] getFieldTypes();
	public boolean[] getFieldNullability();
	public FieldValidator[] getFieldValidators();

	default int getFieldCount() {
		return getFieldNames().length;
	}
	
	default String getFieldName(int argFieldIndex) {
		return getFieldNames()[argFieldIndex];
	}
	
	default Class<?> getFieldType(String argFieldName) {
		return getFieldType(getFieldIndex(argFieldName));
	}
	
	default Class<?> getFieldType(int argFieldIndex) {
		return getFieldTypes()[argFieldIndex];
	}
	
	default boolean getFieldNullability(String argFieldName) {
		return getFieldNullability(getFieldIndex(argFieldName));
	}

	default boolean getFieldNullability(int argFieldIndex) {
		return getFieldNullability()[argFieldIndex];
	}

	default FieldValidator getFieldValidator(String argFieldName) {
		return getFieldValidator(getFieldIndex(argFieldName));
	}

	default FieldValidator getFieldValidator(int argFieldIndex) {
		return getFieldValidators()[argFieldIndex];
	}

	public void output(PrintStream argPS) throws IOException;

	public void output(PrintWriter argPW) throws IOException;
	
}
