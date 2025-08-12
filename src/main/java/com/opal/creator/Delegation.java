package com.opal.creator;

import org.w3c.dom.Node;

public class Delegation extends OpalXMLElement {
	public Delegation(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) throws Exception {
		String lclClassName = getRequiredAttributeValue("Class");
		String lclMethodName = getRequiredAttributeValue("Method");
		String lclLocalMethodName = getAttributeValue("Name");
		String lclReturnTypeName = getRequiredAttributeValue("ReturnType");
		
		((Mapping) getParent()).getMappedClass().add(
			new MethodDelegation(
				lclClassName,
				lclMethodName,
				lclLocalMethodName,
				Class.forName(lclReturnTypeName)
			)
		);
		
		return;
	}
}
