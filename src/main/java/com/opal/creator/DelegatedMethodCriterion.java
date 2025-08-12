package com.opal.creator;

import org.apache.commons.lang3.Validate;

public class DelegatedMethodCriterion extends ComparatorCriterion {
	private final MethodDelegation myMethodDelegation;
	
	public DelegatedMethodCriterion(MethodDelegation argMD) {
		this(argMD, false);
	}
	
	public DelegatedMethodCriterion(MethodDelegation argMD, boolean argInverted) {
		super(argInverted);
		
		Validate.notNull(argMD);
		myMethodDelegation = argMD;
	}
	
	public MethodDelegation getMethodDelegation() {
		return myMethodDelegation;
	}
	
	@Override
	public String getNameElement() {
		return getMethodDelegation().getMethodName(); // TODO:  Add something if it's inverted?
	}
	
	@Override
	public String generateComparisonCode(MappedClass argMC) {
		MethodDelegation lclMD = getMethodDelegation();
		
		String lclFirst = isInverted() ? "argSecond" : "argFirst";
		String lclSecond = isInverted() ? "argFirst" : " argSecond";
		
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Comparable<?>> lclComparable = (Class<? extends Comparable<?>>) lclMD.getReturnType();
			
			String lclMethodName = lclComparable == String.class ? "nullSafeCompareIgnoreCase" : "nullSafeCompare";
			return lclMethodName + "("
					+ lclFirst + '.' + lclMD.getMethodName() + "()"
					+ ", " 
					+ lclSecond + '.' + lclMD.getMethodName() + "()"
					+ ")";
		} catch (ClassCastException lclCCE) {
			throw new IllegalArgumentException("Return type of " + lclMD.getClassName() + "'s delegated method " + lclMD.getMethodName() + " is not Comparable");
		}
	}
}
