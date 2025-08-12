package com.opal;

public class TransactionException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public TransactionException(String argMessage) {
		super(argMessage);
	}
	
	public TransactionException(String argMessage, Throwable argThrowable) {
		super(argMessage, argThrowable);
	}
}
