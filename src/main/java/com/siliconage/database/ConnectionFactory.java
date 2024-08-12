package com.siliconage.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import com.siliconage.util.Factory;
import com.siliconage.util.FactoryException;

/**
 * @author topquark
 */
/* package */ class ConnectionFactory implements Factory<Connection> {
	
	private final DirectConnectionPool myDirectConnectionPool;
	
	public ConnectionFactory(DirectConnectionPool argDCP) {
		super();
		if (argDCP == null) {
			throw new IllegalArgumentException("argDCP is null");
		}
		myDirectConnectionPool = argDCP;
	}
	
	/* (non-Javadoc)
	 * @see com.siliconage.util.Factory#create()
	 */
	@SuppressWarnings("resource")
	@Override
	public Connection create() throws FactoryException {
		try {
			Connection lclConnection;
			if (getUsername() == null) {
				lclConnection = DriverManager.getConnection(getConnectString());
			} else {
				lclConnection = DriverManager.getConnection(getConnectString(), getUsername(), getPassword());
			}
			lclConnection.setAutoCommit(false);
			return new PoolConnection(getDirectConnectionPool(), lclConnection);
		} catch (SQLException lclE) {
			throw new FactoryException("Could not create database connection", lclE);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.siliconage.util.Factory#create(java.lang.Object)
	 */
	@Override
	public Connection create(Object argParameters) throws FactoryException {
		return create();
	}
	
	/* (non-Javadoc)
	 * @see com.siliconage.util.Factory#create(java.util.Map)
	 */
	@Override
	public Connection create(Map<String, Object> argParameters) throws FactoryException {
		return create();
	}
	
//	public Connection create(ReadableStringKeyMap argParameters) throws FactoryException {
//		return create();
//	}
//
	public DirectConnectionPool getDirectConnectionPool() {
		return myDirectConnectionPool;
	}
	
	public String getConnectString() {
		return getDirectConnectionPool().getConnectString();
	}
	
	public String getUsername() {
		return getDirectConnectionPool().getUsername();
	}
	
	public String getPassword() {
		return getDirectConnectionPool().getPassword();
	}
}
