package com.opal.creator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.siliconage.xml.XMLElement;
import com.siliconage.xml.ParseContext;

public abstract class OpalXMLElement extends XMLElement {
	protected OpalXMLElement(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@SuppressWarnings("unused")
	protected void postChildren(OpalParseContext argContext) throws Exception {
		return;
	}
	
	@Override
	protected final void postChildren(ParseContext argContext) throws Exception {
		postChildren((OpalParseContext) argContext);
		return;
	}
	
	@SuppressWarnings("unused")
	protected void preChildren(OpalParseContext argContext) throws Exception {
		return;
	}
	
	@Override
	protected final void preChildren(ParseContext argContext) throws Exception {
		preChildren((OpalParseContext) argContext);
	}
	
	protected Element getSingleChildElement(String argName) {
		Validate.notNull(argName);
		Element lclFound = null;
		for (Element lclE : getChildElements()) {
			if (argName.equals(lclE.getNodeName())) {
				if (lclFound == null) {
					lclFound = lclE;
				} else {
					throw new IllegalStateException("Multiple child elements with name \"" + argName + "\" found.");
				}
			}
		}
		return lclFound;
	}
	
	protected String getSingleChildContent(String argName) {
		Validate.notNull(argName);
		Element lclE = getSingleChildElement(argName);
		if (lclE == null) {
			return null;
		} else {
			return lclE.getTextContent();
		}
	}
	
	protected Node getSingleChildNode(String argName) {
		Validate.notNull(argName);
		Node lclFound = null;
		NodeList lclList = getNode().getChildNodes();
		int lclLength = lclList.getLength();
		for (int lclI = 0; lclI < lclLength; ++lclI) {
			Node lclN = lclList.item(lclI);
			if (argName.equals(lclN.getNodeName())) {
				if (lclFound == null) {
					lclFound = lclN;
				} else {
					throw new IllegalStateException("Multiple child nodes with name \"" + argName + "\" found.");
				}
			}
		}
		return lclFound;
	}
	
	protected List<Element> getChildElements() {
		ArrayList<Element> lclAL = new ArrayList<>();
		NodeList lclList = getNode().getChildNodes();
		int lclLength = lclList.getLength();
		for (int lclI = 0; lclI < lclLength; ++lclI) {
			Node lclN = lclList.item(lclI);
			if (lclN instanceof Element) {
				Element lclE = (Element) lclN;
				lclAL.add(lclE);
			}
		}
		return lclAL;
	}
	
	protected List<Element> getChildElements(String argName) {
		Validate.notNull(argName);
		
		ArrayList<Element> lclAL = new ArrayList<>();
		NodeList lclList = getNode().getChildNodes();
		int lclLength = lclList.getLength();
		for (int lclI = 0; lclI < lclLength; ++lclI) {
			Node lclN = lclList.item(lclI);
			if (lclN instanceof Element) {
				Element lclE = (Element) lclN;
				if (argName.equals(lclE.getNodeName())) {
					lclAL.add(lclE);
				}
			}
		}
		return lclAL;
	}
}
