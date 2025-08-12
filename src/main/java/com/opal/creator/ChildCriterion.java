package com.opal.creator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.Validate;

public class ChildCriterion extends ComparatorCriterion {
	private final String myChildName;
	private final String myComparatorName;
	
	public ChildCriterion(String argChildName, String argComparatorName) {
		this(argChildName, argComparatorName, false);
	}

	public ChildCriterion(String argChildName, String argComparatorName, boolean argInverted) {
		super(argInverted);
		
		Validate.notNull(argChildName);
		myChildName = argChildName;
		
		Validate.notNull(argComparatorName);
		myComparatorName = argComparatorName;
		
		return;
	}
	
	public String getChildName() {
		return myChildName;
	}
	
	public String getComparatorName() {
		return myComparatorName;
	}
	
	@Override
	public String getNameElement() {
		String lclCN = getComparatorName();
		int lclIndex = lclCN.indexOf("Comparator");
		if (lclIndex == -1) {
			return getChildName() + getComparatorName();
		} else {
			return getChildName() + lclCN.substring(0, lclIndex);
		}
	}
	
	/* TODO: Arguably these resolutions should be done in a batch after loading the foreign keys
	 * rather than as the Comparator is actually being built */
	protected MappedForeignKey determineMappedForeignKey(MappedClass argMC) {
		argMC.complain(MessageLevel.Debug, "Checking foreign keys from " + argMC); // NOPMD by Jonah Greenthal on 9/20/14 11:30 PM
		for (MappedForeignKey lclMFK : argMC.getForeignKeysFrom()) {
			argMC.complain(MessageLevel.Debug, "Checking against \"" + lclMFK.getRoleSourceFieldName() + "\" Source = " + lclMFK.getSourceMappedClass() + " Target = " + lclMFK.getTargetMappedClass());
			if (getChildName().equals(lclMFK.getRoleSourceFieldName())) {
				return lclMFK;
			}
		}
		argMC.complain(MessageLevel.Error, "Failed to find child \"" + getChildName() + "\" among the MappedForeignKeys."); // NOPMD by Jonah Greenthal on 9/20/14 11:31 PM
		return null;
	}
	
	@Override
	public String generateComparisonCode(MappedClass argMC) {
		MappedForeignKey lclMFK = determineMappedForeignKey(argMC);
		Validate.notNull(argMC);
		
		String lclCN = getComparatorName();
		MappedClass lclTarget = lclMFK.getTargetMappedClass();
		String lclFCN;
		String lclAccessorMethodName;
		if (lclCN.indexOf('.') == -1) {
			lclFCN = lclTarget.getInterfacePackageName() + '.' + lclTarget.getInterfaceClassName() + '.' + lclCN;
			lclAccessorMethodName = "getInstance";
		} else {
			lclFCN = lclCN;
			try {
				Class<?> lclC = Class.forName(lclFCN);
				Method lclAccessor = null;
				for (Method lclM : lclC.getMethods()) {
					if ((lclM.getModifiers() & Modifier.STATIC) == 0) {
						continue;
					}
					if ((lclM.getModifiers() & Modifier.PUBLIC) == 0) {
						continue;
					}
					if (lclM.getReturnType() != lclC) {
						continue;
					}
					if (lclM.getParameterTypes().length > 0) {
						continue;
					}
					lclAccessor = lclM;
					break;
				}
				if (lclAccessor == null) {
					lclAccessorMethodName = "getInstance";
				} else {
					lclAccessorMethodName = lclAccessor.getName();
				}
			} catch (ClassNotFoundException lclE) {
				lclAccessorMethodName = "getInstance";
			}
		}
		
		String lclFirst = isInverted() ? "argSecond" : "argFirst";
		String lclSecond = isInverted() ? "argFirst" : "argSecond";
		
		// TODO: Detect whether it is a nullSafeCompare (?)
		return lclFCN + '.' + lclAccessorMethodName + "().compare(" + lclFirst + ".get" + lclMFK.getRoleSourceFieldName() + "(), " + lclSecond + ".get" + lclMFK.getRoleSourceFieldName() + "())";
	}
}
