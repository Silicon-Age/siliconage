package com.opal.creator;

import org.w3c.dom.Node;

// import com.siliconage.xml.*;
public class Project extends OpalXMLElement {
	public Project(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		argContext.setProjectName(getRequiredAttributeValue("Name"));
	}
}
