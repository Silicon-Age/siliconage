package com.siliconage.util;

/**
 * @author topquark
 */
public class FactoryException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public FactoryException(String argMessage) {
		super(argMessage);
	}
	
	public FactoryException(String argMessage, Throwable argThrowable) {
		super(argMessage, argThrowable);
	}
}
