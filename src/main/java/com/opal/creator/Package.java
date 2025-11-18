package com.opal.creator;

import org.w3c.dom.Node;

public class Package extends OpalXMLElement {
	public Package(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		argContext.setDefaultPackage(getNode().getTextContent());
	}
}
