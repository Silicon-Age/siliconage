package com.opal.creator;

import org.w3c.dom.Node;

/**
 * @author topquark
 */
public class Author extends OpalXMLElement {
	public Author(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		argContext.setAuthor(getNode().getTextContent());
	}
}
