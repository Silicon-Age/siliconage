package com.opal.cma.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.opal.Factory;
import com.opal.ImplicitTableDatabaseQuery;
import com.opal.UserFacing;

/**
 * @author topquark
 */
@Deprecated
public class UniqueKeyValidator<U extends UserFacing> extends WholeValidator<U> {
	private Factory<U> myFactory;
	private String myQueryString;
	private String[] myFieldNames;
	private boolean myUpdateOnly;
	
//	private String myInsertQueryString;
//	private String myUpdateQueryString;
//	private String[] myInsertFieldNames;
//	private String[] myUpdateFieldNames;
	
	public UniqueKeyValidator(Factory<U> argFactory, String argQueryString, String[] argFieldNames, boolean argUpdateOnly) {
		super();
		setFactory(argFactory);
		setQueryString(argQueryString);
		setFieldNames(argFieldNames);
		setUpdateOnly(argUpdateOnly);
//		setInsertQueryString(argInsertQueryString);
//		setInsertFieldNames(argInsertFieldNames);
//		setUpdateQueryString(argUpdateQueryString);
//		setUpdateFieldNames(argUpdateFieldNames);
	}
	
	@Override
	public void validate(String argPrefix, U argUF, Map<String, Object> argValues, Collection<String> argErrors) {
		Validate.notNull(argValues);
		Validate.notNull(argErrors);
		
		if (isUpdateOnly() && argUF == null) {
			return;
		}
		
		Object[] lclParameters = new Object[getFieldNames().length];
		boolean lclMissing = false;
		for (int lclI = 0; lclI < getFieldNames().length; ++lclI) {
			String lclFieldName = getFieldNames()[lclI];
			String lclCompleteName = argPrefix + lclFieldName;
			if (argValues.containsKey(lclCompleteName)) {
				Object lclO = argValues.get(lclCompleteName);
				if (lclO == null) {
					argErrors.add(lclFieldName + " may not be blank because it is part of a unique key.");
					return;
				}
				lclParameters[lclI] = lclO;
			} else {
				lclMissing = true;
			}
		}

		/* FIXME If one or more elements of the UniqueKey are missing (and presumably were not displayed on the
		 * form for editing), we just assume that they corresponded to a unique key before and will correspond
		 * to one afterward.  This will need to be worked out in more detail later.
		 */
		
		if (lclMissing) {
			return;
		}
		
		ArrayList<U> lclResults = new ArrayList<>();
		
		getFactory().acquireForQuery(
			lclResults,
			new ImplicitTableDatabaseQuery(
				getQueryString(),
				lclParameters
			)
		);
		if (argUF != null) {
			lclResults.remove(argUF);
		}
		if (lclResults.size() > 0) {
			argErrors.add("Violates unique key defined by " + getQueryString());
			return;
		}
		return;
	}
	
	public Factory<U> getFactory() {
		return myFactory; 
	}
	
	public void setFactory(Factory<U> argFactory) {
		Validate.notNull(argFactory);
		myFactory = argFactory;
	}
	
	public String[] getFieldNames() {
		return myFieldNames;
	}
	
	public void setFieldNames(String[] argFieldNames) {
		Validate.isTrue(StringUtils.isNoneEmpty(argFieldNames));
		myFieldNames = argFieldNames;
	}
	
	public String getQueryString() {
		return myQueryString;
	}
	
	public void setQueryString(String argQueryString) {
		Validate.notNull(argQueryString);
		myQueryString = argQueryString;
	}
	public boolean isUpdateOnly() {
		return myUpdateOnly;
	}
	
	public void setUpdateOnly(boolean argUpdateOnly) {
		myUpdateOnly = argUpdateOnly;
	}
}
