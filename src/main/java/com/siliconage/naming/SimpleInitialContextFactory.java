package com.siliconage.naming;
import java.util.Hashtable;
import javax.naming.Context;
// import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * @author topquark
 */
public class SimpleInitialContextFactory implements InitialContextFactory {
	private SimpleContext mySimpleContext;
	
	public SimpleInitialContextFactory() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.spi.InitialContextFactory#getInitialContext(java.util.Hashtable)
	 */
	@Override
	public synchronized Context getInitialContext(Hashtable<?, ?> argHT) {
		if (mySimpleContext == null) {
			mySimpleContext = new SimpleContext();
		}
		return mySimpleContext;
	}
}
