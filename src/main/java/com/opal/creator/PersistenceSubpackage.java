package com.opal.creator;

import org.w3c.dom.Node;

/**
 * @author topquark
 */
public class PersistenceSubpackage extends OpalXMLElement {
	public PersistenceSubpackage(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		argContext.setPersistenceSubpackage(getNode().getTextContent());
	}
}
