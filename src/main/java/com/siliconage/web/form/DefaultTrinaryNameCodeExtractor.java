package com.siliconage.web.form;

import com.siliconage.util.Trinary;

public class DefaultTrinaryNameCodeExtractor extends NullSafeNameCodeExtractor<Trinary> {
	/* This is a singleton class of which only one instance should ever exist.  Clients of this class
	should not create their own instances using a constructor, but should instead invoke the static
	method getInstance() to access the singleton instance. */
	
	/* A static reference to the only instance of this class, which is constructed on class load. */
	private static final DefaultTrinaryNameCodeExtractor ourInstance = new DefaultTrinaryNameCodeExtractor();
	
	/* A static accessor to obtain a reference to the singleton instance. */
	public static final DefaultTrinaryNameCodeExtractor getInstance() {
		return ourInstance;
	}
	
	private static final String[] DEFAULT_DROPDOWN_CODES = {"False", "Unknown", "True", };
	
	@Override
	protected String extractNameInternal(Trinary argT) {
		return argT != null ? DEFAULT_DROPDOWN_CODES[argT.ordinal()] : null;
	}

	@Override
	protected String extractCodeInternal(Trinary argT) {
		return argT.toString();
	}
}
