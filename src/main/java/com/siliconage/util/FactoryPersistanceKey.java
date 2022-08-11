package com.siliconage.util;

/**
 * FactoryPersistanceKey is the key for a hashmap of all the objects created by a factory.
 * The key is used to ensure that generated objects are unique by class and id.
 * <BR><BR>
 * Copyright &copy; 2000 Silicon Age, Inc.  All Rights Reserved.
 * @author <a href="mailto:scoon@silicon-age.com">Scott Coon</a>
 * @author <a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public class FactoryPersistanceKey {
	private final Class<?> myValueClass;
	private final long myId;
	
	
	/**
	 * FactoryPersistanceKey is built with the database ID and class of the object the
	 * factory generated.
	 * @author <a href="mailto:scoon@silicon-age.com">Scott Coon</a>
	 * @param argId long database ID
	 * @param argValueClass Class
	 */
	public FactoryPersistanceKey(long argId, Class<?> argValueClass) {
		super();
		
		myId = argId;
		
		assert argValueClass != null;
		myValueClass = argValueClass;
	}
	
	/**
	 * Compares two objects for equality. Returns a boolean that indicates
	 * whether this object is equivalent to the specified object. This method
	 * is used when an object is stored in a hashtable.
	 * @author <a href="mailto:scoon@silicon-age.com">Scott Coon</a>
	 * @param argObject the Object to compare with
	 * @return <code>true</code> if these Objects are equal; 
	 * <code>false</code> otherwise.
	 * @see java.util.Hashtable
	 */
	@Override
	public boolean equals(Object argObject) {
		if ((argObject == null) || (!this.getClass().equals(argObject.getClass()))) {
			return false;
		} else {
			FactoryPersistanceKey lclObj = (FactoryPersistanceKey) argObject;
			return ((this.getId() == lclObj.getId()) && (this.getValueClass().equals(lclObj.getValueClass())));
		}
	}
	/**
	 * Returns the database ID of the value this key maps to.
	 * @author <a href="mailto:scoon@silicon-age.com">Scott Coon</a>
	 * @return long database ID
	 */
	public long getId() {
		return myId;
	}
	/**
	 * Returns the class of the value this key points to.
	 * @author <a href="mailto:scoon@silicon-age.com">Scott Coon</a>
	 * @return Class
	 */
	public Class<?> getValueClass() {
		return myValueClass;
	}
	
	/**
	 * Generates a hash code for the receiver.
	 * This method is supported primarily for
	 * hash tables, such as those provided in java.util.
	 * @author <a href="mailto:scoon@silicon-age.com">Scott Coon</a>
	 * @return an integer hash code for the receiver
	 * @see java.util.Hashtable
	 */
	@Override
	public int hashCode() {
		return (int)myId + myValueClass.hashCode();
	}
//	/**
//	 * Sets the database ID of the value mapped by the key.
//	 * @author <a href="mailto:scoon@silicon-age.com">Scott Coon</a>
//	 * @param argID long - the database ID of the value this key maps to
//	 */
//	private void setID(long argID) {
//		myID = argID;
//	}
	
//	/**
//	 * Sets the class of the value mapped by this key.
//	 * @author <a href="mailto:scoon@silicon-age.com">Scott Coon</a>
//	 * @param argValueClass Class of the value this key maps to.
//	 * @throws IllegalArgumentException if the argument is <code>null</code>
//	 */
//	private void setValueClass(Class argValueClass) {
//		if (argValueClass == null) {
//			throw new IllegalArgumentException("argValueClass cannot be null in " + getClass().getName() + ".setValueClass().");
//		}
//		myValueClass = argValueClass;
//	}
	
	/**
	 * Returns:
	 * <UL>
	 * <LI>the name of the value class</LI>
	 * <LI>:</LI>
	 * <LI>the database ID</LI>
	 * </UL> 
	 * @author <a href="mailto:scoon@silicon-age.com">Scott Coon</a>
	 * @return String 
	 */
	@Override
	public String toString() {
		return getValueClass().getName() + ":" + getId();
	}
}
