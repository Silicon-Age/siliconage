package com.opal.creator;

import org.w3c.dom.Node;

/**
 * @author topquark
 */
public class TypeMapping extends OpalXMLElement {

	public TypeMapping(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}

	@Override
	protected void preChildren(OpalParseContext argContext) {
		String lclDatabaseType = getAttributeValue("Database");
		if (lclDatabaseType == null) {			
			XMLCreator.complain(MessageLevel.Error, "TypeMapping element is missing required name of database type.");
			return;
		}
		String lclJavaType = getAttributeValue("Java");
		if (lclJavaType == null) {
			XMLCreator.complain(MessageLevel.Error,  "TypeMapping element is missing required name of Java type.");
			return;
		}
		XMLCreator.complain(MessageLevel.Info, "Type-mapping \"" + lclDatabaseType + "\" to \"" + lclJavaType + "\".");
//		System.out.println("Mapping " + lclDatabaseType + " to " + lclJavaType);
		argContext.getRelationalDatabaseAdapter().getUserTypeMappings().put(lclDatabaseType, lclJavaType);
	}
	
	@Override
	protected boolean descend() {
		return false;
	}
}
