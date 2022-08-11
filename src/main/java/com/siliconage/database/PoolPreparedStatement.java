package com.siliconage.database;
import java.io.Reader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.ParameterMetaData;
import java.net.URL;
import java.util.Calendar;
import com.siliconage.util.UnimplementedOperationException;

//import com.siliconage.util.UnimplementedOperationException;

/**
 * Copyright &copy; 2000 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:topquark@silicon-age.com">R. Robert Hentzel</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class PoolPreparedStatement extends PoolStatement implements PreparedStatement {
	private PreparedStatement myPreparedStatement;
	
	/**
	 * Constructs an instance of PoolPreparedStatement and sets the internal
	 * PreparedStatement equal to argPS.
	 * @param argPS PreparedStatement
	 * @param argPC PoolConnection
	 */
	public PoolPreparedStatement(PreparedStatement argPS, PoolConnection argPC) {
		super(argPS, argPC);
		setPreparedStatement(argPS);
		init();
	}
	
//	protected void init() {
//		/* Nothing to do*/
//	}
//	
	/**
	 * Calls addBatch() on the internal PreparedStatement.
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void addBatch() throws SQLException {
		getPreparedStatement().addBatch();
	}
	/**
	 * Clears the parameters of the internal PreparedStatement
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void clearParameters() throws SQLException {
		getPreparedStatement().clearParameters();
	}
	/**
	 * Closes the internal PreparedStatement and returns this to the PoolPreparedStatement.
	 */
	@Override
	public void close() {
		// Don't call super or we'll returnPoolStatement as well and that will really screw things up 
		DatabaseUtility.closeStatement(getPreparedStatement());
//		DatabaseReuser.returnPoolPreparedStatement(this);
	}
	/**
	 * Executes the internal PreparedStatement.
	 * @return boolean The number of rows affected
	 * @throws SQLException If there is a problem
	 */
	@Override
	public boolean execute() throws SQLException {
		return getPreparedStatement().execute();
	}
	/**
	 * @return ResultSet The ResultSet produced by the query
	 * @throws SQLException If there is a problem
	 */
	@Override
	public ResultSet executeQuery() throws SQLException {
		return new PoolResultSet(getPreparedStatement().executeQuery(), this);
	}
	/**
	 * Executes an update on the internal PreparedStatement.
	 * @return The number of rows affected
	 * @throws SQLException If there is a problem
	 */
	@Override
	public int executeUpdate() throws SQLException {
		return getPreparedStatement().executeUpdate();
	}
	/**
	 * Returns the meta data of the internal PreparedStatement.
	 * @return ResultSetMetaData
	 * @throws SQLException If there is a problem
	 */
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return getPreparedStatement().getMetaData();
	}
	/**
	 * Returns the internal PreparedStatement.
	 * @return The PreparedStatement
	 */
	public final PreparedStatement getPreparedStatement() {
		return myPreparedStatement;
	}
	/**
	 * Sets the array of the internal PreparedStatement.
	 * @param i int
	 * @param x Array
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setArray(int i, Array x) throws SQLException {
		getPreparedStatement().setArray(i, x);
	}
	/**
	 * Sets the ASCII steam of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x InputStream
	 * @param length int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
		getPreparedStatement().setAsciiStream(parameterIndex, x, length);
	}
	/**
	 * Sets the BigDecimal of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x BigDecimal
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		getPreparedStatement().setBigDecimal(parameterIndex, x);
	}
	/**
	 * Sets the binary stream of the PreparedStatement.
	 * @param parameterIndex int
	 * @param x InputStream
	 * @param length int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
		getPreparedStatement().setBinaryStream(parameterIndex, x,length);
	}
	/**
	 * Sets the Blob of the internal PreparedStatement.
	 * @param i int
	 * @param x Blob
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setBlob(int i, Blob x) throws SQLException {
		getPreparedStatement().setBlob(i,x);
	}
	/**
	 * Sets the boolean of the PreparedStatement.
	 * @param parameterIndex int
	 * @param x boolean
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		getPreparedStatement().setBoolean(parameterIndex, x);
	}
	/**
	 * Sets the byte of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x byte
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException {
		getPreparedStatement().setByte(parameterIndex, x);
	}
	/**
	 * Sets the bytes of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x byte[]
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		getPreparedStatement().setBytes(parameterIndex, x);
	}
	/**
	 * Sets the character stream of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param reader Reader
	 * @param length int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		getPreparedStatement().setCharacterStream(parameterIndex, reader, length);
	}
	/**
	 * Sets the Clob of the internal PreparedStatement.
	 * @param i int
	 * @param x Clob
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setClob(int i, Clob x) throws SQLException {
		getPreparedStatement().setClob(i,x);
	}
	/**
	 * Sets the Date of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x Date
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException {
		getPreparedStatement().setDate(parameterIndex, x);
	}
	/**
	 * Sets the Date of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x Date
	 * @param cal Calendar
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
		getPreparedStatement().setDate(parameterIndex, x, cal);
	}
	/**
	 * Sets the double of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x double
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {
		getPreparedStatement().setDouble(parameterIndex, x);
	}
	/**
	 * Sets the float of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x float
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {
		getPreparedStatement().setFloat(parameterIndex, x);
	}
	/**
	 * Sets the int of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setInt(int parameterIndex, int x) throws SQLException {
		getPreparedStatement().setInt(parameterIndex, x);
	}
	/**
	 * Sets the long of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x long
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {
		getPreparedStatement().setLong(parameterIndex, x);
	}
	/**
	 * Sets the <code>null</code> of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param sqlType int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		getPreparedStatement().setNull(parameterIndex, sqlType);
	}
	/**
	 * Sets the <code>null</code> of the internal PreparedStatement.
	 * @param paramIndex int
	 * @param sqlType int
	 * @param typeName String
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
		getPreparedStatement().setNull(paramIndex, sqlType, typeName);
	}
	/**
	 * Sets the Object of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x Object
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException {
		getPreparedStatement().setObject(parameterIndex, x);
	}
	/**
	 * Sets the Object of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x Object
	 * @param targetSqlType int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		getPreparedStatement().setObject(parameterIndex, x, targetSqlType);
	}
	/**
	 * Sets the Object of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x Object
	 * @param targetSqlType int
	 * @param scale int
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
		getPreparedStatement().setObject(parameterIndex, x, targetSqlType, scale);
	}
	/**
	 * Sets the the internal PreparedStatement.
	 * @param argPreparedStatement PreparedStatement
	 */
	protected final void setPreparedStatement(PreparedStatement argPreparedStatement) {
		myPreparedStatement = argPreparedStatement;
	}
	/**
	 * Sets the Ref of the internal PreparedStatement.
	 * @param i int
	 * @param x Ref
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setRef(int i, Ref x) throws SQLException {
		getPreparedStatement().setRef(i,x);
	}
	/**
	 * Sets the short of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x short
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {
		getPreparedStatement().setShort(parameterIndex, x);
	}
	/**
	 * Sets the String of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x String
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setString(int parameterIndex, String x) throws SQLException {
		getPreparedStatement().setString(parameterIndex, x);
	}
	/**
	 * Sets the Time of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x Time
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException {
		getPreparedStatement().setTime(parameterIndex, x);
	}
	/**
	 * Sets the Time of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x Time
	 * @param cal Calendar
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
		getPreparedStatement().setTime(parameterIndex, x, cal);
	}
	/**
	 * Sets the Timestamp of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x Timestamp
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		getPreparedStatement().setTimestamp(parameterIndex, x);
	}
	/**
	 * Sets the Timetamp of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x Timestamp
	 * @param cal Calendar
	 * @throws SQLException If there is a problem
	 */
	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
		getPreparedStatement().setTimestamp(parameterIndex, x, cal);
	}
	/**
	 * Sets the Unicode stream of the internal PreparedStatement.
	 * @param parameterIndex int
	 * @param x InputStream
	 * @param length int
	 * @deprecated
	 */
	@Deprecated
	@Override
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) {
		throw new UnsupportedOperationException("Called a deprecated method.");
	}
	
	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {
		return getPreparedStatement().getParameterMetaData();
	}
	
	@Override
	public void setURL(int argInt, URL argURL) throws SQLException {
		getPreparedStatement().setURL(argInt, argURL);
	} // NOPMD by Jonah Greenthal on 9/20/14 11:19 PM
	
	@Override
	public void setAsciiStream(int argArg0, InputStream argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setAsciiStream(int argArg0, InputStream argArg1, long argArg2) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setBinaryStream(int argArg0, InputStream argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setBinaryStream(int argArg0, InputStream argArg1, long argArg2) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setBlob(int argArg0, InputStream argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setBlob(int argArg0, InputStream argArg1, long argArg2) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setCharacterStream(int argArg0, Reader argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setCharacterStream(int argArg0, Reader argArg1, long argArg2) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setClob(int argArg0, Reader argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setClob(int argArg0, Reader argArg1, long argArg2) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setNCharacterStream(int argArg0, Reader argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setNCharacterStream(int argArg0, Reader argArg1, long argArg2) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setNClob(int argArg0, NClob argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setNClob(int argArg0, Reader argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setNClob(int argArg0, Reader argArg1, long argArg2) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setNString(int argArg0, String argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setRowId(int argArg0, RowId argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setSQLXML(int argArg0, SQLXML argArg1) {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public boolean isClosed() {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public boolean isPoolable() {
		throw new UnimplementedOperationException();
	}
	
	@Override
	public void setPoolable(boolean argArg0) {
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
	
}
