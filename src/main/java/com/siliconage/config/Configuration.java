package com.siliconage.config;

import java.io.InputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.database.DatabaseUtility;
import com.siliconage.database.DirectConnectionPoolFactory;

/**
 * Copyright &copy; 2001 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public abstract class Configuration extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;
	
	private static final org.apache.log4j.Logger ourLogger = org.apache.log4j.Logger.getLogger(Configuration.class.getName());

	private DataSource myDataSource;
	
	public static final int NOT_FOUND_INT_VALUE = 0;
	public static final long NOT_FOUND_LONG_VALUE = 0L;
	public static final float NOT_FOUND_FLOAT_VALUE = 0.0F;
	public static final double NOT_FOUND_DOUBLE_VALUE = 0.0;
	public static final short NOT_FOUND_SHORT_VALUE = (short) 0;
	public static final byte NOT_FOUND_BYTE_VALUE = (byte) 0;
	public static final boolean NOT_FOUND_BOOLEAN_VALUE = false;
	public static final String NOT_FOUND_STRING_VALUE = null;
	public static final Object NOT_FOUND_OBJECT_VALUE = null;
	public static final LocalDate NOT_FOUND_LOCALDATE_VALUE = null;
	
	/* The names of database columns in the database table of configuration constants */
	protected static final String KEY_COLUMN_NAME = "name";
	protected static final String VALUE_COLUMN_NAME = "value";
	
	/* The name of the server-specific properties file. */
	private static final String SERVER_PROPERTIES_FILENAME = "server.properties";
	
	/* The name of the application-specific properties file. */
	private static final String APPLICATION_PROPERTIES_FILENAME = "application.properties";
	
	/* The names of the properties that will be in one of those files, giving information on how to
	 * connect to the database. */
	private static final String CONNECT_STRING_PROPERTY_NAME = "CONNECT_STRING";
	private static final String CONFIGURATION_TABLE_NAME_PROPERTY_NAME = "CONFIGURATION_TABLE";
	private static final String USERNAME_PROPERTY_NAME = "USERNAME";
	private static final String PASSWORD_PROPERTY_NAME = "PASSWORD";
	private static final String DATABASE_DRIVER_CLASS_NAME_PROPERTY_NAME = "DATABASE_DRIVER";
	
	/**
	 * creates a new AbstractConfiguration, initializes it, and loads data from
	 * the properties file and from the database.
	 * @throws RuntimeException if loadFromProperties() or 
	 * loadFromDatabase() returns an Exception.
	 */
	protected Configuration() {
		try {
			initialize();
			loadAll();
		} catch (IOException lclE) {
			ourLogger.error("Unable to create Configuration", lclE);
			throw new RuntimeException("Unable to load properties from file system.", lclE);
		} catch (SQLException lclE) {
			ourLogger.error("Unable to create Configuration", lclE);
			throw new RuntimeException("Unable to load configuration constants from database.", lclE);
		} catch (Exception lclE) {
			ourLogger.error("Unable to create Configuration", lclE);
			throw new RuntimeException("Unable to load configuration data.", lclE);
		}
	}
	
	protected void loadAll() throws IOException, SQLException {
		if (getServerPropertiesFilename() != null) {
			loadFromProperties(getServerPropertiesFilename());
		}
		
		if (getConfigurationTableName() != null) {
			loadFromDatabase();
		}
		
		if (getApplicationPropertiesFilename() != null) {
			loadFromProperties(getApplicationPropertiesFilename());
		}
	}
	
	public void reloadAll() throws IOException, SQLException {
		loadAll();
	}
	
//	/**
//	 * Returns whether <code>null</code> keys are allowed.
//	 * @return boolean - always <code>false</code>
//	 */
//	@Override
//	public boolean areNullKeysAllowed() {
//		return false;
//	}
//	
//	/**
//	 * Returns whether <code>null</code> values are allowed.
//	 * @return boolean - always <code>false</code>
//	 */
//	@Override
//	public boolean areNullValuesAllowed() {
//		return false;
//	}
	
	public static String isValidCode(String argCode) {
		if (argCode == null) {
			return "Codes must be non-null.";
		}
		
		if (StringUtils.isBlank(argCode)) {
			return "Codes must be non-blank.";
		}
		
		for (char lclC : argCode.toCharArray()) {
			if (Character.isDigit(lclC) || Character.isUpperCase(lclC) || lclC == '_' || lclC == '-') {
				continue;
			} else {
				return "Code '" + argCode + "' contains an invalid character.  Only digits, uppercase letters, underscores, and hyphens are permitted.";
			}
		}
		
		return null;
	}
	
	/*
	 * Determines if the argument String is a valid database code.
	 * Database codes must be non-null, all caps, and have no spaces.
	 * @param argCode String
	 * @throws IllegalArgumentException if argCode is <code>null</code>, 
	 * argCode has any spaces, or argCode is not all caps
	 */
	
	public static void checkValidCode(String argCode) {
		String lclErrorMessage = isValidCode(argCode);
		if (lclErrorMessage != null) {
			throw new IllegalArgumentException(lclErrorMessage);
		}
	}
	
	/**
	 * Returns the name of the properties file.
	 * @return String
	 */
	protected String getApplicationPropertiesFilename() {
		return APPLICATION_PROPERTIES_FILENAME;
	}
	
	/**
	 * Returns the name of the configuration table in the database.
	 * @return String
	 */
	public String getConfigurationTableName() {
		return getString(getConfigurationTableNamePropertyName());
	}
	
	public String getConfigurationTableNamePropertyName() {
		return CONFIGURATION_TABLE_NAME_PROPERTY_NAME;
	}
	
	/**
	 * Returns the internal ConnectionPool.  Will not be <code>null</code>.
	 * @return ConnectionPool
	 */
	public synchronized DataSource getDataSource() {
		if (myDataSource == null) {
			myDataSource = createDataSource();
		}
		return myDataSource;
	}
	
	public String getConnectString() {
		return getString(getConnectStringPropertyName());
	}
	
	public String getConnectStringPropertyName() {
		return CONNECT_STRING_PROPERTY_NAME;
	}
	
	/**
	 * Returns an Iterator on the keys of the internal map.
	 * @return Iterator
	 */
	public Iterator<String> getKeyIterator() {
		return keySet().iterator();
	}
	
	/**
	 * Returns the Object associated with the argument code.
	 * @return Object
	 * @param argCode database code
	 */
	public Object getObject(String argCode) {
		checkValidCode(argCode);
		return get(argCode);
	}
	
	public String getPassword() {
		return getString(getPasswordPropertyName()); // may be null
	}
	
	protected String getPasswordPropertyName() {
		return PASSWORD_PROPERTY_NAME;
	}
	
	/**
	 * Returns the name of the properties file.
	 * @return String
	 */
	protected String getServerPropertiesFilename() {
		return SERVER_PROPERTIES_FILENAME;
	}
	
	public String getUsername() {
		return getString(getUsernamePropertyName()); // may be null
	}
	
	protected String getUsernamePropertyName() {
		return USERNAME_PROPERTY_NAME;
	}
	
	
	protected void initialize() {
		/* By default, there is no initialization to do. */
	}
	
	protected DataSource createDataSource() {
		// ourLogger.debug("Instantiating a ConnectionPool for connect string \"" + getConnectString() + "\".");
		registerDatabaseDrivers();
		return DirectConnectionPoolFactory.getInstance().create(
			getConnectString(),
			getUsername(),
			getPassword()
		);
	}
	/**
	 * Returns whether items are case sensitive.
	 * @return boolean - always <code>true</code>
	 */
	public boolean isCaseSensitive() {
		return true;
	}
	
	/**
	 * Loads configuration items from the database.
	 * @throws SQLException if there is a problem connecting to the database
	 * @throws RuntimeException if the Oracle JDBC drivers could
	 * not be registered
	 * @throws IllegalStateException if the connect string, username, or
	 * password is <code>null</code>
	 */
	protected void loadFromDatabase() throws SQLException {
		DataSource lclS = Validate.notNull(getDataSource());
		
		try (Connection lclC = lclS.getConnection()) {
			try (ResultSet lclRS = DatabaseUtility.select(lclC, "SELECT * FROM " + getConfigurationTableName())) {
				while (lclRS.next()) {
					String lclKey = Validate.notNull(lclRS.getString(KEY_COLUMN_NAME), "Encountered a configuration entry in the database with a null key");
					checkValidCode(lclKey);
					
					String lclValueString = lclRS.getString(VALUE_COLUMN_NAME); // may be null
					
					put(lclKey, lclValueString); // This stores it as a String.  You'd think we'd want to convert it to the right kind, but java.util.Properties only returns Strings, so this is the same as what the loadFromProperties method does.
				}
			}
		}
	}
	
	public void reloadFromDatabase() throws SQLException {
		loadFromDatabase();
	}
	
	protected InputStream getPropertiesInputStream(String argFilename) {
		InputStream lclIS = this.getClass().getResourceAsStream(argFilename);
		
		if (lclIS == null) {
			 lclIS = ClassLoader.getSystemResourceAsStream(argFilename);
			 if (lclIS == null) {
				throw new IllegalStateException("Could not get \"" + argFilename + "\" as a stream.");
			 }
		}
		
		return lclIS;
	}
	
	/**
	 * Loads items from the properties file.
	 * @param argFilename the name of the properties file from which to load
	 * @throws IOException if there is a I/O problem while loading.
	 * @throws IllegalStateException if the input stream obtained is <code>null</code>
	 */
	protected void loadFromProperties(String argFilename) throws IOException {
		try (InputStream lclIS = getPropertiesInputStream(argFilename)) {
			Properties lclProperties = new Properties();
			
			lclProperties.load(lclIS);
			
			for (Object lclKeyObj : lclProperties.keySet()) {
				Validate.isTrue(lclKeyObj instanceof String, "Properties key '" + lclKeyObj + "' is not a String");
				String lclKey = (String) lclKeyObj;
				checkValidCode(lclKey);
				
				// ourLogger.debug(lclKey + " = " + lclProperties.getProperty(lclKey));
				put(lclKey, lclProperties.getProperty(lclKey));
			}
		} // autoclose lclIS
	}
	
	public void reloadFromProperties(String argFilename) throws IOException {
		loadFromProperties(argFilename);
	}
	
	protected void registerDatabaseDrivers() {
		String lclDatabaseDriverClassName = getDatabaseDriverClassName();
		if (lclDatabaseDriverClassName == null) {
			return;
		} else {
			// ourLogger.debug("Registering database drivers for class \"" + lclDatabaseDriverClassName + "\".");
			try {
				Class.forName(lclDatabaseDriverClassName);
			} catch (ClassNotFoundException lclE) {
				throw new RuntimeException("Could not call Class.forName(...) for the database driver class name \"" + lclDatabaseDriverClassName + "\".", lclE);
			}
		}
	}
	
	protected String getDatabaseDriverClassName() {
		return getString(getDatabaseDriverClassNamePropertyName());
	}
	
	public String getDatabaseDriverClassNamePropertyName() {
		return DATABASE_DRIVER_CLASS_NAME_PROPERTY_NAME;
	}
	
	/**
	 * Sets the internal ConnectionPool.
	 * @param argConnectionPool ConnectionPool
	 */
//	protected void setDataSource(DataSource argDataSource) {
//		myDataSource = argDataSource;
//	}
	
	/**
	 * Puts the argument Object into the internal map with
	 * the argument String as its key.
	 * @param argCode String
	 * @param argValue Object
	 */
	public void setObject(String argCode, Object argValue) {
		checkValidCode(argCode);
		put(argCode, argValue);
	}
	
	public boolean getBoolean(String argKey) {
		return getBoolean(argKey, NOT_FOUND_BOOLEAN_VALUE);
	}
	
	public boolean getBoolean(String argKey, boolean argDefault) {
		Object lclObject = getObject(argKey);
		if (lclObject == null) {
			return argDefault;
		}
		try {
			return ((Boolean) lclObject).booleanValue();
		} catch (ClassCastException lclE) {
			if (lclObject instanceof Number) {
				return ((Number) lclObject).intValue() != 0;
			} else {
				String lclS = lclObject.toString();
				if ("TRUE".equalsIgnoreCase(lclS)) {
					return true;
				} else if ("YES".equalsIgnoreCase(lclS)) {
					return true;
				} else if ("ON".equalsIgnoreCase(lclS)) {
					return true;
				} else {
					return false;
				}
			}
		}
	}
	
	public byte getByte(String argKey) {
		return getByte(argKey, NOT_FOUND_BYTE_VALUE);
		}
	
	public byte getByte(String argKey, byte argDefault) {
		Object lclObject = getObject(argKey);
		if (lclObject == null) {
			return argDefault;
		}
		try {
				return ((Number) lclObject).byteValue();
		} catch (ClassCastException lclE) {
				return Byte.parseByte(lclObject.toString());
		}
	}
	
	public double getDouble(String argKey) {
		return getDouble(argKey, NOT_FOUND_DOUBLE_VALUE);
	}
	
	public double getDouble(String argKey, double argDefault) {
		Object lclObject = getObject(argKey);
		if (lclObject == null) {
			return argDefault;
		}
		try {
			return ((Number) lclObject).doubleValue();
		} catch (ClassCastException lclE) {
			return Double.parseDouble(lclObject.toString());
		}
	}
	
	public float getFloat(String argKey) {
		return getFloat(argKey, NOT_FOUND_FLOAT_VALUE);
	}
	
	public float getFloat(String argKey, float argDefault) {
		Object lclObject = getObject(argKey);
		if (lclObject == null) {
			return argDefault;
		}
		try {
			return ((Number) lclObject).floatValue();
		} catch (ClassCastException lclE) {
			return Float.parseFloat(lclObject.toString());
		}
	}
	
	public int getInt(String argKey) {
		return getInt(argKey, NOT_FOUND_INT_VALUE);
	}
	
	public int getInt(String argKey, int argDefault) {
		Object lclObject = getObject(argKey);
		if (lclObject == null) {
			return argDefault;
		}
		try {
			return ((Number) lclObject).intValue();
		} catch (ClassCastException lclE) {
			return Integer.parseInt(lclObject.toString());
		}
	}
	
	public long getLong(String argKey) {
		return getLong(argKey, NOT_FOUND_LONG_VALUE);
	}
	
	public long getLong(String argKey, long argDefault) {
		Object lclObject = getObject(argKey);
		if (lclObject == null) {
			return argDefault;
		}
		try {
			return ((Number) lclObject).longValue();
		} catch (ClassCastException lclE) {
			return Long.parseLong(lclObject.toString());
		}
	}
	
	public Object getObject(String argKey, Object argDefault) {
		Object lclObject = getObject(argKey);
		return lclObject == null ? argDefault : lclObject;
	}
	
	public short getShort(String argKey) { // NOPMD by Jonah Greenthal on 9/20/14 11:23 PM
		return getShort(argKey, NOT_FOUND_SHORT_VALUE);
	}
	
	public short getShort(String argKey, short argDefault) { // NOPMD by Jonah Greenthal on 9/20/14 11:22 PM
		Object lclObject = getObject(argKey);
		if (lclObject == null) {
			return argDefault;
		}
		try {
			return ((Number) lclObject).shortValue();
		} catch (ClassCastException lclE) {
			return Short.parseShort(lclObject.toString());
		}
	}
	
	public String getString(String argKey) {
		return getString(argKey, NOT_FOUND_STRING_VALUE);
	}
	
	public String getString(String argKey, String argDefault) {
		Object lclObject = getObject(argKey, argDefault);
		return lclObject == null ? null : lclObject.toString();
	}
	
	/* package */ static final DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
	public LocalDate getLocalDate(String argKey) {
		return getLocalDate(argKey, NOT_FOUND_LOCALDATE_VALUE);
	}
	
	public LocalDate getLocalDate(String argKey, LocalDate argDefault) {
		String lclStringifiedDate = getString(argKey);
		if (StringUtils.isBlank(lclStringifiedDate)) {
			return argDefault;
		} else {
			try {
				return LocalDate.parse(lclStringifiedDate, LOCAL_DATE_FORMAT);
			} catch (DateTimeParseException lclE) {
				ourLogger.warn("Could not parse \"" + lclStringifiedDate + "\", which is a configuration value under the key \"" + argKey + "\" as a date.");
				return argDefault;
			}
		}
	}
}
