package com.siliconage.util;


/**
 * This exception is intended to be thrown when an object is not found in the database.
 * Once created, this class is immutable.
 * <BR><BR>
 * Copyright &copy; 2000 Silicon Age, Inc. All Rights Reserved.
 * @author <a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
 * @author <a href="http://www.silicon-age.com">Silicon Age, Inc.</a>
 */
public class ObjectDoesNotExistException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private String myErrorMessage;
	
	/**
	 * Default ObjectDoesNotExistException constructor.
	 * @author <a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
	 */
	public ObjectDoesNotExistException() {
		super();
	}
	
	/**
	 * ObjectDoesNotExistException constructor that sets the value of the error message.
	 * If the argument is null, no value is set.
	 * @author <a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
	 * @param argErrorMessage String
	 */
	public ObjectDoesNotExistException(String argErrorMessage) {
		super();
	
		if (argErrorMessage != null) {
			setErrorMessage(argErrorMessage);
		}
	}
	
	/**
	 * Returns the error message of this exception.
	 * @author <a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
	 * @return String
	 */
	public String getErrorMessage() {
		return myErrorMessage;
	}
	
	/**
	 * Sets the error message of this exception.
	 * Method is private to ensure immutability.
	 * @author <a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
	 * @param argErrorMessage String
	 */
	private void setErrorMessage(String argErrorMessage) {
		myErrorMessage = argErrorMessage;
	}
	
	/**
	 * Returns the error message of this exception.
	 * @author <a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
	 * @return String - the error message of this exception.
	 */
	@Override
	public String toString() {
		return getErrorMessage();
	}
}
