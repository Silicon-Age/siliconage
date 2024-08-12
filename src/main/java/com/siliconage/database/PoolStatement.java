package com.siliconage.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * Copyright &copy; 2000 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class PoolStatement implements Statement {
	private final Statement myStatement;
	private final PoolConnection myConnection;
	
	/**
	 * PoolStatement constructor that sets the value of the internal Statement.
	 * @param argStatement Statement
	 * @param argPC PoolConnection
	 */
	public PoolStatement(Statement argStatement, PoolConnection argPC) {
		super();
		init();
		myStatement = argStatement;
		myConnection = argPC;
	}
	/**
	 * Calls addBatch() on the internal Statement.
	 * @param sql String
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void addBatch(String sql) throws SQLException {
		getStatement().addBatch(sql);
	}
	/**
	 * Calls cancel() on the internal Statement.
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void cancel() throws SQLException {
		getStatement().cancel();
	}
	/**
	 * Clears the batch from the internal Statement.
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void clearBatch() throws SQLException {
		getStatement().clearBatch();
	}
	/**
	 * Clears the warnings from the internal Statement.
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void clearWarnings() throws SQLException {
		getStatement().clearWarnings();
	}
	/**
	 * Closes the internal Statement and returns this to the DatabaseReuser.
	 */
	@Override
	public void close() {
		DatabaseUtility.closeStatement(getStatement());
//		DatabaseReuser.returnPoolStatement(this); // Is this right?
	}
	/**
	 * Calls execute on the internal Statement with the argument SQL string.
	 * @return boolean
	 * @param sql String
	 * @throws SQLException If there is a problem
	 */
	@Override
	public boolean execute(String sql) throws SQLException {
		return getStatement().execute(sql);
	}
	/**
	 * Executes a batch on the internal Statement.
	 * @return int[]
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int[] executeBatch() throws SQLException {
		return getStatement().executeBatch();
	}
	/**
	 * @return ResultSet
	 * @param sql String
	 * @throws SQLException If there is a problem
	 */
	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		return new PoolResultSet(getStatement().executeQuery(sql), this);
	}
	/**
	 * Executes an update on the the internal Statement.
	 * @return int
	 * @param sql String
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int executeUpdate(String sql) throws SQLException {
		return getStatement().executeUpdate(sql);
	}
	/**
	 * Returns the internal Connection.
	 * @return Connection
	 */
	@Override
	public Connection getConnection() {
		return myConnection;
	}
	
	public PoolConnection getPoolConnection() {
		return myConnection;
	}

	/**
	 * Returns the fetch direction from the internal Statement.
	 * @return int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int getFetchDirection() throws SQLException {
		return getStatement().getFetchDirection();
	}
	/**
	 * Returns the fetch size from the internal Statement.
	 * @return int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int getFetchSize() throws SQLException {
		return getStatement().getFetchSize();
	}
	/**
	 * Returns the maximum field size of the internal Statement.
	 * @return int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int getMaxFieldSize() throws SQLException {
		return getStatement().getMaxFieldSize();
	}

	/**
	 * Returns the maximum number of rows of the internal Statement.
	 * @return int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int getMaxRows() throws SQLException {
		return getStatement().getMaxRows();
	}

	/**
	 * Returns whether there are more results in the internal Statement.
	 * @return boolean - <code>true</code> if more results exist;
	 * <code>false</code> otherwise.
	 * @throws SQLException If there is a problem
	 */
	@Override
	public boolean getMoreResults() throws SQLException {
		return getStatement().getMoreResults();
	}

	/**
	 * Returns the query timeout (in seconds) from the internal Statement.
	 * @return int - seconds
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int getQueryTimeout() throws SQLException {
		return getStatement().getQueryTimeout();
	}

	/**
	 * @return ResultSet
	 * @throws SQLException If there is a problem
	 */
	@Override
	public ResultSet getResultSet() throws SQLException {
		return new PoolResultSet(getStatement().getResultSet(), this);
	}

	/**
	 * Returns the result set concurrency from the internal Statement.
	 * @return int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int getResultSetConcurrency() throws SQLException {
		return getStatement().getResultSetConcurrency();
	}

	/**
	 * Returns the result set type from the internal Statement.
	 * @return int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int getResultSetType() throws SQLException {
		return getStatement().getResultSetType();
	}

	/**
	 * Returns the internal Statement.
	 * @return Statement
	 */
	protected final Statement getStatement() {
		return myStatement;
	}
	/**
	 * Returns the update count from the internal Statement.
	 * @return int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int getUpdateCount() throws SQLException {
		return getStatement().getUpdateCount();
	}

	/**
	 * Returns the warnings from the internal Statement.
	 * @return SQLWarning
	 * @throws SQLException If there is a problem
	 */
	@Override
	public SQLWarning getWarnings() throws SQLException {
		return getStatement().getWarnings();
	}

	/**
	 * Returns the internal Statement.
	 * @return Statement
	 */
	public Statement getWrappedStatement() {
		return myStatement;
	}

	/**
	 * Does nothing.
	 */
	protected void init() {
		return;
	}

	/**
	 * Sets the cursor name of the internal Statement.
	 * @param name String
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setCursorName(String name) throws SQLException {
		getStatement().setCursorName(name);
	}

	/**
	 * Sets whether escape processing is allowed in the internal Statement.
	 * @param enable should be <code>true</code> if escape processing is 
	 * allowed; <code>false</code> otherwise.
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
		getStatement().setEscapeProcessing(enable);
	}

	/**
	 * Sets the fetch direction of the internal Statement.
	 * @param direction int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setFetchDirection(int direction) throws SQLException {
		getStatement().setFetchDirection(direction);
	}

	/**
	 * Sets the fetch size of the internal Statement.
	 * @param rows int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setFetchSize(int rows) throws SQLException {
		getStatement().setFetchSize(rows);
	}

	/**
	 * Sets the maximum field size of the internal Statement.
	 * @param max int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		getStatement().setMaxFieldSize(max);
	}
	
	/**
	 * Sets the maximum number of rows in the internal Statement.
	 * @param max int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setMaxRows(int max) throws SQLException {
		getStatement().setMaxRows(max);
	}

	/**
	 * Sets the query timeout (in seconds) of the internal Statement.
	 * @param seconds int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		getStatement().setQueryTimeout(seconds);
	}

	@Override
	public boolean execute(String argString, int argInt) throws SQLException {
		return getStatement().execute(argString, argInt);
	}
	
	@Override
	public boolean execute(String argString, int[] argInts) throws SQLException {
		return getStatement().execute(argString, argInts);
	}
	
	@Override
	public boolean execute(String argString, String[] argStrings) throws SQLException {
		return getStatement().execute(argString, argStrings);
	}
	
	@Override
	public int executeUpdate(String argString, int argInt) throws SQLException {
		return getStatement().executeUpdate(argString, argInt);
	}
	
	@Override
	public int executeUpdate(String argString, int[] argInts) throws SQLException {
		return getStatement().executeUpdate(argString, argInts);
	}
	
	@Override
	public int executeUpdate(String argString, String[] argStrings) throws SQLException {
		return getStatement().executeUpdate(argString, argStrings);
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		return new PoolResultSet(getStatement().getGeneratedKeys(), this);
	}
	
	@Override
	public boolean getMoreResults(int argCount) throws SQLException {
		return getStatement().getMoreResults(argCount);
	}
	
	@Override
	public int getResultSetHoldability() throws SQLException {
		return getStatement().getResultSetHoldability();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return getStatement().isClosed();
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return getStatement().isPoolable();
	}

	@Override
	public void setPoolable(boolean argPoolable) throws SQLException {
		getStatement().setPoolable(argPoolable);
	}

	@Override
	public boolean isWrapperFor(Class<?> argIface) throws SQLException {
		return getStatement().isWrapperFor(argIface);
	}

	@Override
	public <T> T unwrap(Class<T> argIface) throws SQLException {
		return getStatement().unwrap(argIface);
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		getStatement().closeOnCompletion();
		
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return getStatement().isCloseOnCompletion();
	}
}
