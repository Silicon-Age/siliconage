package com.opal.creator;

import org.w3c.dom.Node;

/**
 * @author topquark
 */
public class ApplicationSubpackage extends OpalXMLElement {
	public ApplicationSubpackage(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		argContext.setApplicationSubpackage(getNode().getTextContent());
	}
}
