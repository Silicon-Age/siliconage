package com.opal.rest;

import java.util.Objects;

public class RestResultException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final RestResult myRestResult;
	
	public RestResultException(RestResult argRR) {
		super();
		myRestResult = Objects.requireNonNull(argRR);
	}
	
	public RestResult getRestResult() {
		return myRestResult;
	}
}
