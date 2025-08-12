package com.opal;

/**
 * @author topquark
 */
public class NullValueException extends Exception {
	private static final long serialVersionUID = 1L;
	
	/**
	 * thrown when an automatically generated primitive-valued accessor for an Opal
	 * is called for a field that is null.
	 * 
	 * <p>This checked exception can easily become very annoying in user code, however
	 * that is by design:  code should never assume that database columns will never
	 * contain nulls if they are marked as allowing NULL.  To eliminate the need to
	 * handle this exception, alter your database column to disallow nulls and rebuild
	 * your opals.  If you can't do that, set the <code>NullsAllowed</code> parameter on
	 * the <code>Column</code> element of your opal configuration file to <code>false</code>
	 * for the relevant column.</p>
	 * 
	 * <p>For what it's worth, I predict that choosing the followthe latter course will bite you at
	 * least once.  Database integrity constraints are your friend; use them whenever it is
	 * possible.</p>
	 */
	public NullValueException() {
		super();
	}

	/**
	 * @param argMessage the message indicating the object and field that was null.
	 */
	public NullValueException(String argMessage) {
		super(argMessage);
	}
}
