package com.opal.creator;

import org.w3c.dom.Node;

// import com.siliconage.xml.ParseContext;
// import com.siliconage.xml.XMLElement;
public class Package extends OpalXMLElement {
	public Package(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		argContext.setDefaultPackage(getNode().getTextContent());
//		System.out.println("Default package is now " + argContext.getDefaultPackage());
	}
}
