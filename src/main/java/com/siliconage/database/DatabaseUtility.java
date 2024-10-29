package com.siliconage.database;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
//import java.sql.Types;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.util.MeasurementTally;

/**
 * Copyright &copy; 2000 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public abstract class DatabaseUtility {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(DatabaseUtility.class.getName());
	
	public static final String SEQUENCE_PREFIX = "seq_";
	public static final String SEQUENCE_SUFFIX = "_id";
	
	public static final int CLEAN_RESULT_SET = 0;
	public static final int CLEAN_STATEMENT = 1;
	public static final int CLEAN_CONNECTION = 2;
	
	public static final int NONE_ID = 1;
	public static final int UNSPECIFIED_ID = 2;
	public static final int UNKNOWN_ID = 3;
	public static final int NOT_APPLICABLE_ID = 4;
	public static final int MANUAL_ID = 5;
	public static final int INTERNAL_ID = 6;
	public static final int ERROR_ID = 7;
	
	public static final String SEQUENCE_CN = "seq";
	public static final String NAME_CN = "name";
	
	public static final MeasurementTally<String> ourTally = new MeasurementTally<>();
	
//	private static volatile int ourQueryCount;
	
	public static void tally(String argSQL, double argTime) {
		synchronized (ourTally) {
			ourTally.tally(argSQL, argTime);
		}
	}
	
	/**
	 * Builds and returns the update SQL string based on the arguments.
	 * @param argFullyQualifiedTableName The name of the table from which to delete
	 * @param argWhereClauseColumns The columns on which to filter (using parameters that will be incorporated later in a prepared statement)
	 * @return A delete string ready to be run with parameters as a prepared statement
	 */
	/* CONCERN: This method might have problems if it is used to build a SQL command intended to delete objects with NULL values for
	 * certain columns.  It will generate a string like "WHERE column_name = ?", but putting a NULL into the parameters\
	 * won't work.
	 */
	public static String buildDeleteString(String argFullyQualifiedTableName, String[] argWhereClauseColumns) {
		Validate.notNull(argFullyQualifiedTableName);
		Validate.notNull(argWhereClauseColumns);
		
		StringBuilder lclSQL = new StringBuilder(128); // THINK: How to compute?
		lclSQL.append("DELETE FROM ");
		
		lclSQL.append(argFullyQualifiedTableName);
		
		if (argWhereClauseColumns.length > 0) {
			lclSQL.append(" WHERE ");
			for (int lclI = 0; lclI < argWhereClauseColumns.length; ++lclI) {
				if (lclI != 0) {
					lclSQL.append(" AND ");
				}
				lclSQL.append(argWhereClauseColumns[lclI]);
				lclSQL.append(" = ?");
			}
		}
		
		return lclSQL.toString();
	}
	
	/**
	 * Builds and returns the insert SQL string based on the arguments.
	 * @param argI An Iterator containing the names of columns for which to insert values
	 * @param argTableName The name of the table into which to insert
	 * @return An insert string ready to be run with parameters as a prepared statement
	 */
	public static String buildInsertString(Iterator<String> argI, String argTableName) {
		Validate.notNull(argI);
		Validate.notNull(argTableName);
		
		StringBuilder lclSQL = new StringBuilder(128); // THINK: How to compute?
		lclSQL.append("INSERT INTO ");
		StringBuilder lclQ = new StringBuilder(128);
		lclSQL.append(argTableName);
		
		/* Is there at least one column name in the iterator? */
		/* FIXME: This needs different code to handle cases where there are no values to insert depending on
		 * which database is being used.  MySQL works with an empty column list, but SQL Server requires the
		 * commented-out version.  I have no good solution for the general case. */
		
		/* SQL Server Version */
		
		if (argI.hasNext()) {
			/* Yes.  We make a standard INSERT INTO X () VALUES () statement. */
			lclSQL.append(" (");
			boolean lclFirst = true;
			while (argI.hasNext()) {
				if (!lclFirst) {
					lclSQL.append(", ");
					lclQ.append(", ");
				}
				lclSQL.append(argI.next());
				lclQ.append('?');
				lclFirst = false;
			}
			lclSQL.append(") VALUES (");
			lclSQL.append(lclQ + "");
			lclSQL.append(")");
		} else {
			/* No.  We use this crazy DEFAULT VALUES option. */
			lclSQL.append(" DEFAULT VALUES");
		}
		
		return lclSQL.toString();
	}
	
	public static String buildInsertString(Iterator<String> argI, String[] argOutputColumns, String argTableName) {
		Validate.notNull(argI);
		Validate.notNull(argTableName);
		
		StringBuilder lclSQL = new StringBuilder(128); // THINK: How to compute?
		lclSQL.append("INSERT INTO ");
		StringBuilder lclQ = new StringBuilder(128);
		lclSQL.append(argTableName);
		
		/* Is there at least one column name in the iterator? */
		/* FIXME: This needs different code to handle cases where there are no values to insert depending on
		 * which database is being used.  MySQL works with an empty column list, but SQL Server requires the
		 * commented-out version. */
		
		boolean lclColumnList = argI.hasNext();
		
		if (lclColumnList) {
			/* Yes.  We make a standard INSERT INTO X (...) VALUES (...) statement. */
			lclSQL.append(" (");
			boolean lclFirst = true;
			while (argI.hasNext()) {
				if (!lclFirst) {
					lclSQL.append(", ");
					lclQ.append(", ");
				}
				lclSQL.append(argI.next());
				lclQ.append('?');
				lclFirst = false;
			}
			lclSQL.append(")");
		}
		
		/* This portion dealing with output columns is probably SQL Server-specific. */
		if (argOutputColumns.length > 0) {
			lclSQL.append(" OUTPUT ");
			for (int lclI = 0; lclI < argOutputColumns.length; ++lclI) {
				if (lclI > 0) {
					lclSQL.append (", ");
				}
				lclSQL.append("Inserted."); // This is the generic prefix representing the table into which the row was inserted.
				lclSQL.append(argOutputColumns[lclI]);
			}
		} else {
			ourLogger.warn("insertWithOutput invoked with zero-length array of OUTPUT columns.");
		}
		
		if (lclColumnList) {
			lclSQL.append(" VALUES (");
			lclSQL.append(lclQ + "");
			lclSQL.append(")");
		} else {
			/* No.  We use this crazy DEFAULT VALUES option. */
			lclSQL.append(" DEFAULT VALUES");
		}
		
		return lclSQL.toString();
	}
	
	/**
	 * Builds and returns the update SQL string based on the arguments.
	 * @param argI An Iterator&lt;String&gt; whose elements are the names of the columns to be updated (using parameters that will be incorporated later in a prepared statement)
	 * @param argFullyQualifiedTableName The name of the table to update
	 * @param argWhereClauseColumns The columns on which to filter (using parameters that will be incorporated later in a prepared statement)
	 * @return String - SQL Statement
	 */
	public static String buildUpdateString(Iterator<String> argI, String argFullyQualifiedTableName, String[] argWhereClauseColumns) {
		Validate.notNull(argI);
		Validate.isTrue(argI.hasNext()); // Assumes at least one value in the iterator
		Validate.notNull(argFullyQualifiedTableName);
		Validate.notNull(argWhereClauseColumns);
		
		StringBuilder lclSQL = new StringBuilder(128); // THINK: How to compute?
		lclSQL.append("UPDATE ");
		
		lclSQL.append(argFullyQualifiedTableName);
		
		boolean lclFirst = true;
		while (argI.hasNext()) {
			if (!lclFirst) {
				lclSQL.append(", ");
			} else {
				lclSQL.append(" SET ");
			}
			lclSQL.append(argI.next());
			lclSQL.append(" = ?");
			lclFirst = false;
		}
	
		// THINK: Can we get this out of the Iterator somehow?  Should we?
		if (argWhereClauseColumns.length > 0) {
			lclSQL.append(" WHERE ");
			for (int lclI = 0; lclI < argWhereClauseColumns.length; ++lclI) {
				if (lclI != 0) {
					lclSQL.append(" AND ");
				}
				lclSQL.append(argWhereClauseColumns[lclI]);
				lclSQL.append(" = ?");
			}
		}
		
		return lclSQL.toString();
	}
	
	/**
	 * Builds and returns the update SQL string based on the arguments.
	 * @return String - SQL Statement
	 * @param argI An Iterator&lt;String&gt; whose elements are the names of the columns to be updated (using parameters that will be incorporated later in a prepared statement)
	 * @param argFullyQualifiedTableName The name of the table to update
	 * @param argIDColumnName The name of a key column that will be used to indicate the row to update (using a parameter that will be incorporated later in a prepared statement)
	 */
	public static String buildUpdateString(Iterator<String> argI, String argFullyQualifiedTableName, String argIDColumnName) {
		Validate.notNull(argI);
		Validate.isTrue(argI.hasNext()); // Assume at least one value in the iterator
		Validate.notNull(argFullyQualifiedTableName);
		Validate.notNull(argIDColumnName);
				
		StringBuilder lclSQL = new StringBuilder(128); // THINK: How to compute?
		lclSQL.append("UPDATE ");
		
		lclSQL.append(argFullyQualifiedTableName);
		boolean lclFirst = true;
		while (argI.hasNext()) {
			if (!lclFirst) {
				lclSQL.append(", ");
			} else {
				lclSQL.append(" SET ");
			}
			lclSQL.append(argI.next());
			lclSQL.append(" = ?");
			lclFirst = false;
		}
		
		// THINK: Can we get this out of the Iterator somehow?  Should we?
		
		lclSQL.append(" WHERE ");
		lclSQL.append(argIDColumnName);
		lclSQL.append(" = ?");
		
		return lclSQL.toString();
	}
	
	public static void cleanUp(ResultSet argRS, Connection argC) {
		if (argRS != null) {
//			System.out.println("lclRS != null");
			cleanUp(argRS, CLEAN_STATEMENT);
		}
		if (argC != null) {
//			System.out.println("lclConn != null");
			closeConnection(argC);
		}
	}
	
	/**
	 * @param argRS The ResultSet that should be cleaned up, together with its Statement and Connection.  If <code>null</code>, nothing happens.
	 */
	public static void cleanUp(ResultSet argRS) {
		cleanUp(argRS, CLEAN_CONNECTION);
	}
	
	/**
	 * @param argRS The ResultSet that should be cleaned up, possibly including its Statement and Connection (depending on argCleanLevel).  If <code>null</code>, nothing happens. 
	 * @param argCleanLevel How fastidiously to clean up the ResultSet argRS
	 */
	public static void cleanUp(ResultSet argRS, int argCleanLevel) {
//		System.out.println("cleanUp argCleanLevel = " + argCleanLevel);
		if (argRS == null) {
			return;
		}
		
		try {
			/* We need to get these here in case the ResultSet is pooled and happens to
			be checked back out to another connection in between its closing and our
			acquiring the Statement. */
			
			Statement lclStatement = argRS.getStatement();
			
//			System.out.println("lclStatement = " + lclStatement);
			
			Connection lclConnection = null;
			
			if (lclStatement != null) {
				lclConnection = lclStatement.getConnection();
			}
			
//			System.out.println("lclConnection = " + lclConnection == null ? null : lclConnection.hashCode());
			
			closeResultSet(argRS);
			
			if (argCleanLevel < CLEAN_STATEMENT) {
				return;
			}
			
			if (lclStatement == null) {
				return;
			}
			
			closeStatement(lclStatement);
			
			if (argCleanLevel < CLEAN_CONNECTION) {
				return;
			}
			
			if (lclConnection == null) {
				return;
			}
			
			closeConnection(lclConnection);
			
			return;
		} catch (Exception lclE) {
			ourLogger.error("Suppressing exception thrown while cleaning up.", lclE);
		} 
	}
	
	/**
	 * Closes the argument Connection.
	 * @param argConnection The Connection to close
	 */
	public static void closeConnection(Connection argConnection) {
//		System.out.println("closeConnection; argConnection = " + argConnection == null ? null : argConnection.hashCode());
		if (argConnection != null) {
			try {
				if (!argConnection.isClosed()) {
//					System.out.println("Closing connection " + argConnection.hashCode());
					argConnection.close();
				} else {
//					System.out.println("Connection " + argConnection.hashCode() + " was already closed.");
				}
			} catch (Exception lclE) {
				ourLogger.error("Suppressing exception thrown while closing a Connection.", lclE);
			}
		}
	}
	
	/**
	 * Closes the argument ResultSet.
	 * @param argRS The ResultSet to close
	 */
	public static void closeResultSet(ResultSet argRS) {
//		System.out.println("closeResultSet; closeResultSet = " + argRS);
		if (argRS != null) {
			try {
				argRS.close();
			} catch (Exception lclE) {
				ourLogger.error("Suppressing exception thrown while closing a ResultSet.", lclE);
			}
		}
	}
	
	/**
	 * Closes the argument Statement.
	 * @param argStatement The Statement to close
	 */
	public static void closeStatement(Statement argStatement) {
//		System.out.println("closeStatement; argStatement = " + argStatement );
		if (argStatement != null) {
			try {
				argStatement.close();
			} catch (Exception lclE) {
				ourLogger.error("Suppressing exception while closing a Statement.", lclE);
			}
		}
	}
	
	/* CONCERN: This may not work if you are trying to delete rows where one of the WHERE clause values is null. */
	public static int delete(Connection argConnection, String argFullyQualifiedTablename, String[] argWhereClauseColumns, Object... argWhereClauseValues) throws SQLException {
		if (argConnection == null) {
			throw new IllegalArgumentException("argConnection is null");
		}
		if (argFullyQualifiedTablename == null) {
			throw new IllegalArgumentException("argFullyQualifiedTablename is null");
		}
		
		String lclSQL = buildDeleteString(argFullyQualifiedTablename, argWhereClauseColumns);
		
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("SQL = " + lclSQL);
			if (argWhereClauseValues != null) {
				for (int lclI = 0; lclI < argWhereClauseValues.length; ++ lclI) {
					ourLogger.debug(argWhereClauseColumns[lclI] + " = " + argWhereClauseValues[lclI]);
				}
			}
		}
		return executeDML(argConnection, lclSQL, argWhereClauseValues);
	}
	
	/**
	 * Executes the argument SQL statement.
	 * @param argConnection The connection on which to execute the SQL statement argSQL
	 * @param argSQL The DML statement
	 * @param argParameters The parameters for running argSQL as a prepared statement
	 * @return int
	 * @throws SQLException If there is a problem running the SQL statement
	 */
	/* CONCERN: Because this uses lclPS.setObject, does it work for nvarchar columns (setNString) and/or null columns? */
	public static int executeDML(Connection argConnection, String argSQL, Object... argParameters) throws SQLException {
		if (argConnection == null) {
			throw new IllegalArgumentException("argConnection is null");
		}
		if (argSQL == null) {
			throw new IllegalArgumentException("argSQL is null");
		}
		/* argParameters can be null */
		
		// FEATURE: Invert so that we get the value of the sequence first, then add that to the list of fields.
		// This may involve some weird code since the statement will have to have one more '?' than the argRSKM has
		// parameters.
		
//		ourLogger.debug("Execute DML SQL = " + argSQL);
		
		try (PreparedStatement lclPS = argConnection.prepareStatement(argSQL)) {
			if (argParameters != null) {
//				int lclCount = 0;
				for (int lclI = 0; lclI < argParameters.length; ++lclI) {
					lclPS.setObject(lclI+1, argParameters[lclI]);
//					ourLogger.debug("Parameter [" + lclI + "] = " + String.valueOf(argParameters[lclI]));
				}
			}
			
			return lclPS.executeUpdate();
		} catch (SQLException lclE) {
			ourLogger.error("Error executing DML; Statement = \"" + argSQL + "\"; Parameters = [" + StringUtils.join(argParameters, ", ") + "]");
			throw lclE;
		}
	}
	
	/**
	 * Executes the argument SQL statement.
	 * @param argConnection The connection on which to run the statement
	 * @param argSQL A SQL statement that produces a single column and a single row, containing an integer value
	 * @param argParameters The parameters for running argSQL as a prepared statement
	 * @return int The result of the statement
	 * @throws SQLException If there is a problem running the SQL statement
	 */
	public static int executeIntQuery(Connection argConnection, String argSQL, Object... argParameters) throws SQLException {
		return executeIntQuery(argConnection, argSQL, 1, argParameters);
	}
	
	/**
	 * Executes the argument SQL statement.
	 * @param argConnection The connection on which to run the statement
	 * @param argSQL A SQL statement that produces a single row, containing an integer value
	 * @param argParameters The parameters for running argSQL as a prepared statement
	 * @param argColumn The index (1-based) of the column of the result from which to extract the desired integer value
	 * @return int The result of the statement
	 * @throws SQLException If there is a problem running the SQL statement
	 */
	public static int executeIntQuery(Connection argConnection, String argSQL, int argColumn, Object... argParameters) throws SQLException {
	//	Logger.getInstance().enter("DatabaseUtility", "executeIntQuery");
		// FEATURE: add versions in which you can specify the column by name
	
		ResultSet lclRS = null;
		try {
			lclRS = select(argConnection, argSQL, argParameters);
	//		Logger.getInstance().log(Logger.Debug, "About to extractSingleInt");
			int lclInt = extractSingleInt(lclRS, argColumn);
	//		Logger.getInstance().log(Logger.Debug, "Just ran extractSingleInt");
			return lclInt;
		} finally {
			cleanUp(lclRS, CLEAN_STATEMENT);
	//		Logger.getInstance().exit("DatabaseUtility", "executeIntQuery");
		}
	}
	
	/**
	 * @param argRS The ResultSet from which to extract a single integer in the lone column
	 * @return int The integer value extracted from argRS's only column
	 * @throws SQLException If manipulating argRS causes a SQLException
	 * @throws RuntimeException If either no rows or multiple rows are returned.
	 */
	public static int extractSingleInt(ResultSet argRS) throws SQLException {
		return extractSingleInt(argRS, 1);
	}
	
	/**
	 * @return int
	 * @param argRS The ResultSet from which to extract a single integer in the column whose index (1-based) is argColumn
	 * @param argColumn The index of the column containing the desired integer value
	 * @throws SQLException If manipulating argRS causes a SQLException
	 * @throws RuntimeException If either no rows or multiple rows are returned.
	 */
	public static int extractSingleInt(ResultSet argRS, int argColumn) throws SQLException {
		if (! argRS.next()) {
			throw new RuntimeException("No rows returned when exactly one was expected.");
		}
		int lclInt = argRS.getInt(argColumn);
		if (argRS.next()) {
			throw new RuntimeException("Multiple rows returned when exactly one was expected.");
		}
		return lclInt;
	}
	
	/**
	 * @return int
	 * @param argRS The ResultSet from which to extract a single integer in the column named argColumnName
	 * @param argColumnName The name of the column containing the desired integer value
	 * @throws SQLException If manipulating argRS causes a SQLException
	 * @throws RuntimeException If either no rows or multiple rows are returned.
	 */
	public static int extractSingleInt(ResultSet argRS, String argColumnName) throws SQLException {
		return extractSingleInt(argRS, argRS.findColumn(argColumnName));
	}
	
	/**
	 * @param argConnection The Connection to use
	 * @param argSequence The sequence whose current value to find
	 * @return int The current value of the sequence argSequence
	 * @throws SQLException If there is a problem obtaining the sequence value
	 * @throws RuntimeException If either no rows or multiple rows are returned
	 */
	// This is Oracle-specific.
	public static int getCurrvalForSequence(Connection argConnection, String argSequence) throws SQLException {
		String lclSQL = "SELECT " + argSequence + ".currval FROM Dual\n";
//		Logger.getInstance().log(Logger.Debug, lclSQL);
		ResultSet lclRS = null;
		try {
			lclRS = select(argConnection, lclSQL);
			return extractSingleInt(lclRS);
		} finally {
			cleanUp(lclRS, CLEAN_STATEMENT); // Don't close the Connection!
		}
	}
	
	/**
	 * @param argDataSource The DataSource to use
	 * @param argSequence The sequence whose next value to find
	 * @return int The next value of the sequence argSequence
	 * @throws SQLException If there is a problem obtaining the next sequence value
	 * @throws RuntimeException If either no rows or multiple rows are returned
	 */
	// This is Oracle-specific.
	public static int getNextvalForSequence(DataSource argDataSource, String argSequence) throws SQLException {
		try (Connection lclConnection = argDataSource.getConnection()) {
			return getNextvalForSequence(lclConnection, argSequence);
		}
	}
	
	/**
	 * @param argConnection The Connection to use
	 * @param argSequence The sequence whose next value to find
	 * @return int The next value of the sequence argSequence
	 * @throws SQLException If there is a problem obtaining the next sequence value
	 * @throws RuntimeException If either no rows or multiple rows are returned
	 */
	// This is Oracle-specific.
	public static int getNextvalForSequence(Connection argConnection, String argSequence) throws SQLException {
		String lclSQL = "SELECT " + argSequence + ".nextval FROM Dual\n";
//		Logger.getInstance().log(Logger.Debug, lclSQL);
		ResultSet lclRS = null;
		try {
			lclRS = select(argConnection, lclSQL);
			return extractSingleInt(lclRS);
		} finally {
			cleanUp(lclRS, CLEAN_STATEMENT); // Don't close the Connection
		} 
		
	}

	/* Determines whether a String contains only ASCII (0-127) characters.  A null String is deemed to be ASCII. */
	private static boolean isASCII(String argS) {
		if (argS == null) {
			return true;
		}
		for (int lclI = 0; lclI < argS.length(); ++lclI) {
			if (argS.charAt(lclI) > 127) {
				return false;
			}
		}
		return true;
	}
	
	/* This method is responsible for setting the parameters on DML (UPDATE and INSERT) PreparedStatements.  It has
	 * a long history of having difficulties with null values.  In short, different databases and their JDBC drivers
	 * expect some sort of "typing" of null.
	 * 
	 *  The original incarnation of this code (which worked on Oracle and Sybase), would just call setObject(index, null)
	 *  for all null parameters.
	 *  
	 *  It was found that this didn't work with the jTDS driver (which NAQT used until 2024) for SQL Server.  However,
	 *  setString(argIndex, null) did work (even if the null was not being passed to a (var)char field.
	 *  
	 *  Later on, that was changed to setNull(argIndex, Types.NULL) in the belief that this was the proper way to do it.
	 *  This also worked for all fields on jTDS.
	 *  
	 *  However, it failed with the Microsoft JDBC driver (adopted in 2024).  In particular, nulls being passed to
	 *  date fields would cause errors.  Returning to setObject(argIndex, null) appears to have addressed this (but
	 *  it has not been widely tested with other types of queries or fields).
	 *  
	 *  Note that this code will no longer work with the jTDS driver.
	 *  
	 *  Solving this problem properly (by giving this code access to the "type" of null) will, in many cases, be difficult.
	 */
	private static void setParameter(PreparedStatement argPS, int argIndex, Object argV) throws SQLException {
		switch (argV) {
		case null:
//			argPS.setString(argIndex, null); // Is this still necessary with the Microsoft JDBC Driver?
//			argPS.setNull(argIndex, Types.NULL); // Is this still necessary with the Microsoft JDBC Driver?
			argPS.setObject(argIndex, null);
			break;
		case Character lclC:
			argPS.setString(argIndex, String.valueOf(lclC)); // Is this still necessary with the Microsoft JDBC Driver?
			break;
		case String lclS:
			if (isASCII(lclS)) {
				argPS.setString(argIndex, lclS);
			} else {
				try {
					argPS.setNString(argIndex, lclS);
				} catch (AbstractMethodError lclE) {
					// The jTDS driver doesn't implement setNString
					argPS.setString(argIndex, lclS);
				}
			}
			break;
		default:
			argPS.setObject(argIndex, argV);
			break;
		}
	}
		
	private static void setInsertParameters(PreparedStatement argPS, Map<String, Object> argMap) throws SQLException {
		if (argPS == null) {
			throw new IllegalArgumentException("argPS is null");
		}
		if (argMap == null) {
			throw new IllegalArgumentException("argMap is null");
		}
		
		// FEATURE: Invert so that we get the value of the sequence first, then add that to the list of fields.
		// This may involve some weird code since the statement will have to have one more '?' than the argRSKM has
		// parameters.
		
		Iterator<Map.Entry<String, Object>> lclI = argMap.entrySet().iterator();
		int lclCount = 0;
		while (lclI.hasNext()) {
			Map.Entry<String, Object> lclE = lclI.next();
			Object lclV = lclE.getValue();
			
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("Parameter[" + lclCount + "] = " + String.valueOf(lclV));
			}
			/* See http://java.sun.com/products/jdk/1.1/docs/guide/jdbc/getstart/preparedstatement.doc.html for why this treatment of NULLs is necessary. */
			
			setParameter(argPS, ++lclCount, lclV);
		}
	}

	public static void insert(Connection argConnection, String argFullyQualifiedTableName, Map<String, Object> argMap, Consumer<ResultSet> argGeneratedKeyProcessor) throws SQLException {
		if (argConnection == null) {
			throw new IllegalArgumentException("argConnection is null");
		}
		if (argFullyQualifiedTableName == null) {
			throw new IllegalArgumentException("argFullyQualifiedTableName is null");
		}
		
		if (argMap == null) {
			throw new IllegalArgumentException("argMap is null");
		}
		
		// FEATURE: Invert so that we get the value of the sequence first, then add that to the list of fields.
		// This may involve some weird code since the statement will have to have one more '?' than the argRSKM has
		// parameters.
		
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("DatabaseUtility.insert: Table == " + argFullyQualifiedTableName);
		}
		
		String lclSQL = buildInsertString(argMap.keySet().iterator(), argFullyQualifiedTableName);
		
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("DatabaseUtility.insert: SQL = " + lclSQL);
		}
		
		try (PreparedStatement lclPS = argConnection.prepareStatement(
				lclSQL,
				argGeneratedKeyProcessor != null ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS) 
			) {
			
			setInsertParameters(lclPS, argMap);
			
//			ourLogger.info("About to executeUpdate()");
			
			lclPS.executeUpdate();
			
			if (argGeneratedKeyProcessor != null) {
				try (ResultSet lclRS = lclPS.getGeneratedKeys()) {
					if (lclRS.next()) {
						argGeneratedKeyProcessor.accept(lclRS);
						if (lclRS.next()) {
							ourLogger.warn("DatabaseUtility.insertAndProcessGeneratedKeys: Asked to getGeneratedKeys but more than one row was returned in the ResultSet.");
						}
					} else {
						ourLogger.warn("DatabaseUtility.insertAndProcessGeneratedKeys: Asked to getGeneratedKeys but no rows were returned in the ResultSet.");
					}
				}
			}
		}
	}

	public static void insertWithOutput(Connection argConnection, String argFullyQualifiedTableName, Map<String, Object> argMap, String[] argOutputColumns, Consumer<ResultSet> argOutputProcessor) throws SQLException {
		if (argConnection == null) {
			throw new IllegalArgumentException("argConnection is null");
		}
		if (argFullyQualifiedTableName == null) {
			throw new IllegalArgumentException("argFullyQualifiedTableName is null");
		}
		if (argMap == null) {
			throw new IllegalArgumentException("argMap is null");
		}
		if (argOutputProcessor == null) {
			throw new IllegalArgumentException("argOutputProcessor is null");
		}
		
		// FEATURE: Invert so that we get the value of the sequence first, then add that to the list of fields.
		// This may involve some weird code since the statement will have to have one more '?' than the argRSKM has
		// parameters.
		
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("DatabaseUtility.insertWithOutput: Table == " + argFullyQualifiedTableName);
		}
		
		String lclSQL = buildInsertString(argMap.keySet().iterator(), argOutputColumns, argFullyQualifiedTableName);
		
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("SQL = " + lclSQL);
		}
		
		try (PreparedStatement lclPS = argConnection.prepareStatement(lclSQL)) {
			
			setInsertParameters(lclPS, argMap);
			
//			ourLogger.info("About to executeUpdate()");
			
			try (ResultSet lclRS = lclPS.executeQuery()) {
				if (lclRS.next()) {
					argOutputProcessor.accept(lclRS);
					if (lclRS.next()) {
						ourLogger.warn("DatabaseUtility.insertWithOutput: Asked to getGeneratedKeys but more than one row was returned in the ResultSet.");
					}
				} else {
					ourLogger.warn("DatabaseUtility.insertWithOutput: Asked to getGeneratedKeys but no rows were returned in the ResultSet.");
				}
			}
		}
	}

	/**
	 * @param argConnection The Connection to use
	 * @param argFullyQualifiedTableName The table into which to insert
	 * @param argMap A Map whose keys are the names of columns in argFullyQualifiedTableName and whose values are the values to insert in those respective columns
	 * @throws SQLException If there is a problem inserting
	 */
	public static void insert(Connection argConnection, String argFullyQualifiedTableName, Map<String, Object> argMap) throws SQLException {
		insert(argConnection, argFullyQualifiedTableName, argMap, null);
	}
	
	/**
	 * @param argConnection The Connection to use
	 * @param argTableName The table into which to insert
	 * @param argMap A Map whose keys are the names of columns in argFullyQualifiedTableName and whose values are the values to insert in those respective columns
	 * @param argIDColumnName The name of the identity column
	 * @param argSequenceName The name of the sequence
	 * @return int The sequence value for the inserted row
	 * @throws SQLException If there is a problem inserting
	 * @throws IllegalArgumentException If any of the arguments are <code>null</code> or if either of the Strings are empty
	 * @throws IllegalStateException If argConnection is closed
	 */
	// This is Oracle-specific.
	public static int insertWithSequence(Connection argConnection, String argTableName, Map<String, Object> argMap, String argIDColumnName, String argSequenceName) throws SQLException {
		if (argConnection == null) {
			throw new IllegalArgumentException("argConnection is null in DatabaseUtility.insertWithSequence().");
		}
		if (argConnection.isClosed()) {
			throw new IllegalStateException("Connection is closed in DatabaseUtility.insertWithSequence().");
		}
		if (argTableName == null || "".equals(argTableName)) {
			throw new IllegalArgumentException("argTableName is null or empty in DatabaseUtility.insertWithSequence().");
		}
		if (argMap == null) {
			throw new IllegalArgumentException("argMap is null in DatabaseUtility.insertWithSequence().");
		}
		if (argIDColumnName == null || "".equals(argIDColumnName)) {
			throw new IllegalArgumentException("argIDColumnName is null or empty in DatabaseUtility.insertWithSequence().");
		}
		
//		Logger.getInstance().enter("DatabaseUtility", "insertWithSequence");
		
		int lclID;
		
		String lclSequenceName = argSequenceName;
		if (lclSequenceName == null) {
			lclSequenceName = makeSequenceName(argIDColumnName);
		}
		
		lclID = getNextvalForSequence(argConnection, lclSequenceName);
		
		argMap.put(argIDColumnName, Integer.valueOf(lclID));
		
		String lclSQL = buildInsertString(argMap.keySet().iterator(), argTableName);
		
		try (PreparedStatement lclPS = argConnection.prepareStatement(lclSQL)) {
			Iterator<String> lclI = argMap.keySet().iterator();
			int lclCount = 0;
			while (lclI.hasNext()) {
				setParameter(lclPS, ++lclCount, argMap.get(lclI.next()));
			}
//			Logger.getInstance().log(Logger.Debug, "About to insert.");
			
			lclPS.executeUpdate();
			
//			Logger.getInstance().log(Logger.Debug, "Just inserted.  About to get ID.");
			
			return lclID;
		} catch (SQLException lclE) {
			StringBuilder lclSB = new StringBuilder();
			lclSB.append("Error executing DML; Statement = \"");
			lclSB.append(lclSQL);
			lclSB.append("\"; Parameters = [");
			boolean lclFirst = true;
			for (Map.Entry<String, Object> lclEntry : argMap.entrySet()) {
				if (lclFirst) {
					lclFirst = false;
				} else {
					lclSB.append(", ");
				}
				lclSB.append(String.valueOf(lclEntry.getValue()));
			}
			lclSB.append("]");
			ourLogger.error(lclSB.toString());
			throw lclE;
		}
	}
	
//	/**
//	 * Determines whether the argument represents a special database ID or not.
//	 * @param argID the database ID
//	 * @return <code>true</code> if the argument is strictly between 0 and 1000; <code>false</code>otherwise.
//	 */
//	public static boolean isSpecialID(int argID) {
//		return (0 < argID) && (argID < 1000);
//	}
	
	/**
	 * Creates a column name to hold the primary key of a
	 * table by appending "_id" to end of the argument String.
	 * @param argTableName The name of the table for which to create an identity column name
	 * @return The name of the identity column
	 */
	public static String makeIDColumnName(String argTableName) {
		return argTableName.toLowerCase() + "_id";
	}
	
	/**
	 * Creates a sequence name for a table by prepending the constant
	 * SEQUENCE_PREFIX to the beginning of the argument String.
	 * @param argColumnName The base column name
	 * @return The sequence name
	 */
	public static String makeSequenceName(String argColumnName) {
		return SEQUENCE_PREFIX + argColumnName;
	}
	
	/**
	 * @param argConnection The Connection on which to run argSQL
	 * @param argSQL The select statement
	 * @param argParameters The parameters for running argSQL as a prepared statement
	 * @return ResultSet The ResultSet produced by running the select statement
	 * @throws SQLException If there is a problem selecting
	 */
	public static ResultSet select(Connection argConnection, String argSQL, Object... argParameters) throws SQLException {
		
		if (argConnection == null) {
			throw new IllegalArgumentException("argConnection is null");
		}
		if (argSQL == null) {
			throw new IllegalArgumentException("argSQL is null");
		}
		
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("SQL = " + argSQL);
		}
		
		/* Note that this can't be put into the try-with-resources paradigm. */
		PreparedStatement lclPS = null;
		
		try {
			lclPS = argConnection.prepareStatement(argSQL);
			
			int lclCount = 0;
			
			if (argParameters != null) {
				while (lclCount < argParameters.length) {
					Object lclValue = argParameters[lclCount];
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug("Parameter[" + lclCount + "] = " + String.valueOf(lclValue));
					}
					setParameter(lclPS, ++lclCount, lclValue);
//					if (lclValue == null) {
//						lclPS.setString(++lclCount, null); // pre-increment because SQL parameters are 1-based
//					} else {
//						lclPS.setObject(++lclCount, lclValue); // pre-increment because SQL parameters are 1-based
//					}
				}
			}
			
			long lclStart = System.currentTimeMillis();
			ResultSet lclRS = lclPS.executeQuery();
			long lclEnd = System.currentTimeMillis();
			if (lclEnd - lclStart > 500L) { // Half a second
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Query took " + (lclEnd - lclStart) + " ms.");
				}
			}
//			ourQueryCount++;
//			if (ourQueryCount % 1000 == 0) {
//				ourLogger.debug("Query count has just hit " + ourQueryCount + ".");
//			}
			
			/* The user has no easy access to the returned ResultSet's underlying (Prepared)Statement, so they can't easily
			 * close it when they are done with the ResultSet.  This could (conceivably) lead to performance problems or
			 * memory leaks depending on how the driver handles things.
			 * 
			 * We wrap the ResultSet with a StatementClosingResultSet that will close the Statement when the ResultSet
			 * is closed.  This will prevent the user from retrieving the Statement from the ResultSet and reusing it
			 * with new parameters, but they really shouldn't be doing that.  If they need to batch statements, don't
			 * go through DatabaseUtility::select.
			 */
			
			return new StatementClosingResultSet(lclRS);
		} catch (SQLException | RuntimeException lclE) {
			closeStatement(lclPS);
			ourLogger.error("Couldn't execute SELECT query " + argSQL + " with parameters " + Arrays.toString(argParameters), lclE);
			throw lclE;
		} finally {
			// We don't want to close the statement if we exit successfully!
		}
	}
	
	public static ResultSet select(Connection argConnection, String[] argSelectColumnNames, String argFullyQualifiedTableName, String[] argWhereClauseColumns, Object... argWhereClauseValues) throws SQLException {
		StringBuilder lclSB = new StringBuilder("SELECT ");
		boolean lclFirst = true;
		for (String lclColumnName : argSelectColumnNames) {
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(", ");
			}
			lclSB.append(lclColumnName);
		}
		return select(argConnection, lclSB.toString(), argFullyQualifiedTableName, argWhereClauseColumns, argWhereClauseValues);
	}
	
	public static ResultSet select(Connection argConnection, String argFullyQualifiedTableName, String[] argWhereClauseColumns, Object... argWhereClauseValues) throws SQLException {
		return select(argConnection, "SELECT *", argFullyQualifiedTableName, argWhereClauseColumns, argWhereClauseValues);
	}
	
	public static ResultSet select(Connection argConnection, String argSelectClause, String argFullyQualifiedTableName, String[] argWhereClauseColumns, Object... argWhereClauseValues) throws SQLException {
		if (argFullyQualifiedTableName == null) {
			throw new IllegalArgumentException("argFullyQualifiedTableName is null");
		}
		
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append(argSelectClause);
		lclSB.append(" FROM ");
		
		lclSB.append(argFullyQualifiedTableName);
		
		if (argWhereClauseColumns == null) {
			return select(argConnection, lclSB.toString());
		} else {
			int lclLength = argWhereClauseColumns.length;
			
			for(int lclI = 0; lclI < lclLength; ++lclI) {
				if (lclI == 0) {
					lclSB.append(" WHERE ");
				} else {
					lclSB.append(" AND ");
				}
				lclSB.append(argWhereClauseColumns[lclI]);
				lclSB.append(" = ?");
			}
			
			return select(argConnection, lclSB.toString(), argWhereClauseValues);
		}
	}
	
	public static <T> List<T> select(DataSource argDS, java.util.function.Function<ResultSet, ? extends T> argF, String argSQL, Object... argParameters) throws SQLException {
		List<T> lclTs = null;
		try (Connection lclC = argDS.getConnection()) {
			ResultSet lclRS = null;
			try {
				lclRS = select(lclC, argSQL, argParameters);
				while (lclRS.next()) {
					T lclT = argF.apply(lclRS);
					if (lclT != null) {
						if (lclTs == null) {
							lclTs = new java.util.ArrayList<>();
						}
						lclTs.add(lclT);
					}
				}
			} finally {
				if (lclRS != null) {
					cleanUp(lclRS, CLEAN_STATEMENT);
				}
			}
		}
		if (lclTs == null) {
			return java.util.Collections.emptyList();
		} else {
			return lclTs;
		}
	}
	
	public static int update(Connection argConnection, String argTableName, Map<String, Object> argMap, String[] argWhereClauseColumns, Object... argWhereClauseValues) throws SQLException {
		if (argConnection == null) {
			throw new IllegalArgumentException("argConnection is null");
		}
		if (argTableName == null) {
			throw new IllegalArgumentException("argTableName is null");
		}
		if (argMap == null) {
			throw new IllegalArgumentException("argRSKM is null");
		}
		if (argWhereClauseColumns == null) {
			throw new IllegalArgumentException("argWhereClauseColumns is null");
		}
		if (argWhereClauseValues == null) {
			throw new IllegalArgumentException("argWhereClauseValues is null");
		}

		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("DatabaseUtility.update.  Table == " + argTableName + " argWhereClauseValues.length == " + argWhereClauseValues.length);
		}
		
		if (argMap.size() == 0) {
			throw new IllegalArgumentException("argRSKM has zero elements in DatabaseUtility.update(...).");
		}
		
		String lclSQL = buildUpdateString(argMap.keySet().iterator(), argTableName, argWhereClauseColumns);
	
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("SQL = " + lclSQL);
		}
		
//		Logger.getInstance().log(Logger.Debug, lclSQL);
		
		try (PreparedStatement lclPS = argConnection.prepareStatement(lclSQL)) {
			
			Iterator<Map.Entry<String, Object>> lclI = argMap.entrySet().iterator();
			int lclCount = 0;
			while (lclI.hasNext()) {
				Object lclV = lclI.next().getValue();
				
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Parameter[" + lclCount + "] = " + String.valueOf(lclV));
				}
				
				setParameter(lclPS, ++lclCount, lclV);
//				if (lclV == null) {
//					lclPS.setNull(++lclCount, Types.NULL);
//				} else {
//					
//					/* Microsoft's JDBC driver (and maybe others) don't allow Characters to be passed; they throw
//					 * exceptions.  I don't know the most efficient way to deal with this, but converting it into
//					 * a String solves the problem -- RRH (2003 September 23) */
//					 
//					if (lclV.getClass() == java.lang.Character.class) {
//						lclPS.setObject(++lclCount, String.valueOf(lclV));
//					} else {
//						lclPS.setObject(++lclCount, lclV);
//					}
//				}
			}
			
			for (int lclJ = 0; lclJ < argWhereClauseValues.length; ++lclJ) {
				Object lclV = argWhereClauseValues[lclJ];
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("WhereParameter[" + lclCount + "] = " + String.valueOf(lclV));
				}
				setParameter(lclPS, ++lclCount, lclV);
//				if (lclV == null) {
//					lclPS.setNull(++lclCount, Types.NULL);
//				} else {
//					if (lclV.getClass() == java.lang.Character.class) {
//						lclPS.setObject(++lclCount, String.valueOf(lclV));
//					} else {
//						lclPS.setObject(++lclCount, lclV);
//					}
//				}
			}
			
//			ourLogger.info("lclPS.isClosed() == " + lclPS.isClosed());
//			ourLogger.info("lclPS.getConnection().isClosed() == " + lclPS.getConnection().isClosed());
//			ourLogger.info("About to execute update.");
			
			int lclResult = lclPS.executeUpdate();
			
//			ourLogger.debug("Just finished executeUpdate(); rows affected = " + lclResult + ".");
//			
//			ourLogger.info("lclPS.isClosed() == " + lclPS.isClosed());
//			ourLogger.info("lclPS.getConnection().isClosed() == " + lclPS.getConnection().isClosed());
			
			return lclResult;
		} catch (SQLException lclE) {
			StringBuilder lclSB = new StringBuilder();
			lclSB.append("Error executing DML; Statement = \"");
			lclSB.append(lclSQL);
			lclSB.append("\"; Parameters = [");
			boolean lclFirst = true;
			for (Map.Entry<String, Object> lclEntry : argMap.entrySet()) {
				if (lclFirst) {
					lclFirst = false;
				} else {
					lclSB.append(", ");
				}
				lclSB.append(String.valueOf(lclEntry.getValue()));
			}
			lclSB.append("]");
			ourLogger.error(lclSB.toString(), lclE);
			throw lclE;
		} finally {
//			ourLogger.info("In finally clause.");
		}
	}
}
