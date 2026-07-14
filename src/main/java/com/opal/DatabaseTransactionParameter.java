package com.opal;

import java.sql.Connection;
import java.sql.SQLException;

import com.siliconage.database.DatabaseUtility;

public class DatabaseTransactionParameter extends TransactionParameter {
	@SuppressWarnings("resource") // We own this Connection supplied in our ctor, and we are responsible for closing it.
	private final Connection myConnection;
	private boolean myClosed = false;
	
	protected DatabaseTransactionParameter(Connection argConnection) throws SQLException {
		super();
		
		assert argConnection != null;
		
		myConnection = argConnection;
		myConnection.setAutoCommit(false);
		
		assert myConnection.getAutoCommit() == false;
		
		return;
	}
	
	@Override
	public void close() {
		if (myClosed) {
			throw new IllegalStateException("Tried to close " + this + " when it was already closed.");
		}
		myClosed = true;
		DatabaseUtility.closeConnection(myConnection);
	}
	
	@Override
	public void commitPhaseOne() throws TransactionException {
		try {
			myConnection.commit();
		} catch (Exception lclE) {
			throw new TransactionException("Exception thrown while committing database transaction", lclE);
		}
	}
	
	public Connection getConnection() {
		return myConnection;
	}
	
	@Override
	public void rollback() throws TransactionException {
		try {
			myConnection.rollback();
		} catch (Exception lclE) {
			throw new TransactionException("Exception thrown while rolling back database transaction ", lclE);
		}
	}
		
}
