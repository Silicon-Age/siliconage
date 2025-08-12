package com.opal.creator;

import org.apache.commons.lang3.Validate;

public class SimpleFieldCriterion extends ComparatorCriterion {
	private final ClassMember myClassMember;
	
	public SimpleFieldCriterion(ClassMember argCM) {
		this(argCM, false);
	}
	
	public SimpleFieldCriterion(ClassMember argCM, boolean argInverted) {
		super(argInverted);
		
		Validate.notNull(argCM);
		myClassMember = argCM;
		
		return;
	}
	
	public ClassMember getClassMember() {
		return myClassMember;
	}
	
	@Override
	public String getNameElement() {
		return getClassMember().getBaseMemberName(); // TODO:  Add something if it's inverted?
	}
	
	@Override
	public String generateComparisonCode(MappedClass argMC) {
		ClassMember lclCM = getClassMember();
		
		String lclFirst = isInverted() ? "argSecond" : "argFirst";
		String lclSecond = isInverted() ? "argFirst" : "argSecond";
		
		if (lclCM.isNullAllowed()) {
			String lclMethodName = lclCM.getMemberType() == String.class ? "nullSafeCompareIgnoreCase" : "nullSafeCompare";
			return lclMethodName + "("
					+ lclFirst + '.' + lclCM.getObjectAccessorName() + "()"
					+ ", " 
					+ lclSecond + '.' + lclCM.getObjectAccessorName() + "()"
					+ ")";
		} else {
			if (lclCM.getMemberType() == Integer.class) {
				return "Integer.compare(" + lclFirst + '.' + lclCM.getPrimitiveAccessorName() + "(), " + lclSecond + '.' + lclCM.getPrimitiveAccessorName() + "())";
			} else {
				String lclMethodName = lclCM.getMemberType() == String.class ? "compareToIgnoreCase" : "compareTo";
				
				return lclFirst + '.' + lclCM.getObjectAccessorName() + "()." + lclMethodName + "(" + lclSecond + '.' + lclCM.getObjectAccessorName() + "())";
			}
		}
	}
}
