package com.siliconage.web;

public class MissingIdentityException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public MissingIdentityException() {
		super();
	}
	
	public MissingIdentityException(String s) {
		super(s);
	}
}
