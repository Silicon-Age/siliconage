package com.opal.creator;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.util.Trinary;

import com.opal.OpalUtility;
import com.opal.annotation.Length;
import com.opal.annotation.Nullability;
import com.opal.annotation.Updatability;
import com.opal.creator.database.ReferentialAction;
import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.DefaultValue;

public class ClassMember {
	
	private final DatabaseColumn myDatabaseColumn;
	private final Class<?> myMemberType;
	private String myTypeParameter;

	private Class<?> mySpecifiedType;
	
	private int myFieldIndex;
	private String myBaseMemberName;
	private String myBaseAccessorName;
	private boolean myComparator = false;
	private boolean myInverted = false;
	private boolean myOrdering = false;
	private boolean myMapped = true;
	private boolean myUpdatable = true;

	private int myComputedType = COMPUTED_NO;
	private boolean myCached = false;
	private String myRawComputedExpressionText;
	private String mySubstitutedComputedExpressionText;
	private String myComputedMethod;
	// Other ClassMembers that depends on this ClassMember.
	private final List<ClassMember> myDependents = new ArrayList<>();

	private boolean myDeprecated = false;
	private Trinary myPublic = Trinary.UNKNOWN;
	// private String myAccessorMethodName; // null -> use default
	private Trinary myInverseAccessor = Trinary.UNKNOWN;
	private String myInverseAccessorMethodName;
	private Trinary myNullAllowed = Trinary.UNKNOWN;

	private String myValidationMethodName;
	private String myValidationMethodClassName;
	
	public static final int COMPUTED_NO = 0;
	public static final int COMPUTED_EXPRESSION = 1;
	public static final int COMPUTED_METHOD = 2;
	
	public ClassMember(DatabaseColumn argDC, Class<?> argClass) {
		super();
		Validate.notNull(argDC);
		Validate.notNull(argClass);
		
		myDatabaseColumn = argDC;
		myMemberType = argClass;
		
		return;
	}
	
	private void constructBaseMemberName(String argName) {
		String lclDatabaseColumnName = argName;
		if (getMemberType() == Boolean.class || getMemberType() == Trinary.class) {
			if (OpalUtility.doesNameSuggestBoolean(lclDatabaseColumnName)) {
				myBaseMemberName = OpalUtility.convertUnderscoreIdentifierToJavaIdentifier(
					lclDatabaseColumnName.substring(
						lclDatabaseColumnName.indexOf('_')+1
					)
				);
				myBaseAccessorName = OpalUtility.convertUnderscoreIdentifierToJavaIdentifier(lclDatabaseColumnName, false);
				// TODO: "IS_" will cause a crash
			} else {
				myBaseMemberName = OpalUtility.convertUnderscoreIdentifierToJavaIdentifier(lclDatabaseColumnName);
				myBaseAccessorName = "is" + myBaseMemberName;
			}
		} else {
			myBaseMemberName = OpalUtility.convertUnderscoreIdentifierToJavaIdentifier(lclDatabaseColumnName);
			myBaseAccessorName = "get" + myBaseMemberName;
		}
	}
	
	public String generateFieldDefinition() {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append("\tprivate ");
		lclSB.append(getMemberType().getName());
		lclSB.append(' ');
		lclSB.append(getMemberName());
		lclSB.append(';');
		lclSB.append('\n');
		return lclSB.toString();
	}
	
	private String getBaseAccessorName() {
		/* if (myBaseAccessorName == null) {
			constructBaseAccessorName();
		}
		return myBaseAccessorName; */
		if (myBaseMemberName == null) {
			constructBaseMemberName(getDatabaseColumn().getName());
		}
		return myBaseAccessorName;

	}
	
	/* private void constructBaseAccessorName() {
		if (hasCustomAccessorMethodName()) {
			myBaseAccessorName = getAccessorMethodName(); 
		} else {
			constructDefaultBaseAccessorName();
		}
		return;
	} */
	
	public String getBaseMemberName() {
		if (myBaseMemberName == null) {
			constructBaseMemberName(getDatabaseColumn().getName());
		}
		return myBaseMemberName;
	}
	
	private String getBaseMutatorArgumentName() {
		return "arg" + getBaseMemberName();
	}
	
	private String getBaseMutatorName() {
		return "set" + getBaseMemberName();
	}
	
	public DatabaseColumn getDatabaseColumn() {
		return myDatabaseColumn;
	}
	
	public String getMemberName() {
		return "my" + getBaseMemberName();
	}
	
	public Class<?> getMemberType() {
		if (getSpecifiedType() != null) {
			return getSpecifiedType();
		} else {
			return myMemberType;
		}
	}
	
	public String getMemberParameterizedTypeName() {
		return getMemberParameterizedTypeName(true);
	}
	
	/* FIXME: This sort-of duplicates OpalUtility.generateTypeName(argClass, argGeneric), but not quite.
	 * FIXME: This should probably be made recursive work with things like arrays of parameterized types
	 * (not that Opal currently supports such things).
	 */
	
	public String getMemberParameterizedTypeName(boolean argWildcard) {
		Class<?> lclT = getMemberType();
		if (hasTypeParameter()) {
			return lclT.getName() + '<' + (argWildcard ? getTypeParameter() : "?") + '>';
		} else if (lclT.isArray()) {
			return lclT.getComponentType().getName() + "[]";
		} else {
			return getMemberType().getName();
		}
	}
	
	public String getNewMemberName() {
		return "myNew" + getBaseMemberName();
	}
	
	public String getObjectAccessorName() {
		/* If it's not a primitive type, return the BaseAccessorName */
		if (ClassUtils.wrapperToPrimitive(getMemberType()) == null) {
			return getBaseAccessorName();
		} else {
			return getBaseAccessorName() + "AsObject";
		}
	}
	
	public String getObjectMutatorArgumentName() {
		return getBaseMutatorArgumentName();
	}
	
	public String getObjectMutatorName() {
		return getBaseMutatorName();
	}
	
	public String getOldMemberName() {
		return "myOld" + getBaseMemberName();
	}
	
	public String getPrimitiveAccessorName() {
//		if (TypeUtility.getPrimitiveTypeOrNull(getMemberType()) == null) {
//			return getBaseAccessorName(); /* This originally threw an Exception, but that caused problems. */
//		} else {
			return getBaseAccessorName();
//		}
	}
	
	public String getPrimitiveMutatorArgumentName() {
		return getBaseMutatorArgumentName();
	}
	
	public String getPrimitiveMutatorName() {
		return getBaseMutatorName();
	}
	
	public String getResultSetAccessor() {
		Class<?> lclClass = getMemberType();
		if (lclClass == Integer.class) {
			return "getInt";
		} else if (lclClass == Float.class) {
			return "getFloat";
		} else if (lclClass == Double.class) {
			return "getDouble";
		} else if (lclClass == Long.class) {
			return "getLong";
		} else if (lclClass == Short.class) {
			return "getShort";
		} else if (lclClass == Boolean.class) {
			return "getBoolean";
		} else if (lclClass == Byte.class) {
			return "getByte";
		} else if (lclClass == Character.class) {
			return "getChar";
		} else if (lclClass == String.class) {
			return "getString";
		} else if (lclClass == java.sql.Date.class) {
			return "getDate";
		} else {
			throw new IllegalStateException("Cannot provide name of ResultSet accessor method for " + lclClass);
		}
	}
	
	public int getFieldIndex() {
		return myFieldIndex;
	}
	
	public String getSKMInserter() {
		Class<?> lclClass = getMemberType();
		if (lclClass == int.class) {
			return "putInt";
		} else if (lclClass == float.class) {
			return "putFloat";
		} else if (lclClass == double.class) {
			return "putDouble";
		} else if (lclClass == long.class) {
			return "putLong";
		} else if (lclClass == short.class) {
			return "putShort";
		} else if (lclClass == boolean.class) {
			return "putBoolean";
		} else if (lclClass == byte.class) {
			return "putByte";
		} else if (lclClass == char.class) {
			return "putChar";
		} else if (lclClass == String.class) {
			return "putString";
		} else if (lclClass == java.sql.Date.class) {
			return "putObject";
		} else {
			return "putObject";
		}
	}
	
	public Class<?> getWrapperType() {
		return ClassUtils.primitiveToWrapper(getMemberType());
	}
	
	protected void setBaseMemberName(String argBaseMemberName) {
		myBaseMemberName = argBaseMemberName;
		
		if (getMemberType() == Boolean.class) {
			myBaseAccessorName = "is" + myBaseMemberName;
		} else {
			myBaseAccessorName = "get" + myBaseMemberName;
		}
	}
	
	public void setFieldIndex(int argFieldIndex) {
		myFieldIndex = argFieldIndex;
	}
	
	public String getDefaultValueMemberName() {
		return "ourDefault" + getBaseMemberName();
	}
	
	public boolean hasDefault() {
		return getDefault() != null;
	}
	
	public DefaultValue getDefault() {
		return getDatabaseColumn().getDefault();
	}
	
	public boolean isComparator() {
		return myComparator;
	}
	
	public void setComparator(boolean argComparator) {
		myComparator = argComparator;
	}
	
	public boolean isInverted() {
		return myInverted;
	}
	
	public void setInverted(boolean argInverted) {
		myInverted = argInverted;
	}
		
	public boolean isOrdering() {
		return myOrdering;
	}
	
	public void setOrdering(boolean argOrdering) {
		myOrdering = argOrdering;
	}
	
	public boolean isMapped() {
		return myMapped;
	}
	
	public boolean isUpdatable() {
		return myUpdatable;
	}
	
	public void setMapped(boolean argMapped) {
		myMapped = argMapped;
	}
	
	public void setUpdatable(boolean argUpdatable) {
		myUpdatable = argUpdatable;
	}
	
	@Override
	public String toString() {
		return '[' + getBaseMemberName() + '/' + getDatabaseColumn().getName() + ']';
	}
	
	public boolean isNullAllowed() {
		return myNullAllowed.asBooleanPrimitive(getDatabaseColumn().isNullable());
	}
	
	public void setNullAllowed(boolean argNullable) {
		myNullAllowed = Trinary.valueOf(argNullable);
	}
	
	/* package */ boolean isUnique(MappedClass argMC) {
		return argMC.getMappedUniqueKeys().stream()
			.map(MappedUniqueKey::getClassMembers)
			.anyMatch(argCMs -> argCMs.size() == 1 && argCMs.contains(this));
	}

	public void suggestType(String argTypeName) {
		Validate.notNull(argTypeName);
		try {
			Class<?> lclClass = Class.forName(argTypeName);
			setSpecifiedType(lclClass);
			return;
		} catch (ClassNotFoundException lclE) {
			/* Nothing */
		}
		try {
			Class<?> lclClass = Class.forName("java.lang." + argTypeName);
			setSpecifiedType(lclClass);
			return;
		} catch (ClassNotFoundException lclE) {
			/* Nothing */
		}
		try {
			Class<?> lclClass = Class.forName("java.time." + argTypeName);
			setSpecifiedType(lclClass);
			return;
		} catch (ClassNotFoundException lclE) {
			/* Nothing */
		}
		if (argTypeName.equalsIgnoreCase("Boolean")) {
			setSpecifiedType(Boolean.class);
			return;
		} else if (argTypeName.equalsIgnoreCase("Trinary")) {
			setSpecifiedType(Trinary.class);
			return;
		} else if (argTypeName.equalsIgnoreCase("Int")) {
			setSpecifiedType(Integer.class);
			return;
		} else if (argTypeName.equalsIgnoreCase("Integer")) {
			setSpecifiedType(Integer.class);
			return;
		} else if (argTypeName.equalsIgnoreCase("Short")) {
			setSpecifiedType(Short.class);
			return;
		} else if (argTypeName.equalsIgnoreCase("Byte")) {
			setSpecifiedType(Byte.class);
			return;
		} else if (argTypeName.equalsIgnoreCase("Character")) {
			setSpecifiedType(Character.class);
			return;
		} else if (argTypeName.equalsIgnoreCase("Long")) {
			setSpecifiedType(Long.class);
			return;
		} else if (argTypeName.equalsIgnoreCase("Float")) {
			setSpecifiedType(Float.class);
			return;
		} else if (argTypeName.equalsIgnoreCase("Double")) {
			setSpecifiedType(Double.class);
			return;
		}
		throw new IllegalArgumentException("Could not resolve specified type \"" + argTypeName + "\".");
	}
	
	public Class<?> getSpecifiedType() {
		return mySpecifiedType;
	}
	
	public void setSpecifiedType(Class<?> argSpecifiedType) {
		mySpecifiedType = argSpecifiedType;
	}
	
	public int getComputedType() {
		return myComputedType;
	}
	
	public boolean isComputed() {
		return getComputedType() != COMPUTED_NO;
	}
	
	public boolean isCached() {
		return myCached;
	}
	
	public void setCached(boolean argCached) {
		myCached = argCached;
	}
	
	public boolean requiresStorage() {
		return isMapped() && (!isComputed() || isCached());
	}
	
	public void setComputedType(int argComputedType) {
		myComputedType = argComputedType;
	}
	
	public String getRawComputedExpressionText() {
		return myRawComputedExpressionText;
	}
	
	public void setRawComputedExpressionText(String argRawComputedExpressionText) {
		myRawComputedExpressionText = argRawComputedExpressionText;
	}
	
	public String getSubstitutedComputedExpressionText() {
		return mySubstitutedComputedExpressionText;
	}

	public void setSubstitutedExpressionText(String argSubstitutedExpressionText) {
		mySubstitutedComputedExpressionText = argSubstitutedExpressionText;
	}
	
	public String getComputedMethod() {
		return myComputedMethod;
	}
	
	public void setComputedMethod(String argComputedMethod) {
		myComputedMethod = argComputedMethod;
	}
	
	public List<ClassMember> getDependents() {
		return myDependents;
	}
	
	public String getValidationMethodName() {
		return myValidationMethodName;
	}
	
	public void setValidationMethodName(String argValidationMethodName) {
		myValidationMethodName = argValidationMethodName;
	}
	
	public String getValidationMethodClassName() {
		return myValidationMethodClassName;
	}
	
	public void setValidationMethodClassName(String argValidationMethodClassName) {
		myValidationMethodClassName = argValidationMethodClassName;
	}
	
	public boolean isDeprecated() {
		return myDeprecated;
	}
	
	public void setDeprecated(boolean argDeprecated) {
		myDeprecated = argDeprecated;
	}
	
	public String getTypeParameter() {
		return myTypeParameter;
	}
	
	public boolean hasTypeParameter() {
		return getTypeParameter() != null;
	}
	
	public void setTypeParameter(String argTypeParameter) {
		myTypeParameter = argTypeParameter;
	}
	
	public Trinary getPublic() {
		return myPublic;
	}
	
	public boolean isPublic(boolean argUnknownValue) {
		return myPublic.asBooleanPrimitive(argUnknownValue);
	}
	
	public void setPublic(Trinary argPublic) {
		Validate.notNull(argPublic);
		myPublic = argPublic;
	}
	
	/* public boolean hasCustomAccessorMethodName() {
		return getAccessorMethodName() != null;
	}
	
	public String getAccessorMethodName() {
		return myAccessorMethodName;
	}
	
	public void  setAccessorMethodName(String argMethodName) {
		myAccessorMethodName = argMethodName;
	} */
	
	public Trinary hasInverseAccessor() {
		return myInverseAccessor;
	}
	
	public boolean hasInverseAccessor(boolean argUnknownValue) {
		return myInverseAccessor.asBooleanPrimitive(argUnknownValue);
	}
	
	public void setInverseAccessor(Trinary argInverseAccessor) {
		Validate.notNull(argInverseAccessor);
		myInverseAccessor = argInverseAccessor;
	}
	
	public String getInverseAccessorMethodName() {
		return myInverseAccessorMethodName;
	}
	
	public void setInverseAccessorMethodName(String argInverseAccessorMethodName) {
		myInverseAccessorMethodName = argInverseAccessorMethodName;
	}
	
	public boolean appearsInUserFacing(MappedClass argMC) {
		if (getPublic() != Trinary.UNKNOWN) {
			return getPublic().asBooleanPrimitive(false); // Default value will never be used.
		}
		PolymorphicData lclPD = argMC.getPolymorphicData();
		if (lclPD == null) {
			return true;
		} else if (lclPD instanceof SubtablePolymorphicData) {
			return true;
		} else if (lclPD instanceof SingleTablePolymorphicData) {
			return false;
		} else {
			/* FIXME: Add complaint */
			return true;
		}
	}
	
	protected String generateDefaultInverseAccessorMethodName() {
		String lclB = Validate.notBlank(getBaseMemberName(), "Base member name is blank for " + this);
		String lclA = Validate.notBlank(getBaseAccessorName(), "Base accessor name is blank for " + this);
		int lclIndex = lclA.indexOf(lclB);
		if (lclIndex >= 0) {
			return lclA.substring(0, lclIndex) + "Not" + lclA.substring(lclIndex);
		} else {
			return "isNot" + lclB;
		}
	}
	
//	protected String generateDefaultInverseObjectAccessorMethodName() {
//		return generateInverseAccessorMethodNameOrDefault() + "AsObject";
//	}
	
	public String getInverseAccessorMethodNameOrDefault() {
		String lclS = getInverseAccessorMethodName();
		if (lclS != null) {
			return lclS;
		} else {
			return generateDefaultInverseAccessorMethodName();
		}
	}
	
	public String getInverseObjectAccessorMethodNameOrDefault() {
		return getInverseAccessorMethodNameOrDefault() + "AsObject";
	}
	
	public void outputAnnotations(PrintWriter argPW, MappedClass argMC) {
		Validate.notNull(argPW);
		Validate.notNull(argMC);
		
		argPW.println("\t@" + Updatability.class.getName() + "(updatable = " + (isUpdatable() && argMC.isUpdatable()) + ")");
		argPW.println("\t@" + Nullability.class.getName() + "(nullable = " + isNullAllowed() + ")");
		if (getMemberType() == String.class && getDatabaseColumn().getLength() > 0) {
			argPW.println("\t@" + Length.class.getName() + "(maximum = " + getDatabaseColumn().getLength() + "L)"); // L for long
		}
		
		if (hasDefault()) {
			DefaultValue lclDV = getDefault();
			String lclDVAnnotation = lclDV.generateAnnotation(getMemberType());
			if (StringUtils.isNotBlank(lclDVAnnotation)) {
				argPW.println('\t' + lclDVAnnotation);
			}
		}
		
		if (isDeprecated() && argMC.isDeprecated() == false) {
			argPW.println("\t@Deprecated");
		}
	}
	
	public boolean appearsInSourceOfCascadingUpdateForeignKey(MappedClass argMC) {
		if (argMC == null) {
			throw new IllegalArgumentException("argMC is null");
		}
		
		boolean lclR = argMC.getForeignKeysFrom().stream()
//				.filter(MappedForeignKey::isMapped)
				.filter(x -> x.getUpdateAction() == ReferentialAction.CASCADE)
				.map(MappedForeignKey::getSource)
				.flatMap(CompoundClassMember::stream)
				.anyMatch(this::equals);
		
		if (lclR) {
			System.out.println(this + " in " + argMC + " is part of a cascading update foreign key.");
		}
		
		return lclR;
	}
}
