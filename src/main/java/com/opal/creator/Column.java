package com.opal.creator;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Node;

import com.siliconage.util.Trinary;

public class Column extends OpalXMLElement {
	
	public Column(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) throws Exception {
		MappedClass lclMC = ((Mapping) getParent()).getMappedClass();
		
		String lclName = getRequiredAttributeValue("Name");
		String lclFieldName = getAttributeValue("FieldName");
		ClassMember lclCM = lclMC.getClassMemberByColumnName(lclName);
		
		if (lclCM == null) {
			lclMC.complain(MessageLevel.Warning, "Could not find column \"" + lclName + "\" in " + lclMC + "; ignoring potential configuration in that element.");
			return;
		}
		
		if (lclFieldName != null) {
			lclMC.complain(MessageLevel.Info, "Using manually specified base field name \""  + lclFieldName + " for " + lclName + ".");
			lclCM.setBaseMemberName(lclFieldName);
		}
		
		String lclTypeName = getAttributeValue("Type");
		if (lclTypeName != null) {
			lclCM.suggestType(lclTypeName);
		}
		
		String lclTypeParameter = getAttributeValue("TypeParameter");
		if (lclTypeParameter != null) {
			lclCM.setTypeParameter(lclTypeParameter);
		}
		
		String lclComparatorString = getAttributeValue("Comparator");
		if (lclComparatorString != null) {
			lclCM.setComparator("True".equalsIgnoreCase(lclComparatorString));
		}
		
		/* String lclAccessorMethodName = getAttributeValue("Accessor");
		if (lclAccessorMethodName != null) {
			lclCM.setAccessorMethodName(lclAccessorMethodName);
		} */
		
		String lclInverseString = getAttributeValue("Inverse");
		if (lclInverseString != null) {
			lclCM.setInverseAccessor(Trinary.fromStringCaseInsensitive(lclInverseString));
		} else {
			lclCM.setInverseAccessor(Trinary.UNKNOWN);
		}
		
		String lclInverseAccessorMethodName = getAttributeValue("InverseAccessor");
		if (lclInverseAccessorMethodName != null) {
			lclCM.setInverseAccessorMethodName(lclInverseAccessorMethodName);
			if (lclCM.hasInverseAccessor() == Trinary.UNKNOWN) {
				lclCM.setInverseAccessor(Trinary.TRUE);
			}
		}
		
		if (lclCM.hasInverseAccessor() == Trinary.TRUE) {
			if (lclCM.getMemberType() != Boolean.class && lclCM.getMemberType() != Trinary.class) {
				lclMC.complain(MessageLevel.Warning, "Class member " + lclName + " is being given an inverse accessor, but its type is neither Boolean nor Trinary.");
			}
		}
		
		String lclPublicString = getAttributeValue("Public");
		if (lclPublicString != null) {
			lclCM.setPublic(Trinary.fromStringCaseInsensitive(lclPublicString));
		} else {
			lclCM.setPublic(Trinary.UNKNOWN);
		}
		
		/* TODO: Default to creating a comparator as well; make sure column type is 
		 * Comparable; allow provision of a Comparator if it's not; allow for multiple
		 * columns. */
		
		String lclSequenceString = getAttributeValue("Ordering");
		if ("True".equalsIgnoreCase(lclSequenceString)) {
			lclCM.setOrdering(true);
		}
		
		String lclMappedString = getAttributeValue("Mapped");
		if ("False".equalsIgnoreCase(lclMappedString)) {
			lclMC.complain(MessageLevel.Info, "Column " + lclName + " will not be mapped.");
			lclCM.setMapped(false);
		} else if ("True".equalsIgnoreCase(lclMappedString)) {
			lclCM.setMapped(true);
//		} else {
//			boolean lclIsListed = argContext.getUnmappedColumnNameList().contains(lclName);
//			System.out.println("lclIsListed is " + lclIsListed + " for \"" + lclName + "\"");
//			if (lclIsListed) {
//				lclCM.setMapped(false);
//				lclMC.complain(MessageLevel.Info, "Column " + lclName + " matches one list in the <Unmapped> element, so it will not be mapped.");
//			}
		}
		
		String lclUpdatableString = getAttributeValue("Updatable");
		if ("False".equalsIgnoreCase(lclUpdatableString)) {
			lclCM.setUpdatable(false);
		}
		
		String lclDeprecatedString = getAttributeValue("Deprecated");
		if ("True".equalsIgnoreCase(lclDeprecatedString)) {
			lclCM.setDeprecated(true);
		}
		
		String lclNullableString = getAttributeValue("Nullable");
		if ("True".equalsIgnoreCase(lclNullableString)) {
			lclCM.setNullAllowed(true);
		} else if ("False".equalsIgnoreCase(lclNullableString)) {
			lclCM.setNullAllowed(false);
		}
		
		String lclComputedString = getAttributeValue("Computed");
		if (lclComputedString == null) {
			/* Nothing */
		} else if ("No".equalsIgnoreCase(lclComputedString)) {
			/* Nothing. */
		} else if ("Expression".equalsIgnoreCase(lclComputedString)) {
			lclCM.setComputedType(ClassMember.COMPUTED_EXPRESSION);
			lclCM.setCached(true);
			String lclRawComputedExpressionText = getSingleChildContent("Expression");
			Validate.notNull(lclRawComputedExpressionText, "Expression child element missing for column that is to be computed using an expression.");
//			lclComputedText = lclComputedText.replace("%this%", "getUserFacing()"); // THINK: Does this need to be replicated in handleComputedColumns?
			lclMC.complain(MessageLevel.Info, "Raw expression is ["+ lclRawComputedExpressionText + "] for " + lclCM.getMemberName());
			lclCM.setRawComputedExpressionText(lclRawComputedExpressionText);
		} else if ("Method".equalsIgnoreCase(lclComputedString)) {
			lclCM.setComputedType(ClassMember.COMPUTED_METHOD);
			lclCM.setCached(true);
			String lclComputedText = getSingleChildContent("Method");
			Validate.notNull(lclComputedText);
//			lclComputedText = lclComputedText.replace("%this%", "getUserFacing()"); // THINK: Does this need to be replicated in handleComputedColumns?
			lclCM.setComputedMethod(lclComputedText);
			/* TODO: Reformat the method; should that be done here? */
		} else {
			throw new IllegalStateException("Illegal value \"" + lclComputedString + "\" for Computed attribute.");
		}
		return;
	}
	
	@Override
	protected boolean descend() {
		return false;
	}
}
