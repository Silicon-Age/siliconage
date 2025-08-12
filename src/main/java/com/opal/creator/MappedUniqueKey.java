package com.opal.creator;

import java.util.Iterator;
import java.util.ArrayList;

import org.apache.commons.lang3.Validate;

import com.opal.creator.database.Index;
import com.opal.creator.database.PrimaryKey;

public class MappedUniqueKey {
	private final MappedClass myMappedClass;
	private final Index myIndex;
	private boolean myPrimaryKey;
//	private boolean myUniqueStringKey;
	private final UniqueKeyType mySpecifiedType;
	
	private final ArrayList<ClassMember> myClassMembers = new ArrayList<>();
	
	public MappedUniqueKey(MappedClass argMC, Index argIndex) {
		this(argMC, argIndex, false, UniqueKeyType.DEFAULT);
	}
	
	public MappedUniqueKey(MappedClass argMC, Index argIndex, boolean argPrimaryKey) {
		this(argMC, argIndex, argPrimaryKey, UniqueKeyType.DEFAULT);
	}

	public MappedUniqueKey(MappedClass argMC, Index argIndex, boolean argPrimaryKey, UniqueKeyType argSpecifiedType) {
		super();
		myMappedClass = argMC;
		myIndex = argIndex;
		mySpecifiedType = Validate.notNull(argSpecifiedType);
//		setUniqueStringKey(argUniqueStringKey);
		setPrimaryKey(argPrimaryKey);
		
		resolveColumnNames();
	}
	
	public MappedUniqueKey(MappedClass argMC, PrimaryKey argPK) {
		this(argMC,
			new Index(
				argMC.getTableName(),
				"PK_INDEX",
				true,
				null // Not a filtered index
				),
			true,
			UniqueKeyType.DEFAULT // But we know in this case that DEFAULT will mean UNIQUE because the Index is not filtered
		);
		
		this.getIndex().getColumnNames().addAll(argPK.getColumnNames());
		
		resolveColumnNames(); /* A second time */
		
	}
	
	public UniqueKeyType getSpecifiedType() {
		return mySpecifiedType;
	}
	
	public UniqueKeyType getType() {
		UniqueKeyType lclT = getSpecifiedType();
		if (lclT == UniqueKeyType.DEFAULT) {
			return getIndex().determineDefaultType();
		} else {
			return lclT;
		}
	}
	
	protected void resolveColumnNames() {
//		System.out.println("Resolving column names for " + this);
		Iterator<String> lclI = getIndex().getColumnNames().iterator();
		while (lclI.hasNext()) {
			String lclIndexColumnName = lclI.next();
			ClassMember lclCM = getMappedClass().getClassMemberByColumnName(lclIndexColumnName);
			Validate.notNull(lclCM, "Index uses column \"" + lclIndexColumnName + "\" which is not associated with a ClassMember");
			addClassMember(lclCM);
//			System.out.println("For " + this + ", " + lclIndexColumnName + " resolves to " + lclCM);
		}
	}
	
	public void addClassMember(ClassMember argCM) {
		getClassMembers().add(argCM);
	}
	
	public Iterator<ClassMember> createClassMemberIterator() {
		return getClassMembers().iterator();
	}
	
	public ArrayList<ClassMember> getClassMembers() {
		return myClassMembers;
	}
	
	public Index getIndex() {
		return myIndex;
	}
	
	public MappedClass getMappedClass() {
		return myMappedClass;
	}
	
	public String getOpalKeyClassName(/* String argTypeName */) {
		StringBuilder lclSB = new StringBuilder(32);
		
		/* No longer necessary now that the keys are nested classes of the OpalFactory
		rather than the Opal itself. */
		
		/*
		if (argTypeName != null) {
			lclSB.append(argTypeName);
			lclSB.append('.');
		}
		*/
		
		Iterator<ClassMember> lclJ = createClassMemberIterator();
		while (lclJ.hasNext()) {
			lclSB.append(lclJ.next().getBaseMemberName());
		}
		lclSB.append("OpalKey");
		
		return lclSB.toString();
	}
	
	public boolean isPrimaryKey() {
		return myPrimaryKey;
	}
	
	public void setPrimaryKey(boolean argPrimaryKey) {
		myPrimaryKey = argPrimaryKey;
	}
	
	public boolean isUniqueStringKey() {
		return getIndex().isUniqueStringKey();
	}
	
//	public void setUniqueStringKey(boolean argUniqueStringKey) {
//		myUniqueStringKey = argUniqueStringKey;
//	}
	
	public int sizeClassMember() {
		return getClassMembers().size();
	}
	
	@Override
	public String toString() {
		return String.valueOf(getMappedClass()) + ':' + String.valueOf(getIndex().getColumnNames());
	}
	
	public boolean couldHaveNullComponent() {
		Iterator<ClassMember> lclI = createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (lclCM.isNullAllowed()) {
				return true;
			}
		}
		return false;
	}
		
	public String generateNotNullJavaCondition(String argLocalVariable) {
		Validate.notNull(argLocalVariable);
		StringBuilder lclSB = new StringBuilder(128);
		boolean lclFirst = true;
		Iterator<ClassMember> lclI = createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (lclCM.isNullAllowed()) {
				if (lclFirst) {
					lclFirst = false;
				} else {
					lclSB.append(" && ");
				}
				lclSB.append(argLocalVariable);
				lclSB.append('[');
				lclSB.append(lclCM.getFieldIndex());
				lclSB.append("] != null");
			}
		}
		if (lclFirst) {
			lclSB.append("true");
		}
		
		return lclSB.toString();
	}
	
	public String generateOpalKeyConstructorArguments(String argLocalVariable) {
		Validate.notNull(argLocalVariable);
		StringBuilder lclSB = new StringBuilder(128);
		Iterator<ClassMember> lclI = createClassMemberIterator();
		boolean lclFirst = true;
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (!lclFirst) {
				lclSB.append(", ");
			} else {
				lclFirst = false;
			}
			lclSB.append('(');
			lclSB.append(lclCM.getMemberParameterizedTypeName(false)); // false == wildcard, if generic
			lclSB.append(") ");
			lclSB.append(argLocalVariable);
			lclSB.append('[');
			lclSB.append(lclCM.getFieldIndex());
			lclSB.append(']');
		}
		return lclSB.toString();
	}
	
	public String generateOpalKeyConstructorCall(String argLocalVariable) {
		Validate.notNull(argLocalVariable);
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append("new ");
		lclSB.append(getOpalKeyClassName());
		lclSB.append('(');
		lclSB.append(generateOpalKeyConstructorArguments(argLocalVariable));
		lclSB.append(')');
		return lclSB.toString();
	}
	
	/* Don't call unless argNewArray already doesn't represent a null value for this key */
	public String generateKeyEqualityCondition(String argOldArray, String argNewArray) {
		Validate.notNull(argOldArray);
		Validate.notNull(argNewArray);
		StringBuilder lclSB = new StringBuilder(128);
		boolean lclFirst = true;
		Iterator<ClassMember> lclI = createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(" && ");
			}
			if (lclCM.isNullAllowed()) {
				lclSB.append("((");
				lclSB.append(argOldArray);
				lclSB.append('[');
				lclSB.append(lclCM.getFieldIndex());
				lclSB.append("] != null) ? (");
			}
			lclSB.append(argOldArray);
			lclSB.append('[');
			lclSB.append(lclCM.getFieldIndex());
			lclSB.append("].equals(");
			lclSB.append(argNewArray);
			lclSB.append('[');
			lclSB.append(lclCM.getFieldIndex());
			lclSB.append("])");
			if (lclCM.isNullAllowed()) {
				lclSB.append(") : (");
				lclSB.append(argNewArray);
				lclSB.append('[');
				lclSB.append(lclCM.getFieldIndex());
				lclSB.append("] == null))");
			}
		}
		return lclSB.toString();
	}
	
	public String generateOpalFactoryMethodDefinition(boolean argConcrete) {
		StringBuilder lclSB = new StringBuilder(64);
		lclSB.append("public ");
		lclSB.append(getMappedClass().getOpalClassName());
		lclSB.append(' ');
		lclSB.append(generateOpalFactoryMethodName(argConcrete));
		lclSB.append('(');
		lclSB.append(generateOpalFactoryMethodArguments());
		lclSB.append(") throws PersistenceException;");
		
		return lclSB.toString();
	}
	
	public String generateOpalFactoryMethodName(boolean argConcrete) {
		StringBuilder lclSB = new StringBuilder(32);
		lclSB.append("for");
		Iterator<ClassMember> lclI;
		
		lclI = createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			lclSB.append(lclCM.getBaseMemberName());
		}
		if (argConcrete) {
			lclSB.append("Concrete");
		}
		return lclSB.toString();
	}
	
	public String generateFactoryMethodName() {
		return generateOpalFactoryMethodName(false);
	}
	
	public String generateOpalFactoryMethodArguments() {
		StringBuilder lclSB = new StringBuilder(32);
		boolean lclFirst = true;
		Iterator<ClassMember> lclI = createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(", ");
			}
			lclSB.append(lclCM.getMemberParameterizedTypeName() + ' ' + lclCM.getObjectMutatorArgumentName());
		}
		return lclSB.toString();
	}
	
	public String generateFactoryMethodArguments() {
		return generateOpalFactoryMethodArguments();
	}
}
