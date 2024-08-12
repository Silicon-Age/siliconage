package com.siliconage.database;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.sql.CallableStatement;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Savepoint;
import java.sql.Struct;

/**
 * An implementation of java.sql.Connection that does nothing.
 * <BR><BR>
 * Copyright &copy; 2000 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public final class NullConnection implements Connection {
	private final static NullConnection myInstance = new NullConnection();
	
	/**
	 * Default NullConnection constructor.
	 */
	public NullConnection() {
		super();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void clearWarnings() {
			throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void close() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Statement createStatement() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return Statement
	 * @param resultSetType Ignored
	 * @param resultSetConcurrency Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean getAutoCommit() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public String getCatalog() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns an instance of NullConnection.
	 * @return NullConnection
	 */
	public static NullConnection getInstance() {
		return myInstance;
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public DatabaseMetaData getMetaData() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public int getTransactionIsolation() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public Map<String, Class<?>> getTypeMap() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public SQLWarning getWarnings() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean isClosed() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean isReadOnly() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @param sql Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public String nativeSQL(String sql) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @param sql Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public CallableStatement prepareCall(String sql) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @param sql Ignored
	 * @param resultSetType Ignored
	 * @param resultSetConcurrency Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @param sql Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public PreparedStatement prepareStatement(String sql) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return No return
	 * @param sql Ignored
	 * @param resultSetType Ignored
	 * @param resultSetConcurrency Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void rollback() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param autoCommit Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setAutoCommit(boolean autoCommit) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param catalog Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setCatalog(String catalog) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param readOnly Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setReadOnly(boolean readOnly) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param level Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setTransactionIsolation(int level) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param map Ignored
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setTypeMap(Map<String, Class<?>> map) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Statement createStatement(int argA, int argB, int argC) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int getHoldability() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public CallableStatement prepareCall(String argString, int argA, int argB, int argC) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public PreparedStatement prepareStatement(String argString, int argA, int argB, int argC) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public PreparedStatement prepareStatement(String argString, int argA) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public PreparedStatement prepareStatement(String argString, int[] argInts) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public PreparedStatement prepareStatement(String argString, String[] argStrings) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void releaseSavepoint(Savepoint argSavepoint) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void rollback(Savepoint argSavepoint) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setHoldability(int argHoldability) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Savepoint setSavepoint() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Savepoint setSavepoint(String argString) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Array createArrayOf(String argArg0, Object[] argArg1) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Blob createBlob() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Clob createClob() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public NClob createNClob() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public SQLXML createSQLXML() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Struct createStruct(String argArg0, Object[] argArg1) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Properties getClientInfo() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getClientInfo(String argArg0) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isValid(int argArg0) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setClientInfo(Properties argArg0) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setClientInfo(String argArg0, String argArg1) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isWrapperFor(Class<?> argIface) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public <T> T unwrap(Class<T> argIface) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void abort(Executor argArg0) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int getNetworkTimeout() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getSchema() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setNetworkTimeout(Executor argArg0, int argArg1) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setSchema(String argArg0) {
		throw new UnsupportedOperationException();
	}
}
