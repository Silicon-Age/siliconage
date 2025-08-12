package com.opal.creator;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author topquark
 */
public class Comparators extends OpalXMLElement {
	
	public Comparators(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}

	@Override
	protected void preChildren(OpalParseContext argContext) {
		List<Element> lclList = getChildElements("Column");
		for (Element lclE : lclList) {
//			System.out.println("Adding " + lclE.getText() + " to default comparator column name list");
			argContext.getComparatorColumnNameList().add(lclE.getTextContent());
		}
	}
	
	@Override
	protected boolean descend() {
		return false;
	}
}
