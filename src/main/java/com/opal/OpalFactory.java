package com.opal;

import java.util.Collection;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

/**
 * @author topquark
 */

public interface OpalFactory<U extends UserFacing/*<U>*/, O extends Opal<? extends U>> { // OPALFIXME
	
//	public void commitPhaseOne(TransactionParameter argTP, O argOpal);
//	public void commitPhaseTwo(TransactionParameter argTP, O argOpal);

	// TODO:  Can we get the next method out of this class? */
	public TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap);
	
//	public List<OpalField<U, ?>> getFields(); // OPALFIXME: Restore this
	
	// OPALFIXME: Can all of this be moved to some sort of encapsulating object?
	// OPALFIXME: Restore this
//	default int getFieldCount() {
//		return getFields().size();
//	}
	
	// OPALFIXME: Restore this
//	default OpalField<U, ?> getField(int argFieldIndex) {
//		return getFields().get(argFieldIndex);
//	}
	
	// OPALFIXME: Restore this
//	default OpalField<U, ?> getField(String argFieldName) {
//		var lclFields = getFields();
//		int lclFieldCount = lclFields.size();
//		for (int lclI = 0; lclI < lclFieldCount; ++lclI) {
//			OpalField<U, ?> lclField = lclFields.get(lclI);
//			if (lclField.getName().equals(argFieldName)) {
//				return lclField;
//			}
//		}
//		throw new IllegalArgumentException("No field found with name \"" + argFieldName + "\".");
//	}
	
	// OPALFIXME: Do we need all of these methods?  Or can we tell people to just getField and go from there?
//	default String getFieldName(int argFieldIndex) {
//		return getField(argFieldIndex).getName();
//	}
//	
//	default int getFieldIndex(String argFieldName) {
//		return getField(argFieldName).getIndex();
//	}
//	
//	default Class<?> getFieldType(int argFieldIndex) {
//		return getField(argFieldIndex).getType();
//	}
//	
//	default Class<?> getFieldType(String argFieldName) {
//		return getField(argFieldName).getType();
//	}
//
//	default boolean getFieldNullability(int argFieldIndex) {
//		return getField(argFieldIndex).isNullable();
//	}
//	
//	default boolean getFieldNullability(String argFieldName) {
//		return getField(argFieldName).isNullable();
//	}
//
//	default FieldValidator getFieldValidator(int argFieldIndex) {
//		return getField(argFieldIndex).getValidator();
//	}
//	
//	default FieldValidator getFieldValidator(String argFieldName) {
//		return getField(argFieldName).getValidator();
//	}

	// OPALFIXME: These next nine methods should be removed
	public int getFieldCount();
	public String getFieldName(int argFieldIndex);
	public int getFieldIndex(String argFieldName);
	public Class<?> getFieldType(int argFieldIndex);
	default Class<?> getFieldType(String argFieldName) {
		return getFieldType(getFieldIndex(argFieldName));
	}
	public boolean getFieldNullability(int argFieldIndex);
	default boolean getFieldNullability(String argFieldName) {
		return getFieldNullability(getFieldIndex(argFieldName));
	}	
	public FieldValidator getFieldValidator (int argFieldIndex);
	default FieldValidator getFieldValidator (String argFieldName) {
		return getFieldValidator(getFieldIndex(argFieldName));
	}
	// /OPALFIXME Down to here
	
	public Set<O> getAll();
	public void acquireForQuery(Collection<O> argCollection, Query argQuery);

	default public O getOpalForQuery(Query argQuery) {
		Objects.requireNonNull(argQuery);
		ArrayList<O> lclAL = new ArrayList<>();
		acquireForQuery(lclAL, argQuery);
		if (lclAL.isEmpty()) {
			return null;
		} else if (lclAL.size() == 1) {
			return lclAL.get(0);
		} else {
			throw new IllegalStateException("Query " + argQuery + " passed to get getForQuery returned more than one row.");
		}
	}
	
}
