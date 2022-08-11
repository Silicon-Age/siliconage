package com.siliconage.xml;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class XMLElement {
	private final Node myNode;
	private final XMLElement myParent;
	
	protected XMLElement(XMLElement argParent, Node argNode) {
		super();
		
		myParent = argParent;
		myNode = argNode;
	}
	
	public static String getAttributeValue(Node argN, String argAttributeName) {
		NamedNodeMap lclNNM = argN.getAttributes();
		if (lclNNM == null) {
			throw new IllegalStateException("Invoked getAttributeValue on a node with no attributes");
		} else {
			Node lclN = lclNNM.getNamedItem(argAttributeName);
			if (lclN == null) {
				return null;
			} else {
				return lclN.getNodeValue();
			}
		}
		
	}
	protected String getAttributeValue(String argAttributeName) {
		return getAttributeValue(getNode(), argAttributeName);
	}
	
	protected String getAttributeValue(String argAttributeName, String argDefault) {
		String lclValue = getAttributeValue(argAttributeName);
		return (lclValue != null) ? lclValue : argDefault;
	}
	
	protected Node getNode() {
		return myNode;
	}
	
	public XMLElement getParent() {
		return myParent;
	}
	
	protected String getRequiredAttributeValue(String argAttributeName) {
		String lclValue = getAttributeValue(argAttributeName);
		if (lclValue == null) {
			throw new IllegalStateException("Required attribute " + argAttributeName + " not found.");
		}
		return lclValue;
	}
	
	@SuppressWarnings("unused")
	protected void postChildren(ParseContext argContext) throws Exception {
		return;
	}
	
	@SuppressWarnings("unused")
	protected void preChildren(ParseContext argContext) throws Exception {
		return;
	}
	
	protected boolean descend() throws Exception {
		return true;
	}
	
	public void process(ParseContext argContext) throws Exception {
		preChildren(argContext);
		if (descend()) {
			processChildren(argContext);
		} 
		postChildren(argContext);
	}
	
	protected void processChildren(ParseContext argContext) throws Exception {
		NodeList lclList = getNode().getChildNodes();
		int lclLength = lclList.getLength();
		for (int lclI = 0; lclI < lclLength; ++lclI) {
			Node lclN = lclList.item(lclI);
			if (lclN instanceof Element) {
				Element lclE = (Element) lclN;
				argContext.createXMLElement(this, lclE).process(argContext);
			}
		}
	}
	
//	protected void setNode(Node argNode) {
//		myNode = argNode;
//	}
	
//	protected void setParent(XMLElement argParent) {
//		myParent = argParent;
//	}
}
