package com.siliconage.naming;
import java.util.HashMap;
import java.util.Hashtable;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import com.siliconage.util.UnimplementedOperationException;

/**
 * @author topquark
 */
public class SimpleContext implements Context {
	private static final HashMap<String, Object> myMap = new HashMap<>();
	
	public SimpleContext() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#close()
	 */
	@Override
	public void close() {
		/* Nothing to do. */
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#getNameInNamespace()
	 */
	@Override
	public String getNameInNamespace() {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#destroySubcontext(java.lang.String)
	 */
	@Override
	public void destroySubcontext(String arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#unbind(java.lang.String)
	 */
	@Override
	public void unbind(String argName) {
		myMap.remove(argName);
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#getEnvironment()
	 */
	@Override
	public Hashtable<?,?> getEnvironment() {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
	 */
	@Override
	public void destroySubcontext(Name arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#unbind(javax.naming.Name)
	 */
	@Override
	public void unbind(Name arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#lookup(java.lang.String)
	 */
	@Override
	public Object lookup(String argName) {
		return myMap.get(argName);
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#lookupLink(java.lang.String)
	 */
	@Override
	public Object lookupLink(String arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
	 */
	@Override
	public Object removeFromEnvironment(String arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
	 */
	@Override
	public void bind(String argName, Object argValue) {
		myMap.put(argName, argValue);
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
	 */
	@Override
	public void rebind(String argName, Object argValue) {
		myMap.put(argName, argValue);
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#lookup(javax.naming.Name)
	 */
	@Override
	public Object lookup(Name arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#lookupLink(javax.naming.Name)
	 */
	@Override
	public Object lookupLink(Name arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
	 */
	@Override
	public void bind(Name arg0, Object arg1) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
	 */
	@Override
	public void rebind(Name arg0, Object arg1) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
	 */
	@Override
	public void rename(String arg0, String arg1) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#createSubcontext(java.lang.String)
	 */
	@Override
	public Context createSubcontext(String arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#createSubcontext(javax.naming.Name)
	 */
	@Override
	public Context createSubcontext(Name arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
	 */
	@Override
	public void rename(Name arg0, Name arg1) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#getNameParser(java.lang.String)
	 */
	@Override
	public NameParser getNameParser(String arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#getNameParser(javax.naming.Name)
	 */
	@Override
	public NameParser getNameParser(Name arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#list(java.lang.String)
	 */
	@Override
	public NamingEnumeration<NameClassPair> list(String arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#listBindings(java.lang.String)
	 */
	@Override
	public NamingEnumeration<Binding> listBindings(String arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#list(javax.naming.Name)
	 */
	@Override
	public NamingEnumeration<NameClassPair> list(Name arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#listBindings(javax.naming.Name)
	 */
	@Override
	public NamingEnumeration<Binding> listBindings(Name arg0) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#addToEnvironment(java.lang.String, java.lang.Object)
	 */
	@Override
	public Object addToEnvironment(String arg0, Object arg1) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
	 */
	@Override
	public String composeName(String arg0, String arg1) {
		throw new UnimplementedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see javax.naming.Context#composeName(javax.naming.Name, javax.naming.Name)
	 */
	@Override
	public Name composeName(Name arg0, Name arg1) {
		throw new UnimplementedOperationException();
	}
}
