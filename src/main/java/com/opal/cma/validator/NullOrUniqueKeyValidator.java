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
public class NullOrUniqueKeyValidator<U extends UserFacing> extends WholeValidator<U> {
	private Factory<U> myFactory;
	private String myQueryString;
	private String[] myFieldNames;
	private boolean myUpdateOnly;
	
	public NullOrUniqueKeyValidator(Factory<U> argFactory, String argQueryString, String[] argFieldNames, boolean argUpdateOnly) {
		super();
		setFactory(argFactory);
		setQueryString(argQueryString);
		setFieldNames(argFieldNames);
		setUpdateOnly(argUpdateOnly);
	}
	
	@Override
	public void validate(String argPrefix, U argUF, Map<String, Object> argParsedValues, Collection<String> argErrors) {
		Validate.notNull(argParsedValues);
		Validate.notNull(argErrors);
		if (isUpdateOnly() && argUF == null) {
			return;
		}
		
		Object[] lclParameters = new Object[getFieldNames().length];
		for (int lclI = 0; lclI < getFieldNames().length; ++lclI) {
			String lclFieldName = getFieldNames()[lclI];
			Object lclO = argParsedValues.get(lclFieldName);
			if (lclO == null) {
				return;
			}
			lclParameters[lclI] = lclO;
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
			argErrors.add("Violates null or unique key defined by " + getQueryString());
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
