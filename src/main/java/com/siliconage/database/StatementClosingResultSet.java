package com.siliconage.database;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.net.URL;
import javax.sql.DataSource;

/**
 * Copyright &copy; 2024 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */

/* This class wraps a (non-null) ResultSet.  It delegates all method invocations to the wrapped set.  In addition,
 * it closes its own Statement after it is closed itself.
 * 
 * This class is intended to be used with methods on DatabaseUtility that "hide" their PreparedStatement from the user.
 * As it is, the Statement is never closed.  DatabaseUtility can't do it (because it returns a ResultSet and has no way
 * of determining when it is no longer needed) and the user code can't easily do it because it has no access to the
 * Statement object.  The user code could explicitly close its resultSet.getStatement(), but it couldn't do that with
 * try-with-resources and that would be inelegant and counter to the convenience that DatabaseUtility is supposed to
 * provide.
 * 
 * Since the user has no "easy" access to the underlying PreparedStatement, they should never be tempted to reuse it
 * (with new parameters).  They could try to do this by calling resultSet.getStatement(), but they really shouldn't.
 * And they'll get a "This is closed" exception if they try to do so.
 */
@SuppressWarnings("resource")
/* package */ class StatementClosingResultSet implements ResultSet {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(StatementClosingResultSet.class.getName());

	private final ResultSet myResultSet;
	
	/**
	 * @param argRS The ResultSet
	 */
	public StatementClosingResultSet(ResultSet argRS) {
		super();
		if (argRS == null) {
			throw new IllegalStateException("argRS is null.");
		}
		myResultSet = argRS;
	}

	/**
	 * Calls <code>absolute()</code> on the internal ResultSet.
	 * @param row The row to move the cursor to
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean absolute(int row) throws SQLException {
		return getResultSet().absolute(row);
	}
	
	/**
	 * Calls <code>afterLast()</code> on the internal ResultSet.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void afterLast() throws SQLException {
		getResultSet().afterLast();
	}
	
	/**
	 * Calls <code>beforeFirst()</code> on the internal ResultSet.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void beforeFirst() throws SQLException {
		getResultSet().beforeFirst();
	}
	
	/**
	 * Cancels row updates on the internal ResultSet.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void cancelRowUpdates() throws SQLException {
		getResultSet().cancelRowUpdates();
	}
	
	/**
	 * Clears warnings on the internal ResultSet.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void clearWarnings() throws SQLException {
		getResultSet().clearWarnings();
	}
	
	/**
	 * Closes the internal ResultSet and returns this to the Database Reuser.
	 */
	@Override
	public void close() {
		// Does not automatically check the Connection back in
		ResultSet lclRS = getResultSet();
		try {
			Statement lclS = lclRS.getStatement();
			DatabaseUtility.closeResultSet(lclRS);
			DatabaseUtility.closeStatement(lclS);
		} catch (SQLException lclE) {
			ourLogger.error("Could not execute ResultSet::getStatement on the wrapped ResultSet.");
		}
	}
	
	/**
	 * Deletes a row from the internal ResultSet.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void deleteRow() throws SQLException {
		getResultSet().deleteRow();
	}
	
	/**
	 * Finds the column in the internal ResultSet whose name matches that
	 * of the argument.
	 * @param columnName The name of the column to search for
	 * @return The index of the indicated column
	 * @throws SQLException if there is a problem
	 */
	@Override
	public int findColumn(String columnName) throws SQLException {
		return getResultSet().findColumn(columnName);
	}
	
	/**
	 * Calls first() on the internal ResultSet.
	 * @return <code>true</code> if the cursor is on a valid row; <code>false</code> if there are no rows in the result set
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean first() throws SQLException {
		return getResultSet().first();
	}
	
	/**
	 * @param i The index of the column from which to fetch an Array
	 * @return An Array from the internal ResultSet
	 * @throws SQLException if there is a problem
	 */
	@Override
	public final Array getArray(int i) throws SQLException {
		return getResultSet().getArray(i);
	}
	
	/**
	 * @param colName The name of the column from which to fetch an Array
	 * @return An Array from the internal ResultSet
	 * @throws SQLException if there is a problem
	 */
	@Override
	public final Array getArray(String colName) throws SQLException {
		return getResultSet().getArray(colName);
	}
	/**
	 * @param columnIndex The index of the column from which to fetch an ASCII stream
	 * @return An InputStream from the internal ResultSet
	 * @throws SQLException if there is a problem
	 */
	
	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return getResultSet().getAsciiStream(columnIndex);
	}
	
	/**
	 * @param columnName The name of the column from which to fetch an ASCII stream
	 * @return An InputStream from the internal ResultSet
	 * @throws SQLException if there is a problem
	 */
	@Override
	public InputStream getAsciiStream(String columnName) throws SQLException {
		return getResultSet().getAsciiStream(columnName);
	}
	
	/**
	 * @param columnIndex The index of the column from which to fetch a BigDecimal
	 * @return A BigDecimal from the internal ResultSet
	 * @throws SQLException if there is a problem
	 */
	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		return getResultSet().getBigDecimal(columnIndex);
	}
	
	/**
	 * @return BigDecimal
	 * @param columnIndex int
	 * @param scale int
	 * @throws UnsupportedOperationException always (method is deprecated)
	 * @deprecated
	 */
	@Deprecated
	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale) {
		throw new UnsupportedOperationException("Called a deprecated method.");
	}
	
	/**
	 * @param columnName The name of the column from which to fetch a BigDecimal
	 * @return A BigDecimal from the internal ResultSet
	 * @throws SQLException if there is a problem
	 */
	
	@Override
	public BigDecimal getBigDecimal(String columnName) throws SQLException {
		return getResultSet().getBigDecimal(columnName);
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return BigDecimal
	 * @param columnName String
	 * @param scale int
	 * @throws UnsupportedOperationException always (method is deprecated)
	 * @deprecated
	 */
	@Deprecated
	@Override
	public BigDecimal getBigDecimal(String columnName, int scale) {
		throw new UnsupportedOperationException("Called a deprecated method.");
	}
	
	/**
	 * Returns the binary stream from the internal ResultSet.
	 * @return InputStream
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return getResultSet().getBinaryStream(columnIndex);
	}
	
	/**
	 * Returns the binary stream from the internal ResultSet.
	 * @return InputStream
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public InputStream getBinaryStream(String columnName) throws SQLException {
		return getResultSet().getBinaryStream(columnName);
	}
	
	/**
	 * Returns the Blob from the internal ResultSet.
	 * @return Blob
	 * @param i int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Blob getBlob(int i) throws SQLException {
		return getResultSet().getBlob(i);
	}
	
	/**
	 * Returns the Blob from the internal ResultSet.
	 * @return Blob
	 * @param colName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Blob getBlob(String colName) throws SQLException {
		return getResultSet().getBlob(colName);
	}
	
	/**
	 * Returns the boolean from the internal ResultSet.
	 * @return boolean
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		return getResultSet().getBoolean(columnIndex);
	}
	
	/**
	 * Returns the boolean from the internal ResultSet.
	 * @return boolean
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean getBoolean(String columnName) throws SQLException {
		return getResultSet().getBoolean(columnName);
	}
	
	/**
	 * Returns the byte from the internal ResultSet.
	 * @return byte
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public byte getByte(int columnIndex) throws SQLException {
		return getResultSet().getByte(columnIndex);
	}
	
	/**
	 * Returns the byte from the internal ResultSet.
	 * @return byte
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public byte getByte(String columnName) throws SQLException {
		return getResultSet().getByte(columnName);
	}
	
	/**
	 * Returns the byte array from the internal ResultSet.
	 * @return byte[]
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		return getResultSet().getBytes(columnIndex);
	}
	
	/**
	 * Returns the byte array from the internal ResultSet.
	 * @return byte[]
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public byte[] getBytes(String columnName) throws SQLException {
		return getResultSet().getBytes(columnName);
	}
	
	/**
	 * Returns the character stream from the internal ResultSet.
	 * @return Reader
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return getResultSet().getCharacterStream(columnIndex);
	}
	
	/**
	 * Returns the character stream from the internal ResultSet.
	 * @return Reader
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Reader getCharacterStream(String columnName) throws SQLException {
		return getResultSet().getCharacterStream(columnName);
	}
	
	/**
	 * Returns the Clob from the internal ResultSet.
	 * @return Clob
	 * @param i int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Clob getClob(int i) throws SQLException {
		return getResultSet().getClob(i);
	}
	
	/**
	 * Returns the Clob from the internal ResultSet.
	 * @return Clob
	 * @param colName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Clob getClob(String colName) throws SQLException {
		return getResultSet().getClob(colName);
	}
	
	/**
	 * Call getConcurreny() on the internal ResultSet.
	 * @return int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public int getConcurrency() throws SQLException {
		return getResultSet().getConcurrency();
	}
	
	/**
	 * @return ConnectionPool
	 * @throws SQLException if there is a problem
	 */
	public final DataSource getDataSource() throws SQLException {
		return ((PoolConnection) getStatement().getConnection()).getDataSource();
	}
	
	/**
	 * Returns the cursor name from the internal ResultSet.
	 * @return String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public String getCursorName() throws SQLException {
		return getResultSet().getCursorName();
	}
	
	/**
	 * Returns the Date from the internal ResultSet.
	 * @return Date
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Date getDate(int columnIndex) throws SQLException {
		return getResultSet().getDate(columnIndex);
	}
	
	/**
	 * Returns the Date from the internal ResultSet.
	 * @return Date
	 * @param columnIndex int
	 * @param cal Calendar
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return getResultSet().getDate(columnIndex, cal);
	}
	
	/**
	 * Returns the Date from the internal ResultSet.
	 * @return Date
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Date getDate(String columnName) throws SQLException {
		return getResultSet().getDate(columnName);
	}
	
	/**
	 * Returns the Date from the internal ResultSet.
	 * @return Date
	 * @param columnName String
	 * @param cal Calendar
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Date getDate(String columnName, Calendar cal) throws SQLException {
		return getResultSet().getDate(columnName, cal);
	}
	
	/**
	 * Returns the double from the internal ResultSet.
	 * @return double
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public double getDouble(int columnIndex) throws SQLException {
		return getResultSet().getDouble(columnIndex);
	}
	
	/**
	 * Returns the double from the internal ResultSet.
	 * @return double
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public double getDouble(String columnName) throws SQLException {
		return getResultSet().getDouble(columnName);
	}
	
	/**
	 * Returns the fetch direction of the internal ResultSet.
	 * @return int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public int getFetchDirection() throws SQLException {
		return getResultSet().getFetchDirection();
	}
	
	/**
	 * Returns the fetch size of the internal ResultSet.
	 * @return int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public int getFetchSize() throws SQLException {
		return getResultSet().getFetchSize();
	}
	
	/**
	 * Returns the float from the internal ResultSet.
	 * @return float
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public float getFloat(int columnIndex) throws SQLException {
		return getResultSet().getFloat(columnIndex);
	}
	
	/**
	 * Returns the float from the internal ResultSet.
	 * @return float
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public float getFloat(String columnName) throws SQLException {
		return getResultSet().getFloat(columnName);
	}
	
	/**
	 * Returns the int from the internal ResultSet.
	 * @return int
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public int getInt(int columnIndex) throws SQLException {
		return getResultSet().getInt(columnIndex);
	}
	
	/**
	 * Returns the int from the internal ResultSet.
	 * @return int
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public int getInt(String columnName) throws SQLException {
		return getResultSet().getInt(columnName);
	}
	
	/**
	 * Returns the long from the internal ResultSet.
	 * @return long
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public long getLong(int columnIndex) throws SQLException {
		return getResultSet().getLong(columnIndex);
	}
	
	/**
	 * Returns the long from the internal ResultSet.
	 * @return long
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public long getLong(String columnName) throws SQLException {
		return getResultSet().getLong(columnName);
	}
	
	/**
	 * Returns the meta data from the internal ResultSet.
	 * @return ResultSetMetaData
	 * @throws SQLException if there is a problem
	 */
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return getResultSet().getMetaData();
	}
	
	/**
	 * Returns the Object from the internal ResultSet.
	 * @return Object
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Object getObject(int columnIndex) throws SQLException {
		return getResultSet().getObject(columnIndex);
	}
	
	/**
	 * Returns the Object from the internal ResultSet.
	 * @return Object
	 * @param i int
	 * @param map Map
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
		return getResultSet().getObject(i, map);
	}
	
	/**
	 * Returns the Object from the internal ResultSet.
	 * @return Object
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Object getObject(String columnName) throws SQLException {
		return getResultSet().getObject(columnName);
	}
	
	/**
	 * Returns the Object from the internal ResultSet.
	 * @return Object
	 * @param colName String
	 * @param map Map
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Object getObject(String colName, Map<String, Class<?>> map) throws SQLException {
		return getResultSet().getObject(colName, map);
	}
	
	/**
	 * Returns the Ref from the internal ResultSet.
	 * @return Ref
	 * @param i int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Ref getRef(int i) throws SQLException {
		return getResultSet().getRef(i);
	}
	
	/**
	 * Returns the Ref from the internal ResultSet.
	 * @return Ref
	 * @param colName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Ref getRef(String colName) throws SQLException {
		return getResultSet().getRef(colName);
	}
	
	/**
	 * Returns the internal ResultSet.
	 * @return ResultSet
	 */
	protected final ResultSet getResultSet() {
		return myResultSet;
	}
	
	/**
	 * Returns a row from the internal ResultSet.
	 * @return int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public int getRow() throws SQLException {
		return getResultSet().getRow();
	}
	
	/**
	 * Returns the short from the internal ResultSet.
	 * @return short
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public short getShort(int columnIndex) throws SQLException {
		return getResultSet().getShort(columnIndex);
	} // NOPMD by Jonah Greenthal on 9/20/14 11:23 PM
	
	/**
	 * Returns the short from the internal ResultSet.
	 * @return short
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public short getShort(String columnName) throws SQLException {
		return getResultSet().getShort(columnName);
	} // NOPMD by Jonah Greenthal on 9/20/14 11:19 PM
	
	/**
	 * Returns the internal Statement.
	 * @return Statement
	 */
	@Override
	public Statement getStatement() throws SQLException {
		return getResultSet().getStatement();
	}
	
	/**
	 * Returns the String from the internal ResultSet.
	 * @return String
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public String getString(int columnIndex) throws SQLException {
		return getResultSet().getString(columnIndex);
	}
	
	/**
	 * Returns the String from the internal ResultSet.
	 * @return String
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public String getString(String columnName) throws SQLException {
		return getResultSet().getString(columnName);
	}
	
	/**
	 * Returns the Time from the internal ResultSet.
	 * @return Time
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Time getTime(int columnIndex) throws SQLException {
		return getResultSet().getTime(columnIndex);
	}
	
	/**
	 * Returns the Time from the internal ResultSet.
	 * @return Time
	 * @param columnIndex int
	 * @param cal Calendar
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return getResultSet().getTime(columnIndex, cal);
	}
	
	/**
	 * Returns the Time from the internal ResultSet.
	 * @return Time
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Time getTime(String columnName) throws SQLException {
		return getResultSet().getTime(columnName);
	}
	
	/**
	 * Returns the Time from the internal ResultSet.
	 * @return Time
	 * @param columnName String
	 * @param cal Calendar
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Time getTime(String columnName, Calendar cal) throws SQLException {
		return getResultSet().getTime(columnName, cal);
	}
	
	/**
	 * Returns the Timestamp from the internal ResultSet.
	 * @return Timestamp
	 * @param columnIndex int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return getResultSet().getTimestamp(columnIndex);
	}
	
	/**
	 * Returns the Timestamp from the internal ResultSet.
	 * @return Timestamp
	 * @param columnIndex int
	 * @param cal Calendar
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		return getResultSet().getTimestamp(columnIndex, cal);
	}
	
	/**
	 * Returns the Timestamp from the internal ResultSet.
	 * @return Timestamp
	 * @param columnName String
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Timestamp getTimestamp(String columnName) throws SQLException {
		return getResultSet().getTimestamp(columnName);
	}
	
	/**
	 * Returns the Timestamp from the internal ResultSet.
	 * @return Timestamp
	 * @param columnName String
	 * @param cal Calendar
	 * @throws SQLException if there is a problem
	 */
	@Override
	public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
		return getResultSet().getTimestamp(columnName, cal);
	}
	
	/**
	 * Returns the type of the internal ResultSet.
	 * @return int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public int getType() throws SQLException {
		return getResultSet().getType();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return InputStream
	 * @param columnIndex int
	 * @throws UnsupportedOperationException always (method is deprecated)
	 * @deprecated
	 */
	@Deprecated	@Override
	public InputStream getUnicodeStream(int columnIndex) {
		throw new UnsupportedOperationException("Called a deprecated method.");
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @return InputStream
	 * @param columnName String
	 * @throws UnsupportedOperationException always (method is deprecated)
	 * @deprecated
	 */
	@Deprecated	@Override
	public InputStream getUnicodeStream(String columnName) {
		throw new UnsupportedOperationException("Called a deprecated method.");
	}
	
	/**
	 * Returns the warnings from the internal ResultSet.
	 * @return SQLWarning
	 * @throws SQLException if there is a problem
	 */
	@Override
	public SQLWarning getWarnings() throws SQLException {
		return getResultSet().getWarnings();
	}
	
	/**
	 * Inserts a row into the internal ResultSet.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void insertRow() throws SQLException {
		getResultSet().insertRow();
	}
	
	/**
	 * Calls isAfterLast() on the internal ResultSet.
	 * @return boolean
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean isAfterLast() throws SQLException {
		return getResultSet().isAfterLast();
	}
	
	/**
	 * Calls isBeforeFirst() on the internal ResultSet.
	 * @return boolean
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean isBeforeFirst() throws SQLException {
		return getResultSet().isBeforeFirst();
	}
	
	/**
	 * Calls isFirst() on the internal ResultSet.
	 * @return boolean
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean isFirst() throws SQLException {
		return getResultSet().isFirst();
	}
	
	/**
	 * Calls isLast() on the internal ResultSet.
	 * @return boolean
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean isLast() throws SQLException {
		return getResultSet().isLast();
	}
	
	/**
	 * Calls last() on the internal ResultSet.
	 * @return boolean
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean last() throws SQLException {
		return getResultSet().last();
	}
	
	/**
	 * Moves the internal ResultSet to the current row.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void moveToCurrentRow() throws SQLException {
		getResultSet().moveToCurrentRow();
	}
	
	/**
	 * Moves the internal ResultSet to the insert row.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void moveToInsertRow() throws SQLException {
		getResultSet().moveToInsertRow();
	}
	
	/**
	 * Calls next() on the internal ResultSet.
	 * @return boolean
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean next() throws SQLException {
		return getResultSet().next();
	}
	
	/**
	 * Calls previous() on the internal ResultSet.
	 * @return boolean
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean previous() throws SQLException {
		return getResultSet().previous();
	}
	
	/**
	 * Refreshes a row in the internal ResultSet.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void refreshRow() throws SQLException {
		getResultSet().refreshRow();
	}
	
	/**
	 * @return boolean
	 * @param rows int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean relative(int rows) throws SQLException {
		return getResultSet().relative(rows);
	}
	
	/**
	 * Returns whether a row has been deleted or not.
	 * @return boolean - <code>true</code> if a row has been deleted;
	 * <code>false</code> otherwise.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean rowDeleted() throws SQLException {
		return getResultSet().rowDeleted();
	}
	
	/**
	 * Returns whether a row has been inserted or not.
	 * @return boolean - <code>true</code> if a row has been inserted;
	 * <code>false</code> otherwise.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean rowInserted() throws SQLException {
		return getResultSet().rowInserted();
	}
	
	/**
	 * Returns whether a row has been updated or not.
	 * @return boolean - <code>true</code> if a row has been updated;
	 * <code>false</code> otherwise.
	 * @throws SQLException if there is a problem
	 */
	@Override
	public boolean rowUpdated() throws SQLException {
		return getResultSet().rowUpdated();
	}
	
	/**
	 * Sets the fetch direction of the internal ResultSet.
	 * @param direction int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void setFetchDirection(int direction) throws SQLException {
		getResultSet().setFetchDirection(direction);
	}
	
	/**
	 * Sets the fetch size of the internal ResultSet.
	 * @param rows int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void setFetchSize(int rows) throws SQLException {
		getResultSet().setFetchSize(rows);
	}
	
//	/**
//	 * Sets the internal ResultSet.
//	 * @param argResultSet ResultSet
//	 */
//	final void setResultSet(ResultSet argResultSet) {
//		myResultSet = argResultSet;
//	}
//	
//	/**
//	 * Sets the internal Statement.
//	 * @param argStatement Statement
//	 */
//	void setStatement(Statement argStatement) {
//		myStatement = argStatement;
//	}
	
	/**
	 * Updates the ASCII stream of the internal ResultSet.
	 * @param columnIndex int
	 * @param x InputStream
	 * @param length int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		getResultSet().updateAsciiStream(columnIndex, x,length);
	}
	
	/**
	 * Updates the ASCII stream of the internal ResultSet.
	 * @param columnName String
	 * @param x InputStream
	 * @param length int
	 * @throws SQLException if there is a problem
	 */
	@Override
	public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException {
		getResultSet().updateAsciiStream(columnName, x, length);
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x BigDecimal
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x BigDecimal
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateBigDecimal(String columnName, BigDecimal x) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x InputStream
	 * @param length int
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x InputStream
	 * @param length int
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateBinaryStream(String columnName, InputStream x, int length) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x boolean
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateBoolean(int columnIndex, boolean x) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x boolean
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateBoolean(String columnName, boolean x) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x byte
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateByte(int columnIndex, byte x) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x byte
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateByte(String columnName, byte x) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x byte[]
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateBytes(int columnIndex, byte[] x) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x byte[]
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateBytes(String columnName, byte[] x) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param argColumnIndex int
	 * @param argReader Reader
	 * @param argLength int
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateCharacterStream(int argColumnIndex, Reader argReader, int argLength) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param reader Reader
	 * @param length int
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateCharacterStream(String columnName, Reader reader, int length) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x Date
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateDate(int columnIndex, Date x) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x Date
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateDate(String columnName, Date x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x double
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateDouble(int columnIndex, double x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x double
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateDouble(String columnName, double x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x float
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateFloat(int columnIndex, float x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x float
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateFloat(String columnName, float x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x int
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateInt(int columnIndex, int x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x int
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateInt(String columnName, int x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x long
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateLong(int columnIndex, long x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x long
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateLong(String columnName, long x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateNull(int columnIndex) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateNull(String columnName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x Object
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateObject(int columnIndex, Object x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x Object
	 * @param scale int
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateObject(int columnIndex, Object x, int scale) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x Object
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateObject(String columnName, Object x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x Object
	 * @param scale int
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateObject(String columnName, Object x, int scale) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateRow() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x short
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateShort(int columnIndex, short x) { // NOPMD by Jonah Greenthal on 9/20/14 11:23 PM
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x short
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateShort(String columnName, short x) { // NOPMD by Jonah Greenthal on 9/20/14 11:23 PM
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x String
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateString(int columnIndex, String x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x String
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateString(String columnName, String x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x Time
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateTime(int columnIndex, Time x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x Time
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateTime(String columnName, Time x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnIndex int
	 * @param x Timestamp
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Always throws UnsupportedOperationException.
	 * @param columnName String
	 * @param x Timestamp
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void updateTimestamp(String columnName, Timestamp x) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns whether the internal ResultSet was <code>null</code>
	 * @return boolean - <code>true</code> if the internal ResultSet was 
	 * <code>null</code>; <code>false</code> otherwise.
	 */
	@Override
	public boolean wasNull() throws SQLException {
		return getResultSet().wasNull();
	}

	@Override
	public URL getURL(int argInt) throws SQLException {
		return getResultSet().getURL(argInt);
	}

	@Override
	public URL getURL(String argString) throws SQLException {
		return getResultSet().getURL(argString);
	}

	@Override
	public void updateArray(int argInt, Array argArray) throws SQLException {
		getResultSet().updateArray(argInt, argArray);
	}

	@Override
	public void updateArray(String argString, Array argArray) throws SQLException {
		getResultSet().updateArray(argString, argArray);
	}

	@Override
	public void updateBlob(int argInt, Blob argBlob) throws SQLException {
		getResultSet().updateBlob(argInt, argBlob);
	}

	@Override
	public void updateBlob(String argString, Blob argBlob) throws SQLException {
		getResultSet().updateBlob(argString, argBlob);
	}

	@Override
	public void updateClob(int argInt, Clob argClob) throws SQLException {
		getResultSet().updateClob(argInt, argClob);
	}

	@Override
	public void updateClob(String argString, Clob argClob) throws SQLException {
		getResultSet().updateClob(argString, argClob);
	}

	@Override

	public void updateRef(int argInt, Ref argRef) throws SQLException {
		getResultSet().updateRef(argInt, argRef);
	}
	
	@Override
	public void updateRef(String argString, Ref argRef) throws SQLException {
		getResultSet().updateRef(argString, argRef);
	}

	@Override
	public int getHoldability() throws SQLException {
		return getResultSet().getHoldability();
	}

	@Override
	public Reader getNCharacterStream(int argColumnIndex) throws SQLException {
		return getResultSet().getNCharacterStream(argColumnIndex);
	}

	@Override
	public Reader getNCharacterStream(String argColumnLabel) throws SQLException {
		return getResultSet().getNCharacterStream(argColumnLabel);
	}

	@Override
	public NClob getNClob(int argColumnIndex) throws SQLException {
		return getResultSet().getNClob(argColumnIndex);
	}

	@Override
	public NClob getNClob(String argColumnLabel) throws SQLException {
		return getResultSet().getNClob(argColumnLabel);
	}

	@Override
	public String getNString(int argColumnIndex) throws SQLException {
		return getResultSet().getNString(argColumnIndex);
	}

	@Override
	public String getNString(String argColumnLabel) throws SQLException {
		return getResultSet().getNString(argColumnLabel);
	}

	@Override
	public RowId getRowId(int argColumnIndex) throws SQLException {
		return getResultSet().getRowId(argColumnIndex);
	}

	@Override
	public RowId getRowId(String argColumnLabel) throws SQLException {
		return getResultSet().getRowId(argColumnLabel);
	}

	@Override
	public SQLXML getSQLXML(int argColumnIndex) throws SQLException {
		return getResultSet().getSQLXML(argColumnIndex);
	}

	@Override
	public SQLXML getSQLXML(String argColumnLabel) throws SQLException {
		return getResultSet().getSQLXML(argColumnLabel);
	}

	@Override
	public boolean isClosed() throws SQLException {
		return getResultSet().isClosed();
	}

	@Override
	public void updateAsciiStream(int argColumnIndex, InputStream argX) throws SQLException {
		getResultSet().updateAsciiStream(argColumnIndex, argX);
	}

	@Override
	public void updateAsciiStream(String argColumnLabel, InputStream argX) throws SQLException {
		getResultSet().updateAsciiStream(argColumnLabel, argX);
	}

	@Override
	public void updateAsciiStream(int argColumnIndex, InputStream argX, long argLength) throws SQLException {
		getResultSet().updateAsciiStream(argColumnIndex, argX, argLength);
	}

	@Override
	public void updateAsciiStream(String argColumnLabel, InputStream argX, long argLength) throws SQLException {
		getResultSet().updateAsciiStream(argColumnLabel, argX, argLength);
	}

	@Override
	public void updateBinaryStream(int argColumnIndex, InputStream argX) throws SQLException {
		getResultSet().updateBinaryStream(argColumnIndex, argX);
	}

	@Override
	public void updateBinaryStream(String argColumnLabel, InputStream argX) throws SQLException {
		getResultSet().updateBinaryStream(argColumnLabel, argX);
	}

	@Override
	public void updateBinaryStream(int argColumnIndex, InputStream argX, long argLength) throws SQLException {
		getResultSet().updateBinaryStream(argColumnIndex, argX, argLength);
	}

	@Override
	public void updateBinaryStream(String argColumnLabel, InputStream argX, long argLength) throws SQLException {
		getResultSet().updateBinaryStream(argColumnLabel, argX, argLength);
	}

	@Override
	public void updateBlob(int argColumnIndex, InputStream argInputStream) throws SQLException {
		getResultSet().updateBlob(argColumnIndex, argInputStream);
	}

	@Override
	public void updateBlob(String argColumnLabel, InputStream argInputStream) throws SQLException {
		getResultSet().updateBlob(argColumnLabel, argInputStream);
	}
	
	@Override
	public void updateBlob(int argColumnIndex, InputStream argInputStream, long argLength) throws SQLException {
		getResultSet().updateBlob(argColumnIndex, argInputStream, argLength);
	}

	@Override
	public void updateBlob(String argColumnLabel, InputStream argInputStream, long argLength) throws SQLException {
		getResultSet().updateBlob(argColumnLabel, argInputStream, argLength);
	}

	@Override
	public void updateCharacterStream(int argColumnIndex, Reader argReader) throws SQLException {
		getResultSet().updateCharacterStream(argColumnIndex, argReader);
	}

	@Override
	public void updateCharacterStream(String argColumnLabel, Reader argReader) throws SQLException {
		getResultSet().updateCharacterStream(argColumnLabel, argReader);
	}
	
	@Override
	public void updateCharacterStream(int argColumnIndex, Reader argReader, long argLength) throws SQLException {
		getResultSet().updateCharacterStream(argColumnIndex, argReader, argLength);
	}

	@Override
	public void updateCharacterStream(String argColumnLabel, Reader argReader, long argLength) throws SQLException {
		getResultSet().updateCharacterStream(argColumnLabel, argReader, argLength);
	}

	@Override
	public void updateClob(int argColumnIndex, Reader argReader) throws SQLException {
		getResultSet().updateClob(argColumnIndex, argReader);
	}

	@Override
	public void updateClob(String argColumnLabel, Reader argReader) throws SQLException {
		getResultSet().updateClob(argColumnLabel, argReader);
	}

	@Override
	public void updateClob(int argColumnIndex, Reader argReader, long argLength) throws SQLException {
		getResultSet().updateClob(argColumnIndex, argReader, argLength);
	}

	@Override
	public void updateClob(String argColumnLabel, Reader argReader, long argLength) throws SQLException {
		getResultSet().updateClob(argColumnLabel, argReader, argLength);
	}

	@Override
	public void updateNCharacterStream(int argColumnIndex, Reader argReader) throws SQLException {
		getResultSet().updateNCharacterStream(argColumnIndex, argReader);
	}

	@Override
	public void updateNCharacterStream(String argColumnLabel, Reader argReader) throws SQLException {
		getResultSet().updateNCharacterStream(argColumnLabel, argReader);
	}

	@Override
	public void updateNCharacterStream(int argColumnIndex, Reader argReader, long argLength) throws SQLException {
		getResultSet().updateNCharacterStream(argColumnIndex, argReader, argLength);
	}

	@Override
	public void updateNCharacterStream(String argColumnLabel, Reader argReader, long argLength) throws SQLException {
		getResultSet().updateNCharacterStream(argColumnLabel, argReader, argLength);
	}

	@Override
	public void updateNClob(int argColumnIndex, NClob argClob) throws SQLException {
		getResultSet().updateNClob(argColumnIndex, argClob);

	}
	@Override
	public void updateNClob(String argColumnLabel, NClob argClob) throws SQLException {
		getResultSet().updateNClob(argColumnLabel, argClob);
	}

	@Override
	public void updateNClob(int argColumnIndex, Reader argReader) throws SQLException {
		getResultSet().updateNClob(argColumnIndex, argReader);
	}

	@Override
	public void updateNClob(String argColumnLabel, Reader argReader) throws SQLException {
		getResultSet().updateNClob(argColumnLabel, argReader);
	}

	@Override
	public void updateNClob(int argColumnIndex, Reader argReader, long argLength) throws SQLException {
		getResultSet().updateNClob(argColumnIndex, argReader);
	}

	@Override
	public void updateNClob(String argColumnLabel, Reader argReader, long argLength) throws SQLException {
		getResultSet().updateNClob(argColumnLabel, argReader);
	}

	@Override
	public void updateNString(int argColumnIndex, String argString) throws SQLException {
		getResultSet().updateNString(argColumnIndex, argString);
	}

	@Override
	public void updateNString(String argColumnLabel, String argString) throws SQLException {
		getResultSet().updateNString(argColumnLabel, argString);
	}

	@Override
	public void updateRowId(int argColumnIndex, RowId argX) throws SQLException {
		getResultSet().updateRowId(argColumnIndex, argX);
	}
	
	@Override
	public void updateRowId(String argColumnLabel, RowId argX) throws SQLException {
		getResultSet().updateRowId(argColumnLabel, argX);
	}

	@Override
	public void updateSQLXML(int argColumnIndex, SQLXML argXmlObject) throws SQLException {
		getResultSet().updateSQLXML(argColumnIndex, argXmlObject);
	}

	@Override
	public void updateSQLXML(String argColumnLabel, SQLXML argXmlObject) throws SQLException {
		getResultSet().updateSQLXML(argColumnLabel, argXmlObject);
	}

	@Override
	public boolean isWrapperFor(Class<?> argIface) throws SQLException {
		return getResultSet().isWrapperFor(argIface);
	}

	@Override
	public <T> T unwrap(Class<T> argIface) throws SQLException {
		return getResultSet().unwrap(argIface);
	}

	@Override
	public <T> T getObject(int argColumnIndex, Class<T> argType) throws SQLException {
		return getResultSet().getObject(argColumnIndex, argType);
	}

	@Override
	public <T> T getObject(String argColumnName, Class<T> argType) throws SQLException {
		return getResultSet().getObject(argColumnName, argType);
	}

}
