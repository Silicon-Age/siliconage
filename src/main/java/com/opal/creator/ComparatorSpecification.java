package com.opal.creator;

import java.util.List;
import org.apache.commons.lang3.Validate;

public class ComparatorSpecification {
	private final String myName;
	private final List<ComparatorCriterion> myCriteria;
	
	protected ComparatorSpecification(String argName, List<ComparatorCriterion> argCriteria) {
		super();
		
		myName = argName;
		
		Validate.notNull(argCriteria, "Null criteria for comparator " + argName);
		Validate.isTrue(argCriteria.size() > 0, "Empty criteria for comparator " + argName);
		myCriteria = argCriteria;
	}
	
	protected ComparatorSpecification(List<ComparatorCriterion> argCriteria) {
		this(null, argCriteria);
	}
	
	public String getName() {
		return myName;
	}
	
	public List<ComparatorCriterion> getCriteria() {
		return myCriteria;
	}
	
	public String generateClassName() {
		if (getName() != null) {
			return getName();
		} else {
			StringBuilder lclSB = new StringBuilder(128);
			for (ComparatorCriterion lclCC : getCriteria()) {
				lclSB.append(lclCC.getNameElement());
			}
			lclSB.append("Comparator");
			return lclSB.toString();
		}
	}
}
