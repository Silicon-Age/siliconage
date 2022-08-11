package com.siliconage.database;
import java.util.Map;

import javax.sql.DataSource;

import com.siliconage.util.Factory;

/**
 * @author topquark
 */
public final class DirectConnectionPoolFactory implements Factory<DataSource> {

	private static final DirectConnectionPoolFactory ourInstance = new DirectConnectionPoolFactory();
	
	public static DirectConnectionPoolFactory getInstance() { return ourInstance; }
	
	private DirectConnectionPoolFactory() {
		super();
	}
	
	@SuppressWarnings("static-method")
	public DataSource create(String argConnectionString) {
		if (argConnectionString == null) {
			throw new IllegalArgumentException("argConnectionString is null");
		}
		return new DirectConnectionPool(argConnectionString);
	}
	
	@SuppressWarnings("static-method")
	public DataSource create(String argConnectionString, String argUser, String argPassword) {
		if (argConnectionString == null) {
			throw new IllegalArgumentException("argConnectionString is null");
		}
		if (argUser == null) {
			throw new IllegalArgumentException("argUser is null");
		}
		if (argPassword == null) {
			throw new IllegalArgumentException("argPassword is null");
		}
		
		return new DirectConnectionPool(argConnectionString, argUser, argPassword);
	}
	
	@Override
	public DataSource create() {
		throw new UnsupportedOperationException("Cannot create " + DirectConnectionPool.class.getName() + " objects without passing parameters.");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public DataSource create(Object argParameters) {
		if (argParameters instanceof String) {
			return create((String) argParameters);
		} else if (argParameters instanceof String[]) {
			String[] lclS = (String[]) argParameters;
			if (lclS.length != 3) {
				throw new IllegalArgumentException("If a String[] is passed to create(Object) its length must be exactly 3");
			}
			return create(lclS[0], lclS[1], lclS[2]);
		} else if (argParameters instanceof Map<?, ?>) {
			Map<String, Object> argParameterMap = (Map<String, Object>) argParameters;
			return create(argParameterMap);
//		} else if (argParameters instanceof ReadableStringKeyMap) {
//			return create((ReadableStringKeyMap) argParameters);
		} else {
			throw new IllegalArgumentException("The parameter passed to create(Object) must be a String, String[3], Map, or StringKeyMap.");
		} 
	}
	
	@Override
	public DataSource create(Map<String, Object> argMap) {
		if (argMap == null) {
			throw new IllegalArgumentException("argMap is null");
		}
		return create(
			(String) argMap.get("CONNECT_STRING"),
			(String) argMap.get("USERNAME"),
			(String) argMap.get("PASSWORD")
		);
	}
	
//	public DataSource create(ReadableStringKeyMap argSKM) {
//		if (argSKM == null) {
//			throw new IllegalArgumentException("argSKM is null");
//		}
//		return create(
//			argSKM.getString("CONNECT_STRING"),
//			argSKM.getString("USERNAME"),
//			argSKM.getString("PASSWORD")
//		);
//	}
}
