package com.siliconage.naming;
import java.util.Hashtable;

// import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

/**
 * @author topquark
 */
public class SimpleInitialContextFactoryBuilder implements InitialContextFactoryBuilder {
	public SimpleInitialContextFactoryBuilder() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.spi.InitialContextFactoryBuilder#createInitialContextFactory(java.util.Hashtable)
	 */
	@Override
	public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> argHT) {
		return new SimpleInitialContextFactory();
	}
}
