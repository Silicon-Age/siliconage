package com.opal.creator;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Subpackage extends OpalXMLElement {
	
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(Subpackage.class.getName());

	public Subpackage(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
		
	}

	@Override
	protected void postChildren(OpalParseContext argContext) throws Exception {
		argContext.setCurrentSubpackage(argContext.getPreviousSubpackage());
	}

	@Override
	protected void preChildren(OpalParseContext argContext) throws Exception {
		Element lclE = (Element) getNode();
		
		String lclSubpackageName = StringUtils.trimToNull(lclE.getAttribute("Name"));
		// FIXME: Is there a method for checking whether something is a valid package name?
		if (lclSubpackageName == null) {
			throw new IllegalStateException("Subpackage element specified a blank name.");
		}
		ourLogger.debug("Working on Subpackage " + lclSubpackageName + ".");
		
		argContext.setPreviousSubpackage(argContext.getCurrentSubpackage());
		argContext.setCurrentSubpackage(lclSubpackageName);
		// FIXME: Does uniqueness matter here?
		// FIXME: How does nesting work?
	}
		
}
