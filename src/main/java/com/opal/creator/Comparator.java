package com.opal.creator;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author topquark
 */
public class Comparator extends OpalXMLElement {
	public Comparator(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected boolean descend() {
		return false;
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) {
		Node lclE = getNode();
		NodeList lclList = lclE.getChildNodes();
		int lclLength = lclList.getLength();
		
		MappedClass lclMC = ((Mapping) getParent()).getMappedClass();

		if (lclLength == 0) {
			lclMC.complain(MessageLevel.Error, "Comparator in " + lclMC + " has no children specifying component columns.  Skipping.");
		} else {			
			String lclName = StringUtils.trimToNull(((Element) lclE).getAttribute("Name"));

			boolean lclGood = true;
			ArrayList<ComparatorCriterion> lclCriteria = new ArrayList<>();
			
			for (int lclI = 0; lclI < lclLength; ++lclI) {
				Node lclN = lclList.item(lclI);
				if (!(lclN instanceof Element)) {
					continue;
				} else {
					Element lclC = (Element) lclN;
					if ("Column".equals(lclC.getNodeName())) {
						String lclColumnName = StringUtils.trimToNull(lclC.getAttribute("Name"));
						if (lclColumnName != null) {
							ClassMember lclCM = lclMC.getClassMemberByColumnName(lclColumnName);
							if (lclCM != null) {
								boolean lclInvert = Boolean.parseBoolean(lclC.getAttribute("Invert"));
								lclCriteria.add(new SimpleFieldCriterion(lclCM, lclInvert));
							} else {
								lclMC.complain(MessageLevel.Warning, "Could not find column of " + lclMC + " with name \"" + lclColumnName + "\" while defining a Comparator; that Comparator will not be created.");
								lclGood = false;
							}
						} else {
							System.out.println("Required Name attribute for Column child for Comparator on " + lclMC + " is missing.");
							lclGood = false;
						}
					} else if ("Child".equals(lclC.getNodeName())) {
						String lclChildName = StringUtils.trimToNull(lclC.getAttribute("Name"));
						if (lclChildName != null) {
							String lclComparatorName = StringUtils.trimToNull(lclC.getAttribute("Comparator"));
							if (lclComparatorName != null) {
								boolean lclInvert = Boolean.parseBoolean(lclC.getAttribute("Invert"));
								lclCriteria.add(new ChildCriterion(lclChildName, lclComparatorName, lclInvert));
							} else {
								System.out.println("Required Comparator attribute for Child child for Comparator on " + lclMC + " is missing.");
								lclGood = false;
							}
						} else {
							System.out.println("Required Name attribute for Child child for Comparator on " + lclMC + " is missing.");
							lclGood = false;
						}
					} else if ("DelegatedMethod".equals(lclC.getNodeName())) {
						String lclMethodName = StringUtils.trimToNull(lclC.getAttribute("Name"));
						if (lclMethodName != null) {
							MethodDelegation lclFoundMD = null;
							for (MethodDelegation lclMD : lclMC.getMethodDelegations()) {
								if (lclMD.getMethodName().equals(lclMethodName) && lclMD.getReturnType() != Void.class) {
									// Names match and it returns something, but we also need a no-parameter method
									if (lclMD.getParameters() == null || lclMD.getParameters().length == 0) {
										lclFoundMD = lclMD;
									}
								}
							}
							if (lclFoundMD == null) {
								System.out.println("Could not find delegated method named '" + lclMethodName + "' on " + lclMC + " for Comparator");
								lclGood = false;
							} else {
								boolean lclInvert = Boolean.parseBoolean(lclC.getAttribute("Invert"));
								lclCriteria.add(new DelegatedMethodCriterion(lclFoundMD, lclInvert));
							}
						} else {
							System.out.println("Required Name method for DelegatedMethod child for Comparator on " + lclMC + " is missing.");
							lclGood = false;
						}
					} else {
						System.out.println("Unknown child element \"" + lclC.getNodeName() + "\" when building Comparator.");
					}
				}
			}
			
			if (lclGood) {
				lclMC.complain(MessageLevel.Info, "Adding Comparator " + lclName + " for " + lclMC + ".");
				lclMC.getComparatorSpecifications().add(new ComparatorSpecification(lclName, lclCriteria));
			}
		}
		return;
	}
}
