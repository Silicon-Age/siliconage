package com.opal.creator;

import org.w3c.dom.Node;

/**
 * @author topquark
 */
public class DatabaseSubpackage extends OpalXMLElement {
	public DatabaseSubpackage(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		/* argContext.setApplicationSubpackage(getElement().getText()); */
	}
}
