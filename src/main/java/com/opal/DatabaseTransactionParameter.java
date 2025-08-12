package com.opal;

import java.sql.Connection;
import java.sql.SQLException;

import com.siliconage.database.DatabaseUtility;

public class DatabaseTransactionParameter extends TransactionParameter {
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
		DatabaseUtility.closeConnection(getConnection());
	}
	
	@Override
	public void commitPhaseOne() throws TransactionException {
		try {
//			ourLogger.debug("Committing the SQL");
			getConnection().commit();
//			ourLogger.debug("Commit completed");
		} catch (Exception lclE) {
			throw new TransactionException("Exception thrown while committing database transaction", lclE);
		} finally {
//			ourLogger.debug("Leaving commitPhaseOne()");
		}
	}
	
	public Connection getConnection() {
		return myConnection;
	}
	
	@Override
	public void rollback() throws TransactionException {
		try {
			getConnection().rollback();
		} catch (Exception lclE) {
			throw new TransactionException("Exception thrown while rolling back database transaction ", lclE);
		}
	}

	/* finalize() is now officially deprecated, so let's see if this was doing this any good. */
//	@Override
//	protected void finalize() throws Throwable {
//		try {
//			if (myClosed == false) {
//				close();
//			}
//		} finally {
//			super.finalize();
//		}
//	}
		
}
