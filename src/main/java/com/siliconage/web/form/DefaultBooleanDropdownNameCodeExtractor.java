package com.siliconage.web.form;

public class DefaultBooleanDropdownNameCodeExtractor extends NullSafeNameCodeExtractor<Boolean> {
	/* This is a singleton class of which only one instance should ever exist.  Clients of this class
	should not create their own instances using a constructor, but should instead invoke the static
	method getInstance() to access the singleton instance. */
	
	/* A static reference to the only instance of this class, which is constructed on class load. */
	private static final DefaultBooleanDropdownNameCodeExtractor ourInstance = new DefaultBooleanDropdownNameCodeExtractor();
	
	/* A static accessor to obtain a reference to the singleton instance. */
	public static final DefaultBooleanDropdownNameCodeExtractor getInstance() {
		return ourInstance;
	}
	
	@Override
	protected String extractNameInternal(Boolean argValue) {
		if (argValue == null) {
			return null;
		} else if (argValue.booleanValue()) {
			return "Yes";
		} else {
			return "No";
		}
	}

	@Override
	protected String extractCodeInternal(Boolean argValue) {
		return String.valueOf(argValue);
	}
}
