package com.siliconage.database;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Savepoint;
import java.sql.Struct;
import javax.sql.DataSource;

/**
 * Copyright &copy; 2000 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */

@SuppressWarnings("resource")
/* package */ class PoolConnection implements Connection {
	
	private final Connection myConnection;
	private final DataSource myDataSource;
	private boolean myCheckedIn;
	 
	/**
	 * PoolConnection constructor that sets the value of this Connection's
	 * internal ConnectionPool, Connection, and checked-in status 
	 * (to <code>false</code>).
	 * @param argDS The DataSource
	 * @param argConnection The Connection
	 */
	/* package*/ PoolConnection(DataSource argDS, Connection argConnection) {
		super();
		if (argDS == null) {
			throw new IllegalArgumentException("argDS is null");
		}
		if (argConnection == null) {
			throw new IllegalArgumentException("argConnection is null");
		}
		myDataSource = argDS;
		myConnection = argConnection;
		setCheckedIn(false);
	}
	
	/**
	 * If the internal ConnectionPool is <code>null</code>, the internal
	 * Connection is closed.  Otherwise, this object is returned to the
	 * ConnectionPool.
	 * @throws SQLException if there is a problem
	 */
	public void checkIn() throws SQLException {
		DataSource lclDS = getDataSource();
		if (lclDS == null) {
			getConnection().close();
		} else {
			((DirectConnectionPool) lclDS).returnPoolConnection(this); // THINK: Fix
		}
	}
	
	/**
	 * Clears the warnings from the internal Collection.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void clearWarnings() throws SQLException {
		getConnection().clearWarnings();
	}
	
	/**
	 * Calls checkIn().
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void close() throws SQLException {
		checkIn();
	}
	
	/**
	 * Commits the internal Connection.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void commit() throws SQLException {
		getConnection().commit();
	}
	
	/**
	 * @return Statement
	 * @throws SQLException if there is a problem
	 * @throws RuntimeException if the Connection is already checked-in.
	 */
	@Override
	public Statement createStatement() throws SQLException {
		if (isCheckedIn()) {
			throw new RuntimeException("Tried to createStatement on a checked-in Connection.");
		}
		
		return new PoolStatement(getConnection().createStatement(), this);
	}
	
	/**
	 * @return Statement
	 * @throws SQLException if there is a problem
	 * @throws RuntimeException if the Connection is already checked-in.
	 */
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		if (isCheckedIn()) {
			throw new RuntimeException("Tried to createStatement on a checked-in Connection.");
		}
	
		return new PoolStatement(getConnection().createStatement(resultSetType, resultSetConcurrency), this);
	}
	
	/**
	 * @return the auto-commite status of the internal Connection
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean getAutoCommit() throws SQLException {
		return getConnection().getAutoCommit();
	}
	
	/**
	 * @return the catalog of the internal Connection
	 * @throws SQLException if there is a problem
	 */
	@Override
	public String getCatalog() throws SQLException {
		return getConnection().getCatalog();
	}
	
	/**
	 * @return The internal Connection
	 */
	public final Connection getConnection() {
		return myConnection;
	}
	
	/**
	 * @return The DataSource
	 */
	public final DataSource getDataSource() {
		return myDataSource;
	}
	
	/**
	 * @return The metadata from the internal Connection
	 * @throws SQLException if there is a problem
	 */
	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return getConnection().getMetaData();
	}
	
	/**
	 * Returns the transactional isolation of the internal Connection.
	 * @return The isolation level of the transaction
	 * @throws SQLException if there is a problem
	 */
	@Override
	public int getTransactionIsolation() throws SQLException {
		return getConnection().getTransactionIsolation();
	}
	
	/**
	 * @return the type Map of the internal Connection
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return getConnection().getTypeMap();
	}
	
	/**.
	 * @return the warnings of the internal Connection
	 * @throws SQLException if there is a problem
	 */
	@Override
	public SQLWarning getWarnings() throws SQLException {
		return getConnection().getWarnings();
	}
	
	/**
	 * @return <code>true</code> if the internal Connection is checked-in; <code>false</code> otherwise
	 */
	public final boolean isCheckedIn() {
		return myCheckedIn;
	}
	
	/**
	 * @return <code>true</code> if the internal Connection is closed; <code>false</code> otherwise
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean isClosed() throws SQLException {
		return getConnection().isClosed();
	}
	
	/**
	 * @return <code>true</code> if the internal Connection is read-only; <code>false</code> otherwise.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean isReadOnly() throws SQLException {
		return getConnection().isReadOnly();
	}
	
	/**
	 * @return the nativeSQL of the internal Connection
	 * @param sql String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public String nativeSQL(String sql) throws SQLException {
		return getConnection().nativeSQL(sql);
	}
	
	/**
	 * @return the prepared CallableStatement
	 * @param sql The SQL statement
	 * @throws SQLException if there is a problem
	 */
	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		return getConnection().prepareCall(sql);
	}
	
	/**
	 * @return CallableStatement
	 * @param sql The SQL statement
	 * @param resultSetType The type of the expected ResultSet
	 * @param resultSetConcurrency The concurrency for obtaining the ResultSet
	 * @throws SQLException if there is a problem
	 */
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return getConnection().prepareCall(sql, resultSetType, resultSetConcurrency);
	}
	
	/**
	 * @return a PreparedStatement based on sql
	 * @param sql The SQL statement
	 * @throws SQLException if there is a problem
	 * @throws RuntimeException if the internal Connection is already checked-in.
	 */
	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		if (isCheckedIn()) {
			throw new RuntimeException("Tried to prepareStatement on a checked-in Connection.");
		}
		return new PoolPreparedStatement(getConnection().prepareStatement(sql), this);
	}
	
	/**
	 * @return a PreparedStatement based on sql
	 * @param sql The SQL statement
	 * @param resultSetType The type of the expected ResultSet
	 * @param resultSetConcurrency The concurrency for obtaining the ResultSet
	 * @throws SQLException if there is a problem
	 * @throws RuntimeException if the internal Connection is already checked-in.
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		if (isCheckedIn()) {
			throw new RuntimeException("Tried to prepareStatement on a checked-in Connection.");
		}
		return new PoolPreparedStatement(getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency), this);
	}
	
	/**
	 * Rolls back the internal Connection.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void rollback() throws SQLException {
		getConnection().rollback();
	}
	
	/**
	 * Sets the auto-commit status of the internal Connection.
	 * @param autoCommit should be set to <code>true</code> to force auto-commit
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		getConnection().setAutoCommit(autoCommit);
	}
	
	/**
	 * @param catalog the catalog for the internal Connection
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void setCatalog(String catalog) throws SQLException {
		getConnection().setCatalog(catalog);
	}
	
	/**
	 * @param argCheckedIn the new checked-in status for the internal Connection
	 */
	void setCheckedIn(boolean argCheckedIn) {
		myCheckedIn = argCheckedIn;
	}
	
	/**
	 * @param readOnly <code>true</code> to force the read-only status of the internal Connection to be read-only; false for the opposite
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		getConnection().setReadOnly(readOnly);
	}
	
	/**
	 * @param level The transaction isolation level for the internal Connection
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		getConnection().setTransactionIsolation(level);
	}
	
	/**
	 * @param map The type Map for the internal Connection
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		getConnection().setTypeMap(map);
	}
	
	@Override
	public Statement createStatement(int argA, int argB, int argC) throws SQLException {
		return getConnection().createStatement(argA, argB, argC);
	}
	
	@Override
	public int getHoldability() throws SQLException {
		return getConnection().getHoldability();
	}
	
	@Override
	public CallableStatement prepareCall(String argString, int argA, int argB, int argC) throws SQLException {
		return getConnection().prepareCall(argString, argA, argB, argC);
	}
	
	@Override
	public PreparedStatement prepareStatement(String argString, int argA, int argB, int argC) throws SQLException {
		return getConnection().prepareStatement(argString, argA, argB, argC);
	}
	
	@Override
	public PreparedStatement prepareStatement(String argString, int argA) throws SQLException {
		return getConnection().prepareStatement(argString, argA);
	}
	
	@Override
	public PreparedStatement prepareStatement(String argString, int[] argInts) throws SQLException {
		return getConnection().prepareStatement(argString, argInts);
	}
	
	@Override
	public PreparedStatement prepareStatement(String argString, String[] argStrings) throws SQLException {
		return getConnection().prepareStatement(argString, argStrings);
	}
	
	@Override
	public void releaseSavepoint(Savepoint argSavepoint) throws SQLException {
		getConnection().releaseSavepoint(argSavepoint);
	}
	
	@Override
	public void rollback(Savepoint argSavepoint)  throws SQLException {
		getConnection().rollback(argSavepoint);
	}
	
	@Override
	public void setHoldability(int argHoldability) throws SQLException {
		getConnection().setHoldability(argHoldability);
	}
	
	@Override
	public Savepoint setSavepoint() throws SQLException {
		return getConnection().setSavepoint();
	}
	
	@Override
	public Savepoint setSavepoint(String argString) throws SQLException {
		return getConnection().setSavepoint(argString);
	}
	
	@Override
	public Array createArrayOf(String argTypeName, Object[] argElements) throws SQLException {
		return getConnection().createArrayOf(argTypeName, argElements);
	}
	
	@Override
	public Blob createBlob() throws SQLException {
		return getConnection().createBlob();
	}
	
	@Override
	public Clob createClob() throws SQLException {
		return getConnection().createClob();
	}
	
	@Override
	public NClob createNClob() throws SQLException {
		return getConnection().createNClob();
	}
	
	@Override
	public SQLXML createSQLXML() throws SQLException {
		return getConnection().createSQLXML();
	}
	
	@Override
	public Struct createStruct(String argTypeName, Object[] argAttributes) throws SQLException {
		return getConnection().createStruct(argTypeName, argAttributes);
	}
	
	@Override
	public Properties getClientInfo() throws SQLException {
		return getConnection().getClientInfo();
	}
	
	@Override
	public String getClientInfo(String argName) throws SQLException {
		return getConnection().getClientInfo(argName);
	}
	
	@Override
	public boolean isValid(int argTimeout) throws SQLException {
		return getConnection().isValid(argTimeout);
	}
	
	@Override
	public void setClientInfo(Properties argProperties) throws SQLClientInfoException {
		getConnection().setClientInfo(argProperties);
	}
	
	@Override
	public void setClientInfo(String argName, String argValue) throws SQLClientInfoException {
		getConnection().setClientInfo(argName, argValue);
	}
	
	@Override
	public boolean isWrapperFor(Class<?> argIface) throws SQLException {
		return getConnection().isWrapperFor(argIface);
	}
	
	@Override
	public <T> T unwrap(Class<T> argIface) throws SQLException {
		return getConnection().unwrap(argIface);
	}
	
	@Override
	public void abort(Executor argExecutor) throws SQLException {
		getConnection().abort(argExecutor);
	}
	
	@Override
	public int getNetworkTimeout() throws SQLException {
		return getConnection().getNetworkTimeout();
	}
	
	@Override
	public String getSchema() throws SQLException {
		return getConnection().getSchema();
	}
	
	@Override
	public void setNetworkTimeout(Executor argExecutor, int argMilliseconds) throws SQLException {
		getConnection().setNetworkTimeout(argExecutor, argMilliseconds);
	}
	
	@Override
	public void setSchema(String argSchema) throws SQLException {
		getConnection().setSchema(argSchema);
	}
}
