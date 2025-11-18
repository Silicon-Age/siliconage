package com.opal.creator;

import java.io.File;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.util.Trinary;
import com.opal.annotation.RequiresActiveTransaction;
import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.EntityType;
import com.opal.creator.database.TableName;

public class MappedClass {
	public static final String USER_FACING_SUFFIX = "UserFacing";
	public static final String FACTORY_SUFFIX = "Factory";
	public static final String OPAL_SUFFIX = "Opal";
	public static final String IMPLEMENTATION_SUFFIX = "Impl";
	public static final String MONE_PREFIX = "Mone";
	
	public static final String MONE_SUBPACKAGE_NAME = "mone";
	
	private final TableName myTableName;
	private final String myApplicationPackage;
	private final String myPersistencePackage;
	private final String myCMAPackage;
	private final String myTypeName;
	private final String mySourceDirectory;
	private final String myAuthor;
	
	private final Trinary myView;
	private EntityType myTrueEntityType = EntityType.Unspecified;
	
	private final Trinary myCreatable;
	private final Trinary myUpdatable;
	private final boolean myAbstract;
	private final Trinary myEphemeral;
	
	private final boolean myHttpRequestFactory;
	
	private final Trinary myDefaultBackCollections;
	
	private final String myCollections;
	
	private Trinary myStaticBindings;
	
	private final boolean myDefaultFluentMutators;
	private final boolean myForceFluentMutators;
	
	// private final boolean mySampleCollections;
	
	private final boolean myDeprecated;
	private final MessageLevel myMessageLevel;
	
	private final List<ClassMember> myClassMembers = new ArrayList<>();
	private final List<ClassMember> myLargeClassMembers = new ArrayList<>();
	private final SortedSet<MappedUniqueKey> myMappedUniqueKeys = new TreeSet<>(Comparator.comparing(MappedUniqueKey::generateFactoryMethodName));
	private final List<MappedForeignKey> myForeignKeysFrom = new ArrayList<>();
	private final List<MappedForeignKey> myForeignKeysTo = new ArrayList<>();
	private final List<MethodDelegation> myMethodDelegations = new ArrayList<>();
	private final List<MultipleLookUpSpecification> myMultipleLookUpSpecifications = new ArrayList<>();
	
	private String myInterfaceClassName;
	private String myInterfacePackageName;
	
	private String myUserFacingClassName;
	private String myUserFacingPackageName;
	
	private String myOpalClassName;
	private String myOpalPackageName;
	
	private String myOpalFactoryInterfaceName;
	private String myOpalFactoryPackageName;
	
	private String myFactoryClassName;
	private String myFactoryPackageName;
	
	private String myImplementationClassName;
	private String myImplementationPackageName;
	
	private String myMoneOpalFactoryPackageName;
	private String myMoneOpalFactoryClassName;
	
	private boolean myGetAll;
	private boolean myDependent;
	private boolean myAssociation;
	private List<String> myStaticBindingList;
	
	private PolymorphicData myPolymorphicData;
	private boolean mySuperclassKeyResolved = false;
	private MappedForeignKey mySuperclassKey;
	private List<MappedClass> mySubclasses = new ArrayList<>();
	
	private Trinary myMone = Trinary.UNKNOWN;
	
	private String myPoolName;
	
	/* What this member really means is "potentially polymorphic."  If true, it means that we will try to identify super/sub
	 * table relationships in the database and create super/sub classes accordingly.  If false, we will not do that.  So
	 * tables that are not actually polymorphic may very well have this set to true:  We will try to find polymorphism inherent
	 * in the database structure, and fail.  
	 */
	private boolean myPolymorphic = true;
	
	private int myClassMemberCount = 0;
	
	private String mySpecifiedImplementationClassName;
	
	private final List<ComparatorSpecification> myComparatorSpecifications = new ArrayList<>();
	
	private final boolean myPotentiallyTree;
	private boolean myTree = false;
	private MappedForeignKey myTreeParentKey;
	
	private boolean mySoftReferences = true;
	
	public MappedClass(TableName argTableName,
			String argTypeName,
			String argApplicationPackage,
			String argPersistencePackage,
			String argCMAPackage,
			String argSourceDirectory,
			String argAuthor,
			Trinary argView,
			Trinary argCreatable,
			Trinary argUpdatable,
			boolean argAbstract,
			Trinary argEphemeral,
			boolean argPotentiallyTree,
			boolean argGenerateHttpRequestFactory,
			Trinary argDefaultBackCollections,
			String argCollections,
			boolean argDefaultFluentMutators,
			boolean argForceFluentMutators,
			// boolean argSampleCollections,
			boolean argDeprecated,
			MessageLevel argMessageLevel) {
		
		super();
		
		Validate.notNull(argTableName);
		myTableName = argTableName;
		
		Validate.notNull(argTypeName);
		myTypeName = argTypeName;
		
		// myPackageRoot = Validate.notNull(argPackageRoot);
		// myPackageRoot = "ZZZ"; /* Not used */
		
		Validate.notNull(argApplicationPackage);
		myApplicationPackage = argApplicationPackage;
		
		Validate.notNull(argPersistencePackage);
		myPersistencePackage = argPersistencePackage;
		
		Validate.notNull(argCMAPackage);
		myCMAPackage = argCMAPackage;
		
		Validate.notNull(argSourceDirectory);
		mySourceDirectory = argSourceDirectory;
		
		Validate.notNull(argAuthor);
		myAuthor = argAuthor;
		
		myView = argView;
		myCreatable = argCreatable;
		myUpdatable = argUpdatable;
		// myUpdater = argUpdater;
		myAbstract = argAbstract;
		myEphemeral = argEphemeral;
		myPotentiallyTree = argPotentiallyTree;
		
		myHttpRequestFactory = argGenerateHttpRequestFactory;
		
		myDefaultBackCollections = argDefaultBackCollections;
		
		myCollections = argCollections;
		
		myDefaultFluentMutators = argDefaultFluentMutators;
		myForceFluentMutators = argForceFluentMutators;
		
		// mySampleCollections = argSampleCollections;
		
		myDeprecated = argDeprecated;
		Validate.notNull(argMessageLevel);
		myMessageLevel = argMessageLevel;
		
		generateNames();
	}
	
	private List<ClassMember> getLargeClassMembers() {
		return myLargeClassMembers;
	}
	
	public void addClassMember(ClassMember argCM) {
		Validate.notNull(argCM);
		argCM.setFieldIndex(myClassMemberCount++);
		getClassMembers().add(argCM);
	}
	
	public void addLargeClassMember(ClassMember argCM) {
		Validate.notNull(argCM);
		argCM.setFieldIndex(myClassMemberCount++);
		getLargeClassMembers().add(argCM);
	}
	
	public void addForeignKey(MappedForeignKey argFK) {
		if (argFK != null) {
			getForeignKeysFrom().add(argFK);
		}
	}
	
	public void addMappedUniqueKey(MappedUniqueKey argMUK) {
		if (argMUK != null) {
			getMappedUniqueKeys().add(argMUK);
		}
	}
	
	public void addTargetForeignKey(MappedForeignKey argFK) {
		if (argFK != null) {
			getForeignKeysTo().add(argFK);
		}
	}

	protected boolean createSupplierInterface() { // THINK: Maybe we want to let the user manually request such an interface?
		return getForeignKeysTo().stream()
				.filter(x -> "Protected".equalsIgnoreCase(x.getSourceFieldAccess()) == false)
				.filter(MappedForeignKey::appearsInSourceUserFacing)
				.anyMatch(MappedForeignKey::createMatchesPredicate);
	}
	
	protected boolean createMatchesMethod() {
		return createSupplierInterface();
	}
	
	protected String getSupplierInterfaceName() {
		return getInterfaceClassName() + "Supplier";
	}
	
	protected String getSupplierAccessorName() {
		return "get" + getInterfaceClassName();
	}
	
	/* TODO: Move this elsewhere */
	
	public static void ensureDirectoryExists(File argFile) {
		if (!argFile.exists()) {
			if (argFile.mkdirs() == false) {
				throw new RuntimeException("Unable to create directories for " + argFile);
			}
		}
	}
	
	public Iterator<ClassMember> createClassMemberIterator() {
		return getClassMembers().iterator();
	}
	
//	public Iterator<ClassMember> createLargeClassMemberIterator() {
//		return getLargeClassMembers().iterator();
//	}
	
	public Iterator<MappedForeignKey> createForeignKeyIterator() {
		return getForeignKeysFrom().iterator();
	}
	
	protected ClassMember getOrderingMember() {
		Iterator<ClassMember> lclI = createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (lclCM.isOrdering()) {
				return lclCM;
			}
		}
		return null;
	}
	
	/* FIXME: This will need to be revisited in the case of tables that combines sub- and single-table polymorphism. */
	public boolean implementsPolymorphicCreator() {
		if (isCreatable() == false) {
			return false;
		}
		PolymorphicData lclPD = getPolymorphicData();
		if (lclPD == null) {
			return false;
		}
		if (lclPD instanceof SingleTablePolymorphicData) {
			return true;
		}
		return false;
	}
	
	/* FIXME: This will need to be revisited in the case of tables that combines sub- and single-table polymorphism. */
	public boolean implementsCreator() {
		if (isCreatable() == false) {
			return false;
		}
		MappedClass lclRootSuperclass = getRootSuperclass();
		PolymorphicData lclPD = lclRootSuperclass != null ? lclRootSuperclass.getPolymorphicData() : null;
		if (lclPD == null) {
			return true;
		}
		if (lclPD instanceof SubtablePolymorphicData) {
			if (isAbstract()) {
				return false;
			} else {
				return true;
			}
		}
		return false;
	}
	
	public Iterator<MappedForeignKey> createTargetForeignKeyIterator() {
		return getForeignKeysTo().iterator();
	}
	
	protected void generateNames() {
		String lclTypeName = getTypeName();
		
		myInterfaceClassName = lclTypeName;
		myInterfacePackageName = getApplicationPackage();
		
		myUserFacingClassName = lclTypeName + USER_FACING_SUFFIX;
		myUserFacingPackageName = getPersistencePackage();
		
		myFactoryClassName = myInterfaceClassName + FACTORY_SUFFIX;
		myFactoryPackageName = getApplicationPackage();
		
		myOpalClassName = myTypeName + OPAL_SUFFIX;
		myOpalPackageName = getPersistencePackage();
		
		myOpalFactoryInterfaceName = myOpalClassName + FACTORY_SUFFIX;
		myOpalFactoryPackageName = getPersistencePackage();
		
		myImplementationClassName = myInterfaceClassName + IMPLEMENTATION_SUFFIX;
		myImplementationPackageName = getPersistencePackage();
		
		myMoneOpalFactoryClassName = MONE_PREFIX + myOpalFactoryInterfaceName;
		myMoneOpalFactoryPackageName = getPersistencePackage() + '.' + MONE_SUBPACKAGE_NAME;
	}
	
	public String getImplementationClassName() {
		return myImplementationClassName;
	}
	
	public String getImplementationPackageName() {
		return myImplementationPackageName;
	}
	
	public String getFullyQualifiedImplementationClassName() {
		return getImplementationPackageName() + '.' + getImplementationClassName();
	}
	
	public ClassMember getClassMemberByColumnName(String argColumnName) {
		Validate.notNull(argColumnName);
		
		Iterator<ClassMember> lclI = createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (argColumnName.equals(lclCM.getDatabaseColumn().getName())) {
				return lclCM;
			}
		}
		
		return null;
	}
	
	public ClassMember getClassMemberByBaseName(String argMemberName) {
		Validate.notNull(argMemberName);
		
		Iterator<ClassMember> lclI = createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (argMemberName.equals(lclCM.getBaseMemberName())) {
				return lclCM;
			}
		}
		
		return null;
	}
	
	public int getClassMemberCount() {
		return getClassMembers().size();
	}
	
	protected List<ClassMember> getClassMembers() {
		return myClassMembers;
	}
	
	public String getFactoryPackageName() {
		return myFactoryPackageName;
	}
	
	public String getFactoryClassName() {
		return myFactoryClassName;
	}
	
	public String getFullyQualifiedFactoryClassName() {
		return getFactoryPackageName() + '.' + getFactoryClassName();
	}
	
	public int getForeignKeyCount() {
		return getForeignKeysFrom().size();
	}
	
	protected List<MappedForeignKey> getForeignKeysFrom() {
		return myForeignKeysFrom;
	}
	
	protected List<MappedForeignKey> getForeignKeysTo() {
		return myForeignKeysTo;
	}
	
	public String getInterfacePackageName() {
		return myInterfacePackageName;
	}
	
	public String getInterfaceClassName() {
		return myInterfaceClassName;
	}
	
	public String getFullyQualifiedInterfaceClassName() {
		return getInterfacePackageName() + '.' + getInterfaceClassName();
	}
	
	public String getUserFacingPackageName() {
		return myUserFacingPackageName;
	}
	
	public String getUserFacingClassName() {
		return myUserFacingClassName;
	}
	
	public String getFullyQualifiedUserFacingClassName() {
		return getUserFacingPackageName() + '.' + getUserFacingClassName();
	}
	
	public SortedSet<MappedUniqueKey> getMappedUniqueKeys() {
		return myMappedUniqueKeys;
	}
	
	public String getOpalPackageName() {
		return myOpalPackageName;
	}
	
	public String getOpalClassName() {
		return myOpalClassName;
	}
	
	public String getFullyQualifiedOpalClassName() {
		return getOpalPackageName() + '.' + getOpalClassName();
	}
	
	public String getOpalFactoryPackageName() {
		return myOpalFactoryPackageName;
	}
	
	public String getOpalFactoryInterfaceName() {
		return myOpalFactoryInterfaceName;
	}
	
	public String getMoneOpalFactoryPackageName() {
		return myMoneOpalFactoryPackageName;
	}
	
	protected String getMoneOpalFactoryClassName() {
		return myMoneOpalFactoryClassName;
	}
	
	protected String getFullyQualifiedMoneOpalFactoryClassName() {
		return getMoneOpalFactoryPackageName() + '.' + getMoneOpalFactoryClassName();
	}
	
	public String getFullyQualifiedOpalFactoryInterfaceName() {
		return getOpalFactoryPackageName() + '.' + getOpalFactoryInterfaceName();
	}
	
	public MappedUniqueKey getPrimaryKey() {
		return getMappedUniqueKeys().stream()
			.filter(MappedUniqueKey::isPrimaryKey)
			.findAny()
			.orElse(null);
	}
	
	public MappedUniqueKey getUniqueStringKey() {
		MappedUniqueKey lclUSK = getMappedUniqueKeys().stream()
			.filter(MappedUniqueKey::isUniqueStringKey)
			.findAny()
			.orElse(null);
		
		if (lclUSK != null) {
			return lclUSK;
		} else {
			return getPrimaryKey();
		}
	}
	
	protected String getSourceDirectory() {
		return mySourceDirectory;
	}
	
	public TableName getTableName() {
		return myTableName;
	}
	
	public int getTargetForeignKeyCount() {
		return getForeignKeysTo().size();
	}
	
	/* Does this class have at least one incoming foreign key that will generate a back collection?  Such a key will not
	 * generate a back collection if it actually represents a one-to-one relationship or it has been explicitly marked as
	 * not generating one (by giving it a Collection type of null).
	 */
	public boolean hasAtLeastOneBackCollection() {
		for (MappedForeignKey lclMFK : getForeignKeysTo()) {
			if (lclMFK.representsManyToOneRelationship()) {
				if (lclMFK.hasBackCollection() != false) {
					if (lclMFK.getSourceMappedClass().isEphemeral() == false) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean hasAtLeastOneUniqueKey() {
		return myMappedUniqueKeys != null && myMappedUniqueKeys.isEmpty() == false;
	}
	
	public boolean hasAtLeastOneReference() {
		if (getForeignKeysFrom().isEmpty() == false) {
			return true;
		}
		for (MappedForeignKey lclMFK : getForeignKeysTo()) {
			if (lclMFK.representsOneToOneRelationship() == true) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasAtLeastOneDeprecatedReference() {
		for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
			if (lclMFK.getTargetMappedClass().isDeprecated()) {
				return true;
			}
		}
		for (MappedForeignKey lclMFK : getForeignKeysTo()) {
			if (lclMFK.getSourceMappedClass().isDeprecated()) {
				if (lclMFK.representsOneToOneRelationship()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean hasAtLeastOneDeprecatedCollection() {
		for (MappedForeignKey lclMFK : getForeignKeysTo()) {
			if (lclMFK.getSourceMappedClass().isDeprecated()) {
				if (lclMFK.representsManyToOneRelationship()) {
					if (lclMFK.hasBackCollection()) { /* THINK: Does this need to check whether the source is ephemeral? */
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean hasAtLeastOneDeprecatedLink() {
		return hasAtLeastOneDeprecatedReference() || hasAtLeastOneDeprecatedCollection();
	}
	
	public boolean hasIntrinsicOrdering() {
		return getOrderingMember() != null;
	}
	
	/* Return the number of TargetForeignKeys that actually have collections
	 * associated with them.
	 */
	public int getActualTargetForeignKeyCount() {
		int lclCount = getTargetForeignKeyCount();
		if (lclCount == 0) {
			return 0;
		}
		Iterator<MappedForeignKey> lclI = createTargetForeignKeyIterator();
		while (lclI.hasNext()) {
			if (lclI.next().getForeignKey().getCollectionClass() == null) {
				--lclCount;
			}
		}
		return lclCount;
	}
	
	public String getTypeName() {
		return myTypeName;
	}
	
	public String getFullyQualifiedTypeName() {
		return getInterfacePackageName() + '.' + getTypeName();
	}
	
	public Trinary getView() {
		return myView;
	}
	
	public boolean isView() {
		return myView.asBooleanPrimitive(getTrueEntityType() == EntityType.View);
	}
	
	public boolean isTable() {
		return myView.not().asBooleanPrimitive(getTrueEntityType() == EntityType.Table);
	}
	
	public EntityType getTrueEntityType() {
		return myTrueEntityType;
	}
	
	public void setTrueEntityType(EntityType argET) {
		myTrueEntityType = Validate.notNull(argET);
	}
	
	public Trinary getCreatable() {
		return myCreatable;
	}
	
	public boolean isCreatable() {
		return myCreatable.asBooleanPrimitive(isTable());
	}
	
	public Trinary getUpdatable() {
		return myUpdatable;
	}
	
	public boolean isUpdatable() {
		return myUpdatable.asBooleanPrimitive(isTable());
	}
	
	public boolean isImmutableOpal() {
		return isUpdatableOpal() == false;
	}
	
	public boolean isUpdatableOpal() {
		return isCreatable() || isUpdatable();
	}
	
	public boolean getHttpRequestFactory() {
		return myHttpRequestFactory;
	}
	
	public boolean generateHttpRequestFactory() {
		return myHttpRequestFactory && (isEphemeral() == false);
	}
	
	public boolean generateFluentMutators() {
		return myForceFluentMutators || (myDefaultFluentMutators && getPolymorphicData() == null);
	}
	
	public String determineMutatorReturnType(String argCurrentClassName) {
		return generateFluentMutators() ? argCurrentClassName : "void";
	}
	
	public String getMutatorReturnValue() {
		return generateFluentMutators() ? "return this;" : "return;";
	}
	
	/* public boolean sampleCollections() {
		return mySampleCollections;
	} */
	
	public boolean isGetAll() {
		return myGetAll;
	}
	
	public List<String> getStaticBindingList() {
		return myStaticBindingList;
	}
	
	public void setStaticBindings(ArrayList<String> argStaticBindingList) {
		myStaticBindingList = argStaticBindingList;
	}
	
	public void setGetAll(boolean newGetAll) {
		myGetAll = newGetAll;
	}
	
	@Override
	public String toString() {
		return '[' + String.valueOf(getTableName()) + '/' + getOpalClassName() + ']';
	}
	
	public void determineUniqueForeignKeys() {
		for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
			if (lclMFK.getForeignKey().isOneToOne()) {
				lclMFK.setUnique(true);
			} else {
				for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
					if (lclMFK.getSource().hasIdenticalClassMembers(lclMUK)) {
						lclMFK.setUnique(true);
					}
				}
			}
		}
	}
	
	public void determineOneToOneForeignKeys() {
		for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
			complain(MessageLevel.Debug, "Determining one-to-oneness for " + lclMFK);
			/* Was this key explicitly marked in the configuration file as being a one-to-one relationship? */
			if (lclMFK.getForeignKey().isOneToOne()) {
				/* Yes.  That makes our job easy.  Perhaps at some point we should consider verifying the data
				 * to see if that is true.
				 */
				complain(MessageLevel.Debug, lclMFK + " was manually marked as being one-to-one");
				lclMFK.setRepresentsOneToOneRelationship(true);
				lclMFK.setRepresentsPolymorphism(false);
				lclMFK.getForeignKey().setCollectionClass(null); // Currently redundant
				lclMFK.setUnique(true); // Is this necessary? 
			} else {
				complain(MessageLevel.Debug, "It was not explicitly marked.  Is it unique?");
				/* It was not so marked.  Let's see if the UNIQUE indexes defined on the table suggest that it
				 * should be one-to-one.
				 */
				/* Are the components of this foreign key mandated to be unique by the existence of indices? */
				if (lclMFK.isUnique()) {
					/* Yes. */
					complain(MessageLevel.Debug, "Yes.  Is it the SuperclassKey?");
					if (lclMFK == getSuperclassKey()) {
						complain(MessageLevel.Debug, "Yes.  What is the ultimate (potential) superclass?");
						MappedClass lclMC = lclMFK.getTargetMappedClass();
						complain(MessageLevel.Debug, "Start lclMC = " + lclMC);
						while (lclMC.getSuperclassKey() != null) {
							lclMC = lclMC.getSuperclassKey().getTargetMappedClass();
							complain(MessageLevel.Debug, "Loop lclMC = " + lclMC);
						}
						complain(MessageLevel.Debug, "End " + lclMC + ".  Does it have Polymorphic data?");
						if (lclMC.isPolymorphic() && lclMC.getPolymorphicData() != null) {
							/* Nothing */
							complain(MessageLevel.Debug, "Yes.  So it's not one-to-one, it's an inheritance.");
						} else {
							complain(MessageLevel.Info, lclMFK + " represents a one-to-one relationship (rather than many-to-one).");
							lclMFK.setRepresentsOneToOneRelationship(true);
							lclMFK.getForeignKey().setCollectionClass(null);
							continue;
						}
					} else {
						complain(MessageLevel.Info, lclMFK + " represents a one-to-one relationship (rather than many-to-one).");
						lclMFK.setRepresentsOneToOneRelationship(true);
						lclMFK.getForeignKey().setCollectionClass(null);
						continue;
					}
				}
			}
		}
	}
	
	public void determineAssociationStatus() {
		/* We don't actually need a primary key to be an association table, or at least
		 * we don't need one that involves the foreign keys as that prevents it from 
		 * joining two rows twice.  We should think about this some more. */
		
		/* We must have exactly two foreign keys to be an association table. */
		
		if (getForeignKeyCount() != 2) {
			setAssociation(false);
			return;
		}
		
		HashSet<ClassMember> lclColumns = new HashSet<>(getClassMembers());
		
		Iterator<MappedForeignKey> lclI = createForeignKeyIterator();
		while (lclI.hasNext()) {
			MappedForeignKey lclMFK = lclI.next();
			lclColumns.removeAll(lclMFK.getSource());
		}
		
		/* Every column not involved in those two primary keys must have a sequence/identity, have a
		 * default and not be updatable, or must be a sequence (ordering) column. */
		
		Iterator<ClassMember> lclCI = lclColumns.iterator();
		while (lclCI.hasNext()) {
			ClassMember lclCM = lclCI.next();
			DatabaseColumn lclDC = lclCM.getDatabaseColumn();
			
			if (!lclCM.isMapped()) {
				continue;
			}
			if (lclDC.hasDatabaseGeneratedNumber()) {
				continue;
			}
			if (lclCM.isOrdering()) {
				continue;
			}
			// System.out.println("The column " + lclCM + " means that " + this + " cannot be hidden as a many-to-many table.");
			setAssociation(false);
			return;
		}
		
		/* They did exhaust it, so every column in the primary key is part of one (or both) of the
		 * foreign keys for this class.  Thus it is an association table. */
		return;
	}
	
	public boolean getDependent() {
		return myDependent;
	}
	
	public void setDependent(boolean argDependent) {
		myDependent = argDependent;
	}
	
	public boolean getAssociation() {
		return myAssociation;
	}
	
	public void setAssociation(boolean argAssociation) {
		myAssociation = argAssociation;
	}
	
	public String getApplicationPackage() {
		return myApplicationPackage;
	}
	
	public String getPersistencePackage() {
		return myPersistencePackage;
	}
	
	public String getCMAPackage() {
		return myCMAPackage;
	}
	
	public String getAuthor() {
		return myAuthor;
	}
	
	public void dropUnmappedClassMembers() {
		Iterator<ClassMember> lclI = createClassMemberIterator();
		int lclFieldIndex = 0;
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (!lclCM.isMapped()) {
				lclI.remove();
			} else {
				lclCM.setFieldIndex(lclFieldIndex++);
			}
		}
		
		/* Drop indexes that used unmapped class members */
		Iterator<MappedUniqueKey> lclJ = getMappedUniqueKeys().iterator();
		KeyLoop: while (lclJ.hasNext()) {
			MappedUniqueKey lclMUK = lclJ.next();
			Iterator<ClassMember> lclK = lclMUK.createClassMemberIterator();
			/* ColumnLoop:*/ while (lclK.hasNext()) {
				ClassMember lclCM = lclK.next();
				if (!lclCM.isMapped()) {
					lclMUK.getMappedClass().complain(MessageLevel.Info, "Dropping unique key " + lclMUK + " because class member " + lclCM + " is unmapped.");
					lclJ.remove();
					continue KeyLoop;
				}
			}
		}
	}
	
	public PolymorphicData getPolymorphicData() {
		return myPolymorphicData;
	}
	
	public void setPolymorphicData(PolymorphicData argData) {
		myPolymorphicData = argData;
	}
	
	public List<MappedClass> getSubclasses() {
		return mySubclasses;
	}
	
	public boolean hasSubclasses() {
		return getSubclasses().isEmpty() == false;
	}
	
	public String getSpecifiedImplementationClassName() {
		return mySpecifiedImplementationClassName;
	}
	
	public void setSpecifiedImplementationClassName(String argString) {
		mySpecifiedImplementationClassName = argString;
	}
	
	/* package */ ClassMember determineName() {
		ClassMember lclCM = getClassMemberByColumnName("name");
		if (lclCM != null) {
			return lclCM;
		}
		
		for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
			if (lclMUK.getClassMembers().size() != 1) {
				continue;
			}
			lclCM = lclMUK.getClassMembers().get(0);
			if (lclCM.getMemberType() == String.class) {
				return lclCM;
			}
		}
		
		return null;
	}
	
	/* package */ ClassMember determineCode() {
		MappedUniqueKey lclPK = getPrimaryKey();
		Validate.notNull(lclPK);
		Validate.notNull(lclPK.getClassMembers());
		
		if (lclPK.getClassMembers().size() == 1) {
			ClassMember lclCM = lclPK.getClassMembers().get(0);
			Class<?> lclT = lclCM.getMemberType();
			if (lclT == String.class || lclT == Integer.class || lclT == Byte.class || lclT == Short.class || lclT == Long.class) {
				return lclCM;
			}
		}
		
		ClassMember lclCM = getClassMemberByColumnName("code");
		if (lclCM != null) {
			return lclCM;
		}
		
		for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
			if (lclMUK.getClassMembers().size() != 1) {
				continue;
			}
			lclCM = lclMUK.getClassMembers().get(0);
			Class<?> lclT = lclCM.getMemberType();
			if (lclT == String.class || lclT == Integer.class || lclT == Byte.class || lclT == Short.class || lclT == Long.class) {
				return lclCM;
			}
		}
		
		return null;
	}
	
	public List<ComparatorSpecification> getComparatorSpecifications() {
		return myComparatorSpecifications;
	}
	
	public List<MethodDelegation> getMethodDelegations() {
		return myMethodDelegations;
	}
	
	public void add(MethodDelegation argMD) {
		Validate.notNull(argMD);
		getMethodDelegations().add(argMD);
	}
	
	public MappedForeignKey getSuperclassKey() {
		if (mySuperclassKeyResolved == false) {
			determineSuperclassKey();
			mySuperclassKeyResolved = true;
		}
		return mySuperclassKey;
	}
	
	public void setSuperclassKey(MappedForeignKey argMFK) { // argMFK can be null
		if (argMFK != null) {
			if (argMFK.getSourceMappedClass() != this) {
				throw new IllegalStateException("Passed a MappedForeignKey to setSuperclassKey but the source MappedClass wasn't this.");
			}
		}
		mySuperclassKey = argMFK;
	}
	
	public MappedClass getSuperclass() {
		if (hasSuperclass()) {
			return getSuperclassKey().getTargetMappedClass();
		} else {
			return null;
		}
	}
	
	public boolean hasSuperclass() {
		return getSuperclassKey() != null && getSuperclassKey().representsPolymorphism();
	}
	
	/* Note that this returns null if the class is not participating in subtable polymorphism even if, trivially, it still seems like
	 * it is its own root superclass.
	 */
	public MappedClass getRootSuperclass() {
		MappedClass lclRoot;
		MappedForeignKey lclSK = getSuperclassKey();
		if (lclSK == null) {
			if (getPolymorphicData() != null && getPolymorphicData() instanceof SubtablePolymorphicData) {
				return this;
			} else {
				return null;
			}
		} else {
			lclRoot = null;
		}
		while (lclSK != null) {
			lclRoot = lclSK.getTargetMappedClass();
			lclSK = lclRoot.getSuperclassKey();
		}
		return lclRoot;
	}
	
	public void validateAndResolveInheritance() {
		MappedForeignKey lclSK = getSuperclassKey();
		PolymorphicData lclPD = getPolymorphicData();
		
		if (lclSK == null) {
			if (lclPD == null) {
				/* That's fine; there is no PolymorphicData and the database doesn't suggest that this is a subtable. */
			} else if (lclPD instanceof SingleTablePolymorphicData) {
				validateAndResolveSingleTablePolymorphicData();
			} else if (lclPD instanceof SubtablePolymorphicData) {
				/* This is apparently the ultimate superclass.  FIXME: We should verify, however, that it actually has subclasses. */
				getSubclasses().add(this);
				validateAndResolveSubtablePolymorphicData();
			}
		} else {
			if (lclPD != null) {
				throw new IllegalStateException("Table " + this.getTableName() + " is configured as a subtable as part of a polymorphism scheme, but it also has a Polymorphism element.  That should only be configured for the ultimate supertable.  If you are trying to use SingleTable Polymorphism for a table is part of Subtable Polymorphism, sorry, Opal doesn't handle that.");
			}
			if (lclSK.representsOneToOneRelationship()) {
				/* Nothing */
			} else {
				MappedForeignKey lclSK2 = lclSK;
				MappedClass lclRootSupertable = null;
				while (lclSK2 != null) {
					lclRootSupertable = lclSK2.getTargetMappedClass();
					lclRootSupertable.getSubclasses().add(this);
					lclSK2 = lclRootSupertable.getSuperclassKey();
				}
				
				assert lclRootSupertable != null;
				assert lclRootSupertable != this;
				
				PolymorphicData lclUPD = lclRootSupertable.getPolymorphicData();
				
				if (lclUPD == null) {
					throw new IllegalStateException("Table " + this.getTableName() + " is apparently part of a Subtable polymorphism scheme, but its ultimate supertable was not configured with a <Polymorphism> element that showed how to map its rows to concrete types.");
				} else if (lclUPD instanceof SingleTablePolymorphicData) {
					throw new IllegalStateException("Table " + this.getTableName() + " has an ultimate supertable that is configured to use SingleTable Polymorphism rather than Subtable Polymorphism.  Opal cannot mix those two types.");
				} else if (lclUPD instanceof SubtablePolymorphicData) {
					/* This is fine, we have a superclass and it has an eventual ancestor with SubtablePolymorphicData.  Ideally, perhaps, we could try to verify
					 * that one can actually instantiate this kind of class from the data in the type table.
					 */
				}
			}
		}
	}
	
	protected void validateAndResolveSingleTablePolymorphicData() {
		PolymorphicData lclPD = getPolymorphicData();
		Validate.notNull(lclPD);
		Validate.isTrue(lclPD instanceof SingleTablePolymorphicData);
		
		SingleTablePolymorphicData lclSPD = (SingleTablePolymorphicData) lclPD;
		
		List<String> lclMethods = lclSPD.getDereferenceMethods();
		List<String> lclNames = lclSPD.getDereferenceNames();
		
		Validate.isTrue(lclMethods.size() == lclNames.size());
		
		MappedClass lclCurrentClass = this;
		
		MethodLoop:
		for (int lclI = 0; lclI < lclMethods.size(); ++lclI) {
			lclCurrentClass.complain(MessageLevel.Debug, "Determining dereference key #" + lclI + " for table " + getTableName() + '.');
			String lclMethod = lclMethods.get(lclI);
			String lclName = lclNames.get(lclI);
			
			// FIXME: Move this switch inside the loop.  (Did I mean outside?)
			
			switch (lclMethod) {
				case "Table":
					for (MappedForeignKey lclMFK : lclCurrentClass.getForeignKeysFrom()) {
						if (lclMFK.getTargetMappedClass().getTableName().getTableName().equals(lclName)) {
							lclSPD.getDereferenceKeys().add(lclMFK);
							lclCurrentClass = lclMFK.getTargetMappedClass();
							continue MethodLoop;
						}
					}
					throw new IllegalStateException("Could not find a foreign key from " + lclCurrentClass.getTableName() + " to table " + lclName + " when attempting to configure polymorphism.");
				case "Type":
					for (MappedForeignKey lclMFK : lclCurrentClass.getForeignKeysFrom()) {
						if (lclMFK.getTargetMappedClass().getTypeName().equals(lclName)) {
							lclSPD.getDereferenceKeys().add(lclMFK);
							lclCurrentClass = lclMFK.getTargetMappedClass();
							continue MethodLoop;
						}
					}
					throw new IllegalStateException("Could not find a foreign key from " + lclCurrentClass.getTableName() + " to the table for type " + lclName + " when attempting to configure polymorphism.");
				default:
					throw new IllegalStateException("Unknown dereference type " + lclMethod);
			}
		}
		
		Validate.isTrue(lclSPD.getDereferenceKeys().size() == lclMethods.size());
		Validate.isTrue(lclSPD.getDereferenceKeys().size() == lclNames.size());
		
		/* FIXME: At some point in here should we verify that the classes listed in that column exist and are are of the
		 * proper interface?  
		 */
		return;
	}
	
	protected void validateAndResolveSubtablePolymorphicData() {
		PolymorphicData lclPD = getPolymorphicData();
		Validate.notNull(lclPD);
		Validate.isTrue(lclPD instanceof SubtablePolymorphicData);
		
		SubtablePolymorphicData lclSPD = (SubtablePolymorphicData) lclPD;
		
		List<String> lclMethods = lclSPD.getDereferenceMethods();
		List<String> lclNames = lclSPD.getDereferenceNames();
		
		Validate.isTrue(lclMethods.size() == lclNames.size());
		
		MappedClass lclCurrentClass = this;
		
		MethodLoop:
		for (int lclI = 0; lclI < lclMethods.size(); ++lclI) {
			lclCurrentClass.complain(MessageLevel.Debug, "Determining dereference key #" + lclI + " for table " + getTableName() + '.');
			String lclMethod = lclMethods.get(lclI);
			String lclName = lclNames.get(lclI);
			
			// FIXME: Move this switch inside the loop
			
			switch (lclMethod) {
			case "Table":
				for (MappedForeignKey lclMFK : lclCurrentClass.getForeignKeysFrom()) {
					if (lclMFK.getTargetMappedClass().getTableName().getTableName().equals(lclName)) {
						lclSPD.getDereferenceKeys().add(lclMFK);
						lclCurrentClass = lclMFK.getTargetMappedClass();
						continue MethodLoop;
					}
				}
				throw new IllegalStateException("Could not find a reference from " + lclCurrentClass.getTableName() + " to table " + lclName + " when attempting to configure polymorphism.");
			case "Type":
				for (MappedForeignKey lclMFK : lclCurrentClass.getForeignKeysFrom()) {
					if (lclMFK.getTargetMappedClass().getTypeName().equals(lclName)) {
						lclSPD.getDereferenceKeys().add(lclMFK);
						lclCurrentClass = lclMFK.getTargetMappedClass();
						continue MethodLoop;
					}
				}
				throw new IllegalStateException("Could not find a reference from " + lclCurrentClass.getTableName() + " to the table for type " + lclName + " when attempting to configure polymorphism.");
			default:
				throw new IllegalStateException("Unknown dereference type " + lclMethod);
			}
		}
		
		Validate.isTrue(lclSPD.getDereferenceKeys().size() == lclMethods.size());
		Validate.isTrue(lclSPD.getDereferenceKeys().size() == lclNames.size());
		
		MappedClass lclMC = getSuperclass();
		if (lclMC != null) {
			if (isCreatable() && lclMC.isCreatable() == false) {
				throw new IllegalStateException("The type " + getTypeName() + " from table " + getTableName() + " is creatable, but the parent type " + lclMC.getTypeName() + " from table " + lclMC.getTableName() + " is not.");
			}
			if (isUpdatable() && lclMC.isUpdatable() == false) {
				throw new IllegalStateException("The type " + getTypeName() + " from table " + getTableName() + " is updatable, but the parent type " + lclMC.getTypeName() + " from table " + lclMC.getTableName() + " is not.");
			}
		}
		
		return;
	}
	
	/* This method looks through the foreign keys "out of" this class to see if any of them is indicative of a subtable
	 * relationship.  That sort of relationship is indicated by having a primary key that, in its entirety, is a foreign
	 * key to the primary key of another table.  If that happens, this table is a "subtable" of the one to which it
	 * refers.
	 * 
	 * If we find such a key, we mark it as isPolymorphism() == true.
	 */
	private void determineSuperclassKey() {
		complain(MessageLevel.Debug, "Determining SuperclassKey for " + this);
		
		/* If the configuration file explicitly says that this table is not polymorphic, then we don't bother checking
		 * anything.  This allows the user to override a database structure that would otherwise result in polymorphic
		 * classes.
		 */
		if (isPolymorphic() == false) {
			setSuperclassKey(null);
			Iterator<MappedForeignKey> lclMFKI = createForeignKeyIterator();
			while (lclMFKI.hasNext()) {
				lclMFKI.next().setRepresentsPolymorphism(false);
			}
		} else {
			/* Grab this table's primary ley. */
			MappedUniqueKey lclPK = getPrimaryKey();
		
			/* Now loop through the foreign keys "out of" this table.  We are looking for one that has the same columns
			 * in the same order as this table's primary key.
			 */
			boolean lclFound = false;
			Iterator<MappedForeignKey> lclMFKI = createForeignKeyIterator();
			while (lclMFKI.hasNext()) {
				MappedForeignKey lclMFK = lclMFKI.next();
				
				complain(MessageLevel.Debug, "Checking " + lclMFK + " to see if it is a FK indicating polymorphism.");
				/* Is the target of the key manually marked as being a polymorphic table?  If it isn't, then we aren't going
				 * to generate polymorphism.
				 */
				if (lclMFK.getTargetMappedClass().isPolymorphic()) {
					/* Does it have the same class members in the same order? */
					if (lclMFK.getSource().hasIdenticalClassMembers(lclPK)) {
						complain(MessageLevel.Debug, "We have found a candidate SuperclassKey on " + lclMFK);
						/* Yes.  Let's make sure that we only find one such key.  Having two would indicate some sort of
						 * multiple inheritance, which Opal doesn't currently support.
						 * 
						 * Have we already found such a matching key?
						 */
						if (lclFound == false) {
							/* No.  That's good. */
							complain(MessageLevel.Info, getTableName() + " appears to be a subclass of " + lclMFK.getTargetMappedClass().getTableName());
							
							/* As a second check, make sure that no prior process has provided this class with a non-null
							 * superclass key.  This shouldn't ever happen, but, if it did, it's a problem to be debugged.
							 */
							if (mySuperclassKey != null) {
								throw new IllegalStateException("But " + getTableName() + " already has a superclass of " + mySuperclassKey.getTargetMappedClass().getTableName());
							}
							
							/* Store this key as the one giving this MappedClass its superclass. */
							setSuperclassKey(lclMFK);
							lclFound = true;
						}
					} else {
						complain(MessageLevel.Debug, "It does not have the same source columns as the primary key.");
					}
				} else {
					complain(MessageLevel.Debug, "The target mapped class is not polymorphic.");
				}
			}
			complain(MessageLevel.Debug, "Done determining SuperclassKey for " + this);
		}
	}
	
	public void determinePolymorphism() {
		MappedForeignKey lclMFK = getSuperclassKey();
		complain(MessageLevel.Debug, "Checking to see if " + this + " actually has a superclass.");
		if (lclMFK != null) {
			complain(MessageLevel.Debug, "It has a non-null SuperclassKey, so possibly.");
			if (lclMFK.representsOneToOneRelationship() == false) {
				complain(MessageLevel.Debug, "The SuperclassKey is not one-to-one, so possibly . . .");
				MappedClass lclMC = lclMFK.getTargetMappedClass();
				while (lclMC.getSuperclassKey() != null) {
					lclMC = lclMC.getSuperclassKey().getTargetMappedClass();
				}
				if (lclMC.isPolymorphic() && lclMC.getPolymorphicData() != null) {
					complain(MessageLevel.Debug, "The ultimate ancestor is Polymorphic and has Polymorphic data defined.");
					/* We mark this key as providing polymorphism. */
					lclMFK.setRepresentsPolymorphism(true);
					complain(MessageLevel.Debug, "So it does have a true superclass.");
					/* We give this key protected access on both the source and the target side so that the accessors and mutators for
					 * it do not show up in the interface.  The user will be oblivious to the fact that the data is stored in multiple
					 * tables (and thus multiple Opals) and he never has to call a get() method to get the other "half" of the data.
					 * 
					 * In addition, we don't want the user mucking with these links.
					 */
					lclMFK.getForeignKey().setSourceAccess("Protected");
					lclMFK.getForeignKey().setTargetAccess("Protected");
					
					/* A subtable relationship will have at most one Opal on the source end (unlike most foreign keys) because that key
					 * is also a primary key (and thus unique).  We don't even need to store a back reference (like other non-primary unique
					 * foreign keys because we never have to follow a link down the hierarchy (toward subclasses).  All of the reasons that
					 * we would want to do that are taken care of by all of the Opals having links to the same UserFacing.
					 * 
					 * So, we don't need to store either a back Collection or a single back reference.
					 */
					lclMFK.getForeignKey().setCollectionClass(null);
					
					/* Indicate that we've found a matching key so that if we find *another* matching key, we can throw an Exception. */
				}
			} else {
				complain(MessageLevel.Debug, "But one-to-one relationship is true.");
			}
		} else {
			complain(MessageLevel.Debug, "But it has a null SuperclassKey.");
		}
	}
	
	/* FIXME: Maybe provide output in the case of two tree-like foreign keys? */
	public void determineTree() {
		complain(MessageLevel.Debug, "Checking to see whether " + this + " is a tree.");
		if (isPotentiallyTree() == false) {
			setTree(false);
			setTreeParentKey(null);
		} else {
			MappedForeignKey lclTreeParentKey = null;
			for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
				if (lclMFK.getSourceMappedClass() == lclMFK.getTargetMappedClass()) {
					if (lclMFK.isUnique() == false) {
						if (lclMFK.getTargetMappedClass().hasIntrinsicOrdering()) {
							if (lclTreeParentKey == null) {
								lclTreeParentKey = lclMFK;
							} else {
								lclTreeParentKey = null;
								break;
							}
						}
					}
				}
			}
			
			if (lclTreeParentKey != null) {
				complain(MessageLevel.Info, this + " can be treated as a tree.");
				setTree(true);
				setTreeParentKey(lclTreeParentKey);
				
				/* FIXME: Only fiddle with these if they have not already been manually specified.  If they have been manually specified,
				 * we'll need to know to generate versions with the proper name on the Interface.
				 */
			}
		}
	}

	public void handleComputedClassMembers() {
		/* We need to look at the various non-synthetic computed fields and determine which other fields
		 * each depends on. */
		
		/* FIXME: This is a mess.  We should compute the dependencies at the same time as we parse the expression, and the
		 * error-handing in the expression parsing is a mess.  It also won't handle expressions that legitimately contain the
		 * percent-sign character.
		 */
				
		Iterator<ClassMember> lclCMI = createClassMemberIterator();
		while (lclCMI.hasNext()) {
			ClassMember lclCM = lclCMI.next();
			
			/* We only look at mapped fields */
			if (lclCM.isMapped() == false) {
				continue;
			}
			
			/* We only have to work on computed fields. */
			if (lclCM.isComputed() == false) {
				continue;
			}
			
			/* Determine dependencies */
			
			/* Determine ClassMember dependencies */
			
			String lclText = lclCM.getRawComputedExpressionText();
			Validate.notNull(lclText);
			Iterator<ClassMember> lclJ = createClassMemberIterator();
			while (lclJ.hasNext()) {
				ClassMember lclCM2 = lclJ.next();
				String lclObjectAccessorSymbol = "%" + lclCM2.getBaseMemberName() + "%";
				String lclPrimitiveAccessorSymbol = "%%" + lclCM2.getBaseMemberName() + "%%"; // Note that any string containing this will also contain the former
				
				if (lclCM2.getPrimitiveAccessorName() != null && lclText.indexOf(lclPrimitiveAccessorSymbol) != -1) {
					complain(MessageLevel.Debug, lclCM.getBaseMemberName() + " is a computed column that depends on the primitive value of " + lclCM2.getBaseMemberName() + ".");
					lclCM2.getDependents().add(lclCM);
				} else if (lclCM2.getObjectAccessorName() != null && lclText.indexOf(lclObjectAccessorSymbol) != -1) {
					complain(MessageLevel.Debug, lclCM.getBaseMemberName() + " is a computed column that depends on the object value of " + lclCM2.getBaseMemberName() + ".");
					lclCM2.getDependents().add(lclCM);
				}
			}
			
			/* Determine ForeignKey dependencies */
			
			Iterator<MappedForeignKey> lclMFKI = createForeignKeyIterator();
			while (lclMFKI.hasNext()) {
				MappedForeignKey lclMFK = lclMFKI.next();
				String lclAccessorSymbol = "%" + lclMFK.getRoleSourceFieldName() + "%";
				if (lclText.indexOf(lclAccessorSymbol) != -1) {
					complain(MessageLevel.Debug, lclCM.getBaseMemberName() + " is a computed column that depends on the value of the " + lclMFK.getRoleSourceFieldName() + " reference (foreign key).");
					lclMFK.getDependents().add(lclCM);
				}
			}
			
			/* Work out the correct Expression */
			
			/* FIXME: Instead of going through getUserFacing(), load the values directly from myNewValues (or myValues) */
			
			StringBuilder lclSB = new StringBuilder();
			int lclI = 0;
			final int lclL = lclText.length();
			while (lclI < lclL) {
				char lclC = lclText.charAt(lclI);
				if (lclC == '%') {
					if (lclI < lclL -1) {
						if (lclText.charAt(lclI+1) == '%') {
							// Primitive reference
							int lclNextPercent = lclText.indexOf('%', lclI + 2);
							if (lclNextPercent == -1) {
								complain(MessageLevel.Error, "Could not parse computed-column expression for " + lclCM.getBaseMemberName() + ".");
								lclSB.setLength(0);
								break;
							} else {
								if (lclNextPercent >= lclL - 1) {
									lclSB.setLength(0);
									complain(MessageLevel.Error, "Error in computed-column expression for " + lclCM.getBaseMemberName() + ".");
									break;
								}
								String lclFieldName = lclText.substring(lclI + 2, lclNextPercent);
								ClassMember lclCM2 = findClassMemberByBaseName(lclFieldName);
								if (lclCM2 != null) {
									lclSB.append("getUserFacing().");
									lclSB.append(lclCM2.getPrimitiveAccessorName());
									lclSB.append("()");
									lclI += lclFieldName.length();
									lclI += 4; // Account for the percent signs
									continue; // continue the while loop
								}
								complain(MessageLevel.Error, "Could not find primitive field " + lclFieldName + " when parsing the computed-column expression for " + lclCM.getBaseMemberName() + ".");
								lclSB.setLength(0);
								break;
							}
						} else {
							// Object or foreign key reference
							int lclNextPercent = lclText.indexOf('%', lclI + 2);
							if (lclNextPercent == -1) {
								complain(MessageLevel.Error, "Could not parse computed-column expression for " + lclCM.getBaseMemberName() + ".");
								lclSB.setLength(0);
								break;
							}
							String lclFieldName = lclText.substring(lclI + 1, lclNextPercent);
							ClassMember lclCM2 = findClassMemberByBaseName(lclFieldName);
							if (lclCM2 != null) {
								lclSB.append("getUserFacing().");
								lclSB.append(lclCM2.getObjectAccessorName());
								lclSB.append("()");
								lclI += lclFieldName.length();
								lclI += 2; // Account for the percent signs
								continue; // continue the while loop
							}
							MappedForeignKey lclMFK = findForeignKeyByRoleSourceFieldName(lclFieldName);
							if (lclMFK != null) {
								lclSB.append("getUserFacing().");
								lclSB.append(lclMFK.getAccessorName());
								lclSB.append("()");
								lclI += lclFieldName.length();
								lclI += 2; // Account for the percent signs
								continue; // continue the while loop
							}
							complain(MessageLevel.Error, "Could not find object or foreign key field " + lclFieldName + " when parsing the computed-column expression for " + lclCM.getBaseMemberName() + ".");
							lclSB.setLength(0);
							break;
						}
					} else {
						complain(MessageLevel.Error, "Percent sign at end of expression in computed-column expression for " + lclCM.getBaseMemberName() + ".");
						lclSB.setLength(0);
						break;
					}
				} else {
					lclSB.append(lclC);
					++lclI;
				}
			}
			if (lclSB.length() > 0) {
				lclCM.setSubstitutedExpressionText(lclSB.toString());
			}
		}
	}
	
	public ClassMember findClassMemberByBaseName(String argBaseName) {
		if (argBaseName == null) {
			return null;
		}
		return getClassMembers().stream()
				.filter(x -> x.getBaseMemberName().equals(argBaseName))
				.findAny()
				.orElse(null);
	}
	
	public MappedForeignKey findForeignKeyByRoleSourceFieldName(String argRoleSourceFieldName) {
		if (argRoleSourceFieldName == null) {
			return null;
		}
		return getForeignKeysFrom().stream()
				.filter(x -> x.getRoleSourceFieldName().equals(argRoleSourceFieldName))
				.findAny()
				.orElse(null);
	}
	
	public boolean isAbstract() {
		return myAbstract;
	}
	
	public boolean isConcrete() {
		return !isAbstract();
	}
	
	public boolean doesImplHaveOpalReference() {
		return isConcrete()  || (isPolymorphic() && (getPolymorphicData() instanceof SingleTablePolymorphicData));
	}
	
	public String getImplOpalMemberName() {
		return "my" + getInterfaceClassName() + "Opal";
	}
	
	public String getImplOpalAccessorName() {
		return "get" + getInterfaceClassName() + "Opal";
	}
	
	public boolean needsConcreteFactoryMethod() {
		return hasSuperclass() && hasSubclasses() && isAbstract() == false;
	}
	
	public String getOpalFactoryAccessorName() {
		return "get" + getOpalFactoryInterfaceName();
	}
	
	public boolean isPolymorphic() {
		return myPolymorphic;
	}
	
	public void setPolymorphic(boolean argPolymorphic) {
		myPolymorphic = argPolymorphic;
	}
	
	public boolean isTree() {
		return myTree;
	}
	
	public void setTree(boolean argTree) {
		myTree = argTree;
	}
	
	public MappedForeignKey getTreeParentKey() {
		return myTreeParentKey;
	}
	
	public void setTreeParentKey(MappedForeignKey argTreeParentKey) {
		myTreeParentKey = argTreeParentKey;
	}
	
	public boolean isPotentiallyTree() {
		return myPotentiallyTree;
	}
	
	public boolean isDeprecated() {
		return myDeprecated;
	}
	
	public MessageLevel getMessageLevel() {
		return myMessageLevel;
	}
	
	public Trinary getEphemeral() {
		return myEphemeral;
	}
	
	public boolean isEphemeral() {
		return getEphemeral().asBooleanPrimitive(isView());
	}
	
	public Trinary hasBackCollectionsByDefault() {
		return myDefaultBackCollections;
	}
	
	public boolean hasBackCollectionsByDefault(boolean argDefault) {
		return myDefaultBackCollections.asBooleanPrimitive(argDefault);
	}
	
	public String getCollections() {
		return myCollections;
	}
	
	public boolean requiresTypedCreate() {
		return isPolymorphic() && (getPolymorphicData() != null) && getPolymorphicData().requiresTypedCreate();
	}
	
	public void complain(MessageLevel argLevel, String argMessage) {
		XMLCreator.complain(argLevel, this, argMessage);
	}
	
	public Trinary getStaticBindings() {
		return myStaticBindings;
	}
	
	public boolean getStaticBindings(boolean argUnknownValue) {
		return myStaticBindings.asBooleanPrimitive(argUnknownValue);
	}
	
	public void setStaticBindings(Trinary argStaticBindings) {
		myStaticBindings = Validate.notNull(argStaticBindings);
	}
	
	public boolean generateStaticBindings() {
		return getStaticBindings(true) && getStaticBindingList() != null && getStaticBindingList().isEmpty() == false;
	}
	
	public boolean hasDatabaseGeneratedColumns() {
		Iterator<ClassMember> lclCMI = createClassMemberIterator();
		while (lclCMI.hasNext()) {
			ClassMember lclCM = lclCMI.next();
			if (lclCM.isMapped() && lclCM.getDatabaseColumn().hasDatabaseGeneratedNumber()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasComplicatedDefaults() {
		Iterator<ClassMember> lclCMI = createClassMemberIterator();
		while (lclCMI.hasNext()) {
			ClassMember lclCM = lclCMI.next();
			if (lclCM.isMapped() && lclCM.getDatabaseColumn().hasComplicatedDefault()) {
				return true;
			}
		}
		return false;
	}
	
	public List<MultipleLookUpSpecification> getMultipleLookUpSpecifications() {
		return myMultipleLookUpSpecifications;
	}
	
	public void addMultipleLookUpSpecification(MultipleLookUpSpecification argMLUS) {
		getMultipleLookUpSpecifications().add(argMLUS);
	}
	
	public boolean hasMultipleLookUpSpecifications() {
		return getMultipleLookUpSpecifications().isEmpty() == false;
	}
	
	public void setMone(Trinary argMone) {
		Validate.notNull(argMone);
		myMone = argMone;
	}
	
	public Trinary isMone() {
		return myMone;
	}
	
	protected String getFactorySingletonInstanceName() {
		return "ourInstance";
	}
	
	protected String getMoneFactorySingletonInstanceName() {
		return "ourMoneInstance";
	}
	
	protected String getFactorySingletonAccessorName() {
		return "getInstance";
	}
	
	protected String getMoneFactorySingletonAccessorName() {
		return "getMoneInstance";
	}
	
	public boolean usesSoftReferences() {
		return mySoftReferences;
	}
	
	public void setSoftReferences(boolean argSoftReferences) {
		mySoftReferences = argSoftReferences;
	}
	
	public String getPoolName() {
		return myPoolName;
	}
	
	public void setPoolName(String argPoolName) {
		Validate.notNull(argPoolName);
		myPoolName = argPoolName;
	}
	
	@SuppressWarnings("resource")
	protected void printRequiresActiveTransactionAnnotation(PrintWriter argW, int argIndentations) {
		Validate.notNull(argW);
		Validate.isTrue(argIndentations >= 0);
		
		argW.println(StringUtils.repeat('\t', argIndentations) + "@" + RequiresActiveTransaction.class.getName());
	}
}
