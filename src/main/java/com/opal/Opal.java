package com.opal;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;

public interface Opal<U extends UserFacing/*<U>*/> { // OPALFIXME

	//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(Opal.class.getName());
	
	public U getUserFacing();
	
	// OPALFIXME: Restore this
//	public List<OpalField<U, ?>> getFields();

	// OPALFIXME: Restore this
//	default OpalField<U, ?> getField(int argFieldIndex) {
//		// CHECK bounds?
//		return getFields().get(argFieldIndex);
//	}

	// OPALFIXME: Restore this
//	default OpalField<U, ?> getField(String argFieldName) {
//		Validate.notNull(argFieldName, "argFieldName is null.");
//		var lclFields = getFields();
//		int lclSize = getFields().size();
//		for (int lclI = 0; lclI < lclSize; ++lclI) {
//			OpalField<U, ?> lclField = lclFields.get(lclI);
//			if (argFieldName.equals(lclField.getName())) {
//				return lclField;
//			}
//		}
//		throw new IllegalArgumentException("\"" + argFieldName + "\" is not a valid field name.");
//	}

	// OPALFIXME Restore this
//	default int getFieldIndex(String argFieldName) {
//		return getField(argFieldName).getIndex();
//	}

	// OPALFIXME: The following four lines should be deleted.
	public String[] getFieldNames();
	public Class<?>[] getFieldTypes();
	public boolean[] getFieldNullability();
	public FieldValidator[] getFieldValidators();

	default int getFieldCount() {
//		return getFields().size();
		return getFieldNames().length;
	}
	
	default int getFieldIndex(String argFieldName) {
		for (int i = 0; i < getFieldCount(); ++i) {
			if (argFieldName.equals(getFieldNames()[i])) {
				return i;
			}
		}
		return -1; // Exception?
	}
	
	default String getFieldName(int argFieldIndex) {
//		return getField(argFieldIndex).getName();
		return getFieldNames()[argFieldIndex];
	}

	public Object getFieldValue(int argFieldIndex);
//	default Object getField(int argFieldIndex) {
//		return getFieldValue(argFieldIndex);
//	}
	
	default Object getFieldValue(String argFieldName) {
		return getFieldValue(getFieldIndex(argFieldName));
	}
//	default Object getField(String argFieldName) {
//		return getFieldValue(argFieldName);
//	}
	
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
