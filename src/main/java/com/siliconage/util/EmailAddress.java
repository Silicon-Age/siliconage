package com.siliconage.util;
import java.io.Serializable;

/**
 * Encapsulates a String containing a valid email address.
 * Once created, the class is immutable.
 * <BR><BR>
 * Copyright &copy; 2000, 2001 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public final class EmailAddress implements Serializable, Comparable<EmailAddress> {
	private static final long serialVersionUID = 1L;
	
	private String myEmail;
	
	/**
	 * Default EmailAddress constructor.
	 * <B>This should not be called except for database persistence.</B>
	 */
	protected EmailAddress() {
		super();
	}
	/**
	 * EmailAddress constructor that checks to see if the argument address
	 * entered is a valid email address.
	 * <BR>
	 * If the argument is not a valid email address, an exception is thrown.
	 * @param argEmail String
	 * @throws IllegalArgumentException if the argument String does not represent a
	 * valid email address.
	 */
	public EmailAddress(String argEmail) {
		setEmail(argEmail);
	}
	/**
	 * compareTo method compares the email addresses by the order inherited
	 * from the String value.
	 */
	@Override
	public int compareTo(EmailAddress argEmailAddress) {
		if (argEmailAddress == null) {
			throw new IllegalArgumentException("argEmailAddress is null");
		}
		
		return this.getEmail().compareTo(argEmailAddress.getEmail());
	}
	
	// Overridden Object methods
	/**
	 * Compares two EmailAddresses for equality. Two EmailAddresses are equal
	 * if and only if Strings that they encapsulate are equal.
	 * @param argObject the Object to compare with
	 * @return boolean - <code>true</code> if these EmailAddresses are equal; 
	 * <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object argObject) {
		// Nothing is equal to null.
		if (argObject == null) {
			return false;
		}
		
		// An EmailAddress is never equal to an object of another class, not even a	subclass.
		if (this.getClass() != argObject.getClass()) {
			return false;
		}
		
		// Two EmailAddresses are equal if and only if Strings that they
		// encapsulate are equal
		return this.getEmail().equals(((EmailAddress) argObject).getEmail());
	}
	/**
	 * Returns the email address encapsulated by this object.
	 * @return String
	 */
	public String getEmail() {
		return myEmail;
	}
	/**
	 * Returns the hash code of the encapsulated String of this EmailAddress.
	 * @return int - hash code for the receiver
	 * @see java.util.Hashtable
	 */
	@Override
	public int hashCode() {
		return getEmail().hashCode();
	}
	
	// Static methods
	/**
	 * Static method that returns true if the specified String is a valid
	 * e-mail address.
	 * <BR>
	 * A valid email address will:
	 * <UL>
	 * <LI>be non-null</LI>
	 * <LI>have a length greater than zero</LI>
	 * <LI>have an @ sign somewhere other than the first character</LI>
	 * <LI>have at least one character between the @ sign and a period</LI>
	 * <LI>have at least one character after the last period</LI>
	 * <LI>contain no spaces</LI>
	 * </UL>
	 * @param argEmail the String to be checked
	 * @return <code>true</code> if the String is a valid e-mail address;
	 * <code>false</code> otherwise.
	 */
	public static boolean isEmail(String argEmail) {
		// argument must be non-null and have non-zero length
		if (argEmail == null || argEmail.length() < 1) {
			return false;
		}
		
		int lclAtIndex = argEmail.indexOf('@');
		int lclPeriodIndex = argEmail.lastIndexOf('.');
		int lclSpaceIndex = argEmail.indexOf(' ');
		
		// String index goes from 0 to length-1
		int lclLastIndexOfString = argEmail.length() - 1;
		
		if ((lclAtIndex > 1) 								// '@' cannot be in first position
				&& (lclPeriodIndex >= lclAtIndex + 1)		// Must be at least one valid char btwn '@' and '.'
				&& (lclPeriodIndex != lclLastIndexOfString)	// Must be at least one valid char after '.'
				&& (lclSpaceIndex < 0)						// Must not have any spaces anywhere in String
		) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * Sets the value of the internal String containing the email address.
	 * This method is private to ensure immutability.
	 * <BR>
	 * If the argument is not a valid email address, an exception is thrown.
	 * @param argEmail String
 	 * @throws IllegalArgumentException if the argument String does not represent a
	 * valid email address.
	 */
	protected void setEmail(String argEmail) {
		if (isEmail(argEmail)) {		
			myEmail = argEmail;
		} else {
			throw new IllegalArgumentException(
				"Argument does not represent a valid email address in "
				+ getClass().getName() + ".setEmail().");
		}
	}
	/**
	 * Returns the email address encapsulated by this object.
	 * @return the email address encapsulated by this object.
	 */
	@Override
	public String toString() {
		return getEmail();
	}
}
