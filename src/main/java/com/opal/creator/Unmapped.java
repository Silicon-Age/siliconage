package com.opal.creator;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author topquark
 */
public class Unmapped extends OpalXMLElement {
	
	public Unmapped(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}

	@Override
	protected void preChildren(OpalParseContext argContext) {
		List<Element> lclList = getChildElements("Column");
		for (Element lclE : lclList) {
			System.out.println("Adding \"" + lclE.getTextContent() + "\" to the list of columns that are unmapped by default");
			argContext.getUnmappedColumnNameList().add(lclE.getTextContent());
		}
	}
	
	@Override
	protected boolean descend() {
		return false;
	}
}
