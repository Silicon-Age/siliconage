package com.opal.rest;

import org.apache.commons.lang3.Validate;

public class RestResultException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final RestResult myRestResult;
	
	public RestResultException(RestResult argRR) {
		super();
		Validate.notNull(argRR);
		myRestResult = argRR;
	}
	
	public RestResult getRestResult() {
		return myRestResult;
	}
}
