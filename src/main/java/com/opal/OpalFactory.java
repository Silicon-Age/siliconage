package com.opal;

import java.util.Collection;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

/**
 * @author topquark
 */

public interface OpalFactory<U extends UserFacing, O extends Opal<? extends U>> {
	
//	public void commitPhaseOne(TransactionParameter argTP, O argOpal);
//	public void commitPhaseTwo(TransactionParameter argTP, O argOpal);

	// TODO:  Can we get the next method out of this class? */
	public TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap);
	
	public int getFieldCount();
	
	public String getFieldName(int argFieldIndex);
	public int getFieldIndex(String argFieldName);
	
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

	public Set<O> getAll();
	public void acquireForQuery(Collection<O> argCollection, Query argQuery);

	default public O getOpalForQuery(Query argQuery) {
		Validate.notNull(argQuery);
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
