package com.opal.creator;

import org.w3c.dom.Node;

public class Schema extends OpalXMLElement {
	public Schema(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		argContext.setDefaultSchema(getNode().getTextContent());
		argContext.getRelationalDatabaseAdapter().setDefaultSchema(getNode().getTextContent());
	}
}
