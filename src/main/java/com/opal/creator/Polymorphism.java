package com.opal.creator;

import java.util.List;

import org.apache.commons.lang3.Validate;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Polymorphism extends OpalXMLElement {
	public final String[] ourDereferenceMethods = new String[] { "Table", "Type" };
	
	public Polymorphism(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected boolean descend() {
		return false;
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) throws Exception {
		String lclPolymorphismType = getAttributeValue("Type");
		
		switch (lclPolymorphismType) {
		case "Subtable":
			handleSubtable();
			break;
		case "SingleTable":
			handleSingleTable();
			break;
		default:
			throw new IllegalStateException("Polymorphism element for " + getTableName() + " doesn't specify either Subtable or SingleTable.");
		}
		
		return;
	}
	
	protected Mapping getMapping() {
		return (Mapping) getParent();
	}
	
	protected MappedClass getMappedClass() {
		return getMapping().getMappedClass();
	}
	
	protected String getTableName() {
		return getMapping().getMappedClass().getTableName().getTableName();
	}
	
	protected void handleSingleTable() {
		List<Element> lclEs = getChildElements("ConcreteTypeMethod");
		if (lclEs.size() == 0) {
			throw new IllegalStateException("SingleTable Polymorphism element for table " + getTableName() + " does not have a ConcreteTypeMethod child.");
		} else if (lclEs.size() > 1) {
			throw new IllegalStateException("SingleTable Polymorphism element for table " + getTableName() + " has more than one ConcreteTypeMethod child.");
		}
		
		Element lclE = lclEs.get(0);
		assert lclE != null;
		
		SingleTablePolymorphicData lclPD = new SingleTablePolymorphicData();
		
		MappedClass lclMC = getMappedClass();
		
		NodeList lclNL = lclE.getElementsByTagName("Dereference");
		for (int lclI = 0; lclI < lclNL.getLength(); ++lclI) {
			Node lclN = lclNL.item(lclI);
			assert lclN instanceof Element;
			Element lclD = (Element) lclN;
			
			lclMC.complain(MessageLevel.Debug, "Processing Dereference element #" + lclI);
			
			for (String lclMethod : ourDereferenceMethods) {
				String lclName = lclD.getAttribute(lclMethod);
				lclMC.complain(MessageLevel.Debug, "It is method " + lclMethod + " with name " + lclName);
				lclPD.getDereferenceMethods().add(lclMethod);
				lclPD.getDereferenceNames().add(lclName);
				break;
			}
		}
		
		lclNL = lclE.getElementsByTagName("Column");
		Validate.isTrue(lclNL.getLength() != 0, "ConcreteTypeMethod element for table " + getTableName() + " has no Column element.");
		Validate.isTrue(lclNL.getLength() <= 1, "ConcreteTypeMethod element for table " + getTableName() + " has more than one Column element.");
		
		Node lclN = lclNL.item(0);
		assert lclN instanceof Element;
		
		Element lclC = (Element) lclN;
		
		String lclName = lclC.getAttribute("Name");
		Validate.notNull(lclName, "Name attribute for Column element in ConcreteTypeMethod element for " + getTableName() + " is missing.");
		
		String lclMethod = lclC.getAttribute("Method");
		Validate.notNull(lclMethod, "Method attribute for Column element in ConcreteTypeMethod element for " + getTableName() + " is missing.");
		
		// FIXME: Ensure that method is recognized
		
		lclPD.setColumnName(lclName);
		lclPD.setMethod(lclMethod);
		
		lclMC.setPolymorphicData(lclPD);
		
		lclMC.complain(MessageLevel.Debug, "Table " + getMappedClass().getTableName() + " has " + ((SingleTablePolymorphicData) getMappedClass().getPolymorphicData()).getDereferenceMethods().size() + " dereference methods.");

		return;
	}
	
	protected void handleSubtable() {
		List<Element> lclEs = getChildElements("ConcreteTypeMethod");
		if (lclEs.size() == 0) {
			throw new IllegalStateException("Subtable Polymorphism element for table " + getTableName() + " does not have a ConcreteTypeMethod child.");
		} else if (lclEs.size() > 1) {
			throw new IllegalStateException("Subtable Polymorphism element for table " + getTableName() + " has more than one ConcreteTypeMethod child.");
		}
		
		Element lclE = lclEs.get(0);
		assert lclE != null;
		
		SubtablePolymorphicData lclPD = new SubtablePolymorphicData();
		
		MappedClass lclMC = getMappedClass();
		
		NodeList lclNL = lclE.getElementsByTagName("Dereference");
		for (int lclI = 0; lclI < lclNL.getLength(); ++lclI) {
			Node lclN = lclNL.item(lclI);
			assert lclN instanceof Element;
			Element lclD = (Element) lclN;
			
			lclMC.complain(MessageLevel.Debug, "Processing Dereference element #" + lclI);
			
			for (String lclMethod : ourDereferenceMethods) {
				String lclName = lclD.getAttribute(lclMethod);
//				if (lclName != null) { // According to FindBugs, getAttribute will never return null
				lclMC.complain(MessageLevel.Debug, "It is method " + lclMethod + " with name " + lclName);
					lclPD.getDereferenceMethods().add(lclMethod);
					lclPD.getDereferenceNames().add(lclName);
					break;
//				} 
			}
		}
		
		lclNL = lclE.getElementsByTagName("Column");
		Validate.isTrue(lclNL.getLength() != 0, "ConcreteTypeMethod element for table " + getTableName() + " has no Column element.");
		Validate.isTrue(lclNL.getLength() <= 1, "ConcreteTypeMethod element for table " + getTableName() + " has more than one Column element.");
		
		Node lclN = lclNL.item(0);
		assert lclN instanceof Element;
		
		Element lclC = (Element) lclN;
		
		String lclName = lclC.getAttribute("Name");
		Validate.notNull(lclName, "Name attribute for Column element in ConcreteTypeMethod element for " + getTableName() + " is missing.");
		
		String lclMethod = lclC.getAttribute("Method");
		Validate.notNull(lclMethod, "Method attribute for Column element in ConcreteTypeMethod element for " + getTableName() + " is missing.");
		
		// FIXME: Ensure that method is recognized
		
		lclPD.setColumnName(lclName);
		lclPD.setMethod(lclMethod);
		
		lclMC.setPolymorphicData(lclPD);
		
		lclMC.complain(MessageLevel.Debug, "Table " + getMappedClass().getTableName() + " has " + ((SubtablePolymorphicData) getMappedClass().getPolymorphicData()).getDereferenceMethods().size() + " dereference methods.");
		return;
	}
}
