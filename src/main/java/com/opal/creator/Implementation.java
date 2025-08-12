package com.opal.creator;

import org.w3c.dom.Node;

/**
 * @author topquark
 */
public class Implementation extends OpalXMLElement {
	public Implementation(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) throws Exception {
		((Mapping) getParent()).getMappedClass().setSpecifiedImplementationClassName(getNode().getTextContent());
	}
}
