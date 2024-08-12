package com.siliconage.database;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import com.siliconage.util.FactoryException;
import com.siliconage.util.Pool;
import com.siliconage.util.UnimplementedOperationException;

/**
 * Creates a pool of connections to a database.
 * <BR><BR>
 * Copyright &copy; 2000 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class DirectConnectionPool implements javax.sql.DataSource {
//	private static final org.apache.log4j.Logger ourLogger = org.apache.log4j.Logger.getLogger(DirectConnectionPool.class.getName());
	
	//	private final ArrayList myConnections = new ArrayList();
	
	private final String myConnectString;
	private final String myUsername;
	private final String myPassword;
	
	private PrintWriter myPrintWriter;
	
	private final Pool<Connection> myPoolConnections;
	
	/**
	 * Creates a new ConnectionPool using the argument connection string.
	 * @param argConnectString String
	 */
	protected DirectConnectionPool(String argConnectString) {
		super();
		
		if (argConnectString == null) {
			throw new IllegalArgumentException("argConnectString is null");
		}
		
		myConnectString = argConnectString;
		myUsername = null;
		myPassword = null;
		
		myPoolConnections = new Pool<>(new ConnectionFactory(this));
	}
	
	protected DirectConnectionPool(String argConnectString, String argUsername, String argPassword) {
		super();
		
		if (argConnectString == null) {
			throw new IllegalArgumentException("argConnectionString is null");
		}
		
		myConnectString = argConnectString;
		myUsername = argUsername;
		myPassword = argPassword;
		
		myPoolConnections = new Pool<>(new ConnectionFactory(this));
	}
	
	public String getConnectString() {
		return myConnectString;
	}
	
	public String getUsername() {
		return myUsername;
	}
	
	public String getPassword() {
		return myPassword;
	}
		
	public synchronized void close() {
//		ourLogger.debug("Closing down ConnectionPool...");
//		ArrayList lclConnections = getConnections();
//		if (lclConnections == null) {
//			return;
//		}
//
//		ArrayList lclList = getConnections();
//
//		synchronized (lclList) {
//			Iterator lclI = getConnections().iterator();
//			while (lclI.hasNext()) {
//				try {
//					PoolConnection lclPC = (PoolConnection) lclI.next();
//					if (lclPC != null) {
//						synchronized (lclPC) {
//							Connection lclConnection = lclPC.getConnection();
//							synchronized (lclConnection) {
//								if (lclConnection.isClosed()) {
//									ourLogger.debug(lclConnection + " is already closed");
//								} else {
//									ourLogger.debug("Closing " + lclConnection);
//									DatabaseUtility.closeConnection(lclConnection);
//								}
//							}
//						}
//					}
//				} catch (SQLException lclE) {
//					ourLogger.error("Exception caught while closing connections while closing connection pool.");
//					lclE.error(System.err);
//				}
//				lclI.remove();
//			}
//		}
//		if (lclList.size() != 0) {
//			throw new IllegalStateException("ConnectionPool was not reduced to zero stored PoolConnections.");
//		}
//		ourLogger.debug("ConnectionPool emptied.");
	}

	/*
	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} catch (Throwable lclT) {
			ourLogger.error("Squashing Throwable while finalizing " + this);
			ourLogger.error(lclT.toString(), lclT);
		}
		super.finalize();
	}
	*/
	
	/**
	 * @return Connection
	 * @throws SQLException if there is a problem creating the connection
	 */
	@Override
	public Connection getConnection() throws SQLException  {
		try {
			return myPoolConnections.get();
		} catch (FactoryException lclE) {
			if (lclE.getCause() instanceof SQLException) {
				throw (SQLException) lclE.getCause();
			} else {
				throw new RuntimeException("Could not instantiate new Connection", lclE);
			}
		}
	}
	
	/**
	 * @param argPC PoolConnection
	 * @throws IllegalArgumentException if the argument is <code>null</code>
	 */
	public void returnPoolConnection(PoolConnection argPC) {
		if (argPC == null) {
			throw new IllegalArgumentException("argPC is null");
		}
		myPoolConnections.put(argPC);
	}
	
	/**
	 * Returns the name of this class.
	 * @return String
	 */
	@Override
	public String toString() {
		return getClass().getName();
	}
	
	/* (non-Javadoc)
	 * @see javax.sql.DataSource#getLoginTimeout()
	 */
	@Override
	public int getLoginTimeout() {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.sql.DataSource#setLoginTimeout(int)
	 */
	@Override
	public void setLoginTimeout(int arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.sql.DataSource#getLogWriter()
	 */
	@Override
	public PrintWriter getLogWriter() {
		return myPrintWriter;
	}
	
	/* (non-Javadoc)
	 * @see javax.sql.DataSource#setLogWriter(java.io.PrintWriter)
	 */
	@Override
	public void setLogWriter(PrintWriter argPW) {
		myPrintWriter = argPW;
	}
	
	/* (non-Javadoc)
	 * @see javax.sql.DataSource#getConnection(java.lang.String, java.lang.String)
	 */
	@Override
	public Connection getConnection(String arg0, String arg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public boolean isWrapperFor(Class<?> argIface) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public <T> T unwrap(Class<T> argIface) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}
	
//	@Override
//	public Logger getParentLogger() {
//		throw new UnimplementedOperationException();
//	}
}
