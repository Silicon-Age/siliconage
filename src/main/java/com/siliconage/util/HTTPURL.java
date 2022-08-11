package com.siliconage.util;
import java.io.Serializable;

/**
 * Encapsulates a String containing a valid HTTP URL.
 * The class is created only if the String does represent an valid absolute HTTP URL.
 * Note that a URL beginning with https:// will <b>not</b> be considered valid.
 * Once created, this class is immutable.
 * <BR><BR>
 * Copyright &copy; 2000, 2001 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class HTTPURL implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String myName;
	
	/**
	 * Default HTTPURL constructor.
	 * <B>This should not be called except for database persistence.</B>
	 */
	public HTTPURL(){
		super();
	}
	
	/**
	 * HTTPURL constructor that takes a String, and performs tests on it to
	 * ensure that it represents a valid absolute HTTP URL.
	 * @param argName - the String to be encapsulated by this object
	 * @throws IllegalArgumentException if the String does not represent a valid absolute HTTP URL
	 */
	public HTTPURL(String argName) {
		super();
		setName(argName);
	}
	
	/**
	 * Returns the string encapsulated by the this object.
	 * @return String
	 */
	public String getName() {
		return myName;
	}
	
	/**
	 * Static method that returns <code>true</code> if the specified String is a valid
	 * HTTP URL.  Note that a URL beginning with https:// will <b>not</b> be considered valid.
	 * <BR>
	 * A valid URL will:	
	 * <UL>
	 * <LI>be non-null</LI>
	 * <LI>have a length greater than zero</LI>
	 * <LI>begin with http://</LI>
	 * <LI>have at least one character between the second / sign and a period</LI>
	 * <LI>have at least one character after the last period</LI>
	 * <LI>contain no spaces</LI>
	 * </UL>
	 * @param argName - the String to be checked
	 * @return <code>true</code> if the String is a valid HTTP URL;
	 * <code>false</code> otherwise.
	 */
	public static boolean isHTTPURL(String argName) {
		int lclFixedCharsInFront = 7;					// 7 chars must lead off each URL, namely, http://
	
		if (argName != null && argName.length() > 0) {			// argument must be non-null and have non-zero length
	
			int lclSlashIndex = lclFixedCharsInFront - 1;
			int lclPeriodIndex = argName.lastIndexOf('.');
			int lclSpaceIndex = argName.indexOf(' ');
			int lclLastIndexOfString = argName.length() - 1;	 	// String index goes from 0 to length-1
	
			if ((argName.substring(0, lclFixedCharsInFront).equals("http://")) && 		// Must begin with http://
				(lclPeriodIndex >= lclSlashIndex + 1) &&	   				// Must be at least one valid char between second '/' and '.'
				(lclPeriodIndex != lclLastIndexOfString) &&	 				// Must be at least one valid char after '.'
				(lclSpaceIndex  < 0)) {							// Must not have any spaces anywhere in String
				
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Sets the string encapsulated by the this object.
	 * Method is private to ensure immutability.
	 * @param argName String
	 * @throws IllegalArgumentException if the String does not represent a valid absolute HTTP URL
	 */
	private void setName(String argName) {
		if (isHTTPURL(argName)) {
			myName = argName;
		} else {
			throw new IllegalArgumentException("Argument does not represent a valid absolute HHTP URL address in " + 
								getClass().getName() + " constructor.");
		}
	}
	
	/**
	 * @return the string encapsulated by the this object.
	 */
	@Override
	public String toString() {
		return getName();
	}
}
