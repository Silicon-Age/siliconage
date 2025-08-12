package com.opal;

public class PersistenceException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public PersistenceException(String argMessage) {
		super(argMessage);
	}
	
	public PersistenceException(String argMessage, Throwable argThrowable) {
		super(argMessage, argThrowable);
	}
}
