package com.siliconage.util;

/**
 * Exception to be thrown if there are problems manipulating Calendar or Date
 * objects.
 * <BR><BR>
 * Copyright &copy; 2001 Silicon Age, Inc.  All Rights Reserved.
 * @author <a href="mailto:matt.bruce@silicon-age.com">Matt Bruce</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class CalendarException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public CalendarException() {
		super();
	}
	
	public CalendarException(String argMessage) {
		super(argMessage);
	}
}
