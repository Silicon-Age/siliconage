package com.opal;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.siliconage.util.UnimplementedOperationException;

public class MoneDataSource implements DataSource {

	/* This is a singleton class of which only one instance should ever exist.  Clients of this class
	should not create their own instances using a constructor, but should instead invoke the static
	method getInstance() to access the singleton instance. */

	/* A static reference to the only instance of this class, which is constructed on class load. */

	private static final MoneDataSource ourInstance = new MoneDataSource();

	/* A static accessor to obtain a reference to the singleton instance. */

	public static final MoneDataSource getInstance() {
		return ourInstance;
	}

	private MoneDataSource() {
		super();
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return null;
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}

	@Override
	public void setLogWriter(PrintWriter argArg0) throws SQLException {
		throw new UnimplementedOperationException();
	}

	@Override
	public void setLoginTimeout(int argArg0) throws SQLException {
		throw new UnimplementedOperationException();
	}

	@Override
	public boolean isWrapperFor(Class<?> argArg0) throws SQLException {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> argArg0) throws SQLException {
		return null;
	}

	@Override
	public Connection getConnection() throws SQLException {
		throw new UnimplementedOperationException();
	}

	@Override
	public Connection getConnection(String argUsername, String argPassword) throws SQLException {
		throw new UnimplementedOperationException();
	}

}
