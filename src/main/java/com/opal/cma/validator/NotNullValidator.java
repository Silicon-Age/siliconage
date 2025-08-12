package com.opal.cma.validator;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * @author topquark
 */
@Deprecated
public class NotNullValidator extends FieldValidator {
	private static final NotNullValidator ourInstance = new NotNullValidator();
	
	public static NotNullValidator getInstance() {
		return ourInstance;
	}
	
	/* Public to eventually allow the substitution of new error messages. */
	public NotNullValidator() {
		super();
	}
	
	@Override
	protected Class<?> getParameterType() {
		return null;
	}
	
	@Override
	protected void validateInternal(List<String> argList, Object argObject) {
		if (argObject == null) {
			argList.add("%f must not be blank.");
		} else if (argObject.getClass() == String.class) {
			if (StringUtils.isEmpty((String) argObject)) {
				argList.add("%f must not be entirely whitepace.");
			}
		}
	}
}
