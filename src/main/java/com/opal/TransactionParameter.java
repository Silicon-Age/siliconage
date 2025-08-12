package com.opal;

public abstract class TransactionParameter {
	public TransactionParameter() {
		super();
	}
	
	@SuppressWarnings("unused")
	public void close() throws TransactionException {
		return;
	}
	
	@SuppressWarnings("unused")
	public void commitPhaseOne() throws TransactionException {
		return;
	}
	
	@SuppressWarnings("unused")
	public void commitPhaseTwo() throws TransactionException {
		return;
	}
	
	public abstract void rollback() throws TransactionException;
}
