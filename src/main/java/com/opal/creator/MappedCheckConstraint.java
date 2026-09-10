package com.opal.creator;

import java.util.Objects;

import org.apache.commons.lang3.ClassUtils;

import com.opal.creator.database.CheckConstraint;
import com.opal.creator.database.CheckConstraintParser;
import com.opal.creator.database.CheckConstraintParser.ParseResult;
//import com.opal.creator.database.CheckConstraintParser.Supported;

/* This class represents a database-level CHECK constraint that has been determined to be relevant to the fields
 * that are actually mapped and can be paired with a ClassMember to assist with Java code generation.
 */
/* package */ class MappedCheckConstraint {
	private final ClassMember myClassMember;
	private final CheckConstraint myCheckConstraint;
	private final ParseResult myParseResult;
	
	/* package */ MappedCheckConstraint(ClassMember cm, CheckConstraint cc) {
		super();
		
		myClassMember = Objects.requireNonNull(cm);
		myCheckConstraint = Objects.requireNonNull(cc);
		Objects.requireNonNull(cm);
		
		/* If the member's type is a wrapper for a primitive and null is not allowed, then we want to parse
		 * the CHECK expression as if NULLs are not allowed (thereby producing a somewhat cleaner expression).
		 */
		Class<?> type = cm.getMemberType();
		Class<?> relevantType;
		if (ClassUtils.isPrimitiveWrapper(type) && (cm.isNullAllowed() == false)) {
			relevantType = ClassUtils.wrapperToPrimitive(type);
		} else {
			relevantType = type;
		}

		myParseResult = CheckConstraintParser.parseColumnCheck(
				relevantType,
				cm.isNullAllowed(),
				cm.getDatabaseColumn().getName(),
				cc.getSQLDefinition()
				);

// FIXME: Do some actual logging for these.
//		if (myParseResult instanceof Supported) {
//			System.out.println("Successfully parsed constraint " + cc.getSQLDefinition());
//		} else {
//			System.out.println("*** Could not parse constraint " + cc.getSQLDefinition() + " ***");
//		}
	}
	
	/* package */ ClassMember getClassMember() {
		return myClassMember;
	}
	
	/* package */ CheckConstraint getCheckConstraint() {
		return myCheckConstraint;
	}
	
	/* package */ ParseResult getParseResult() {
		return myParseResult;
	}
	
	/* package */ String getMethodName() {
		return "test" + getClassMember().getBaseMemberName() + "CheckConstraint";
	}
	
	/* package */ String getArgumentName() {
		return getClassMember().getObjectMutatorArgumentName();
	}	
}
