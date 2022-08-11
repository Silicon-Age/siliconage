package com.siliconage.util;

/**
 * Will be thrown by methods that have no functionality.
 * <BR><BR>
 * Copyright &copy; 2000 Silicon Age, Inc.  All Rights Reserved.
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class UnimplementedOperationException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public UnimplementedOperationException() {
		super();
	}
	
	public UnimplementedOperationException(String argErrorMessage) {
		super(argErrorMessage);
	}
}
