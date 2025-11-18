package com.opal.creator;

import org.w3c.dom.Node;

public class Pool extends OpalXMLElement {
	
	protected String myJNDIName;
	
	public Pool(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}	
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		String lclPoolName = getAttributeValue("Name", OpalParseContext.DEFAULT_POOL_NAME);
		String lclJNDIName = getRequiredAttributeValue("JNDIName");
		argContext.getPoolMap().put(lclPoolName, lclJNDIName);
		// FIXME: Make sure it didn't replace another one.
	}
}
