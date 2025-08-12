package com.opal.creator;

import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.opal.creator.database.Key;

/**
 * @author topquark
 */
public class MultipleLookUp extends OpalXMLElement {

	public MultipleLookUp(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected boolean descend() {
		return false;
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		List<Element> lclChildren = getChildElements("Column");

		MappedClass lclMC = ((Mapping) getParent()).getMappedClass();

		if (lclChildren.size() == 0) {
			lclMC.complain(MessageLevel.Warning, "MultipleLookUp has no children specifying component columns.  Skipping.");
		} else {
			
			Key lclKey = new Key(
				lclMC.getTableName(),
				"UNNAMED_KEY",
				false // not required
			);
			Iterator<?> lclI = lclChildren.iterator();
			while (lclI.hasNext()) {
				lclKey.getColumnNames().add(((Element) lclI.next()).getAttribute("Name")); // FIXME: Could use better error checking and reporting
			}

			lclMC.addMultipleLookUpSpecification(new MultipleLookUpSpecification(lclMC, lclKey));
			
			lclMC.complain(MessageLevel.Debug, "Created MultipleLookUp index " + lclKey);
			
		}
		return;
	}
}
