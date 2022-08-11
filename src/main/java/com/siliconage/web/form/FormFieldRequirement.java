package com.siliconage.web.form;

import com.siliconage.util.Trinary;

public enum FormFieldRequirement {
	REQUIRED(Trinary.TRUE),
	CONDITIONALLY_REQUIRED(Trinary.UNKNOWN),
	NOT_REQUIRED(Trinary.FALSE),
	NOT_APPLICABLE(Trinary.FALSE);
	
	private final Trinary myTrinaryInterpretation;
	
	private FormFieldRequirement(Trinary argTrinaryInterpretation) {
		myTrinaryInterpretation = argTrinaryInterpretation;
	}
	
	public Trinary asTrinary() {
		return myTrinaryInterpretation;
	}
	
	public static FormFieldRequirement of(boolean argB) {
		return argB ? REQUIRED : NOT_REQUIRED;
	}
}
