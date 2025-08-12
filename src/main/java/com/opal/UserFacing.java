package com.opal;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * @author topquark
 */
public interface UserFacing /* extends Serializable */ {
	/**
	 * outputs the internal data of the object to the specified {@link PrintStream} in lines of the form "FIELD_NAME=field_value".
	 *
	 * <p>This is primarily intended as an aid to debugging since the output is unformatted.</p>
	 *
	 * @param argPS the <code>PrintStream</code> to which the object's contents are to be written.  May not be <code>null</code>.
	 * @throws IOException when problems occur writing to <code>argPS</code>
	 */
	public void output(PrintStream argPS) throws IOException;

	/**
	 * outputs the internal data of the object to the specified <code>PrintWriter</code> in lines of the form "FIELD_NAME=field_value".
	 *
	 * <p>This is primarily intended as an aid to debugging since the output is unformatted.</p>
	 *
	 * @param argPW the <code>PrintWriter</code> to which the object's contents are to be written.  May not be <code>null</code>.
	 * @throws IOException when problems occur writing to <code>argPW</code>
	 */
	public void output(PrintWriter argPW) throws IOException;
	
	/* These methods are a sort of poor-man's (also a fast man's) reflection for accessing
	 * the names of the basic fields of the UserFacing object. */
	
	public int getFieldCount();
	public int getFieldIndex(String argFieldName);
	public String getFieldName(int argFieldIndex);
	
	/* These methods allow programs to access the data values of a UserFacing object (stored
	 * in its Opal) by name and/or index. */
	 
	public Object getField(int argFieldIndex);
	default public Object getField(String argFieldName) {
		return getField(getFieldIndex(argFieldName));
	}
	
	/* These methods allow programs to set the data values of a UserFacing object (stored
	 * in its Opal) by name and/or index.  Sets will be type- and null-checked appropriately. */
	 
	public void setField(int argFieldIndex, Object argValue);
	default public void setField(String argFieldName, Object argValue) {
		setField(getFieldIndex(argFieldName), argValue);
	}
	
	/* These methods allow programs to determine the type (Java class) of each field belonging
	 * to the UserFacing object. */
	 
	public Class<?> getFieldType(int argFieldIndex);
	default public Class<?> getFieldType(String argFieldName) {
		return getFieldType(getFieldIndex(argFieldName));
	}
	
//	public void unlink();
//	public void reload();
	
//	public String getUniqueString();
	
//	public boolean isNew();
//	public boolean isDeleted();
	
//	public String toDebugString();
	
//	public int extendedHashCode();
	
}
