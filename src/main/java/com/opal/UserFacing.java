package com.opal;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
//import java.util.List; // OPALFIXME: Restore this

/**
 * @author topquark
 */
public interface UserFacing/* <U extends UserFacing<U>> */ { // OPALFIXME
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
	
//	OPALFIXME: Restore this
//	public List<OpalField<U, ?>> getFields(); // FIXME: OpalField<U, ?>?

//	OPALFIXME: Restore this
//	default OpalField<U, ?> getField(int argFieldIndex) { // FIXME: Validate?
//		return getFields().get(argFieldIndex);
//	}
	
//	OPALFIXME: Restore this
//	default OpalField<U, ?> getField(String argFieldName) { // FIXME: Validate?
//		return getField(getFieldIndex(argFieldName));
//	}
	
	public int getFieldCount();
	public int getFieldIndex(String argFieldName);
	public String getFieldName(int argFieldIndex);
	
	/* These methods allow programs to access the data values of a UserFacing object (stored
	 * in its Opal) by name and/or index. */
	 
	public Object getFieldValue(int argFieldIndex);
	default public Object getFieldValue(String argFieldName) {
		return getFieldValue(getFieldIndex(argFieldName));
	}
	
//	// OPALFIXME: This method should be removed
//	default Object getField(int argFieldIndex) { // FIXME: Deprecated for removal
//		return getFieldValue(argFieldIndex);
//	}
//	// OPALFIXME: This method should be removed
//	default Object getField(String argFieldName) { // FIXME: Deprecated for removal
//		return getFieldValue(argFieldName);
//	}
	
	/* These methods allow programs to set the data values of a UserFacing object (stored
	 * in its Opal) by name and/or index.  Sets will be type- and null-checked appropriately. */
	 
	public void setFieldValue(int argFieldIndex, Object argValue);
	default void setFieldValue(String argFieldName, Object argValue) {
		setFieldValue(getFieldIndex(argFieldName), argValue);
	}
	
//	// OPALFIXME: This method should be removed
//	default void setField(int argFieldIndex, Object argValue) { // FIXME: Deprecated for removal
//		setFieldValue(argFieldIndex, argValue);
//	}
//	// OPALFIXME: This method should be removed
//	default void setField(String argFieldName, Object argValue) { // FIXME: Deprecated for removal
//		setFieldValue(argFieldName, argValue);
//	}
	
	/* These methods allow programs to determine the type (Java class) of each field belonging
	 * to the UserFacing object. */
	 
	public Class<?> getFieldType(int argFieldIndex);
	default public Class<?> getFieldType(String argFieldName) {
		return getFieldType(getFieldIndex(argFieldName));
	}
	
}
