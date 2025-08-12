package com.opal;

public abstract class OpalPersistenceEngine {
	public OpalPersistenceEngine() {
		super();
	}
	public abstract TransactionParameter createTransactionParameter();
}
