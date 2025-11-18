package com.opal.creator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.util.Fast3Set;
import com.siliconage.util.TypeUtility;
import com.siliconage.util.UnimplementedOperationException;
import com.siliconage.util.StringUtility;
import com.siliconage.util.Trinary;
import com.siliconage.web.JavadocHelper;
import com.opal.AbstractFactory;
import com.opal.AbstractFactoryMap;
import com.opal.AbstractIdentityFactory;
import com.opal.AbstractIdentityImpl;
import com.opal.AbstractImpl;
import com.opal.AbstractMoneIdentityOpalFactory;
import com.opal.ArgumentTooLongException;
import com.opal.EphemeralOpal;
import com.opal.FactoryCreator;
import com.opal.FactoryPolymorphicCreator;
import com.opal.FieldValidator;
import com.opal.IdentityOpalFactory;
import com.opal.IdentityUserFacing;
import com.opal.IllegalNullArgumentException;
import com.opal.ImmutableOpal;
import com.opal.ImplicitTableDatabaseQuery;
import com.opal.MultipleValueDatabaseOpalKey;
import com.opal.NullValueException;
import com.opal.OpalBacked;
import com.opal.OpalCache;
import com.opal.OpalFactory;
import com.opal.OpalFactoryCreator;
import com.opal.OpalFactoryPolymorphicCreator;
import com.opal.OpalField;
import com.opal.OpalKey;
import com.opal.OpalPlainField;
import com.opal.OpalUtility;
import com.opal.PersistenceException;
import com.opal.ReferenceType;
import com.opal.SingleValueDatabaseOpalKey;
import com.opal.StoreGeneratedPrimaryKey;
import com.opal.TransactionAware;
import com.opal.TransactionContext;
import com.opal.TransactionParameter;
import com.opal.Tree;
import com.opal.TreeAdapter;
import com.opal.UpdatableOpal;
import com.opal.UserFacing;
import com.opal.UserFacingBackCollectionSet;
import com.opal.annotation.Creatability;
import com.opal.annotation.Nullability;
import com.opal.annotation.RequiresActiveTransaction;
import com.opal.annotation.Updatability;
import com.opal.creator.database.ReferentialAction;
import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.DefaultValue;
import com.opal.creator.database.EntityType;
import com.opal.creator.database.TableName;
import com.opal.types.JavaClass;
import com.opal.types.OpalBackCollectionDoubleSet;
import com.opal.types.OpalBackCollectionLoader;
import com.opal.types.OpalBackCollectionSet;

public class MappedClass {
	private static final String NOT_YET_LOADED_STATIC_MEMBER_NAME = "NOT_YET_LOADED";
	
	private static final boolean GENERATE_FIELD_CLASS = false;
	
	public static final String USER_FACING_SUFFIX = "UserFacing";
	public static final String FACTORY_SUFFIX = "Factory";
	public static final String OPAL_SUFFIX = "Opal";
	public static final String IMPLEMENTATION_SUFFIX = "Impl";
	public static final String MONE_PREFIX = "Mone";
	
	public static final String MONE_SUBPACKAGE_NAME = "mone";
	
	private static final Class<?>[] OUTPUT_CLASSES = {PrintStream.class, PrintWriter.class}; // These need to have a method named "println" that takes one argument, a String. If you add to or remove from this array, you'll have to modify AbstractImpl too.
	
	@SuppressWarnings("rawtypes") private static final Class<? extends Stream> USER_FACING_STREAM_CLASS = Stream.class;
	@SuppressWarnings("rawtypes") private static final Class<? extends Set> USER_FACING_SET_CLASS = Set.class;
	
	@SuppressWarnings("rawtypes") private static final Class<? extends Collection> ACQUIRE_COLLECTION_INTERFACE = Collection.class;
	private static final Class<? extends HttpServletRequest> HTTP_REQUEST_CLASS = HttpServletRequest.class;
	@SuppressWarnings("rawtypes") private static final Class<? extends Collection> HTTP_MULTIPLE_INTERMEDIATE_COLLECTION_CLASS = LinkedList.class;
	@SuppressWarnings("rawtypes") private static final Class<? extends Collection> CREATE_ARRAY_INTERMEDIATE_COLLECTION_CLASS = LinkedList.class;
	
	@SuppressWarnings("rawtypes") private static final Class<? extends Collection> MULTIPLE_LOOK_UP_COLLECTION_INTERFACE = List.class;
	@SuppressWarnings("rawtypes") private static final Class<? extends Collection> MULTIPLE_LOOK_UP_COLLECTION_CLASS = LinkedList.class;
	
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
	
	protected void createImplementation() throws IOException {
		String lclAbstractClassFileName = StringUtility.makeFilename(getSourceDirectory(), getImplementationPackageName(), getImplementationClassName());
		
		File lclAbstractClassFile = new File(lclAbstractClassFileName);
		
		try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclAbstractClassFile)))) {
			lclBW.println("package " + getImplementationPackageName() + ';');
			lclBW.println();
			
			final String lclICN = getFullyQualifiedInterfaceClassName();
			final String lclOCN = getFullyQualifiedOpalClassName();
			
			lclBW.println("@javax.annotation.Generated(\"com.opal\")");
			if (isDeprecated()) {
				lclBW.println("@Deprecated");
			}
			// TODO Figure out how to not make this class public
			lclBW.print("public ");
			if (isAbstract()) {
				lclBW.print("abstract ");
			}
			lclBW.print("class " + getImplementationClassName() + " extends ");
			if (hasSuperclass() == false) {
				if (isEphemeral()) {
					lclBW.print(AbstractImpl.class.getName() + "<" + lclICN + ", " + lclOCN + "> ");
				} else {
					lclBW.print(AbstractIdentityImpl.class.getName() + "<" + lclICN + ", " + lclOCN + "> ");
				}
			} else {
				lclBW.print(getSuperclass().getImplementationClassName() + ' ');
			}
			lclBW.println("implements " + lclICN + " {");
			
			lclBW.println();
			
			if (doesImplHaveOpalReference()) {
				lclBW.println("\tprivate final " + lclOCN + ' ' + getImplOpalMemberName() + ';');
				lclBW.println();
			}
			
			// TODO: Figure out how to not make this constructor public
			// FIXME: Can we figure out a way to disallow passing NOT_YET_LOADED for the "base" object?
			{
				String lclA = "arg" + getOpalClassName();
				lclBW.print("\t");
				if (isAbstract()) { /* FIXME: Check to make sure only tables with subtables are declared Abstract? */
					lclBW.print("protected ");
				} else {
					lclBW.print("public ");
				}
				lclBW.print(getImplementationClassName() + "(");
				if (doesImplHaveOpalReference()) {
					lclBW.print(getOpalClassName() + ' ' + lclA);
				}
				lclBW.println(") {");
				
				if (hasSuperclass() == false) {
					lclBW.println("\t\tsuper();");
				} else if (getSuperclass().isAbstract()) {
					lclBW.println("\t\tsuper();");
				} else {
					lclBW.println("\t\tsuper(" + getSuperclass().getFullyQualifiedOpalClassName() + '.' + NOT_YET_LOADED_STATIC_MEMBER_NAME + ");");
				}
				
				if (doesImplHaveOpalReference()) {
					lclBW.println("\t\t" + getImplOpalMemberName() + " = org.apache.commons.lang3.Validate.notNull(" + lclA + ");");
				}
				lclBW.println("\t}");
				lclBW.println();
			}
			
			String lclIOAN = getImplOpalAccessorName() + "()";
			if (doesImplHaveOpalReference()) {
				lclBW.println("\tprotected " + lclOCN + ' ' + lclIOAN + " {");
				// lclBW.println("\t\tassert " + getImplOpalMemberName() + " != " + lclOCN + ".NOT_YET_LOADED;");
				lclBW.println("\t\treturn " + getImplOpalMemberName() + ';');
				lclBW.println("\t}");
			} else {
				lclBW.println("\tprotected abstract " + lclOCN + ' ' + lclIOAN + ';');
			}
			lclBW.println();
			
			if (hasSuperclass()) {
				MappedForeignKey lclSK = getSuperclassKey();
				MappedClass lclSC = getSuperclass();
				assert lclSK.getTargetMappedClass() == lclSC;
				
				lclBW.println("\t@Override");
				lclBW.println("\tprotected " + lclSC.getOpalClassName() + ' ' + lclIOAN + " {");
				lclBW.println("\t\treturn " + lclIOAN + ".get" + lclSK.getSourceOpalFieldName() + "();");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			lclBW.println("\t@Override");
//			if (isEphemeral() == false) {
//				lclBW.println("\tpublic " + lclOCN + " getOpal() {"); // xyzzy
//			} else {
				lclBW.println("\tpublic " + lclOCN + " getOpal() {");
//			}
			lclBW.println("\t\treturn " + lclIOAN + ';');
			lclBW.println("\t}");
			lclBW.println();
			
			if (getSuperclass() != null) {
				for (Class<?> lclOutput : OUTPUT_CLASSES) {
					lclBW.println("\t@Override");
					lclBW.println("\tprotected void outputSuperclassOpalFields(" + lclOutput.getName() + " argOutput) {");
					lclBW.println("\t\tif (argOutput != null) {");
					lclBW.println("\t\t\tget" + getSuperclassKey().getRoleSourceOpalFieldName() + "().output(argOutput);");
					lclBW.println("\t\t}");
					lclBW.println("\t}");
					lclBW.println();
				}
			}
			
			if (doesImplHaveOpalReference()) {
				lclBW.println("\t@Override");
//				if (isEphemeral() == false) {
//					lclBW.println("\tpublic " + lclOCN + " getBottomOpal() {");
//				} else {
					lclBW.println("\tpublic " + lclOCN + " getBottomOpal() {");
//				}
				lclBW.println("\t\treturn " + lclIOAN + ';');
				lclBW.println("\t}");
				lclBW.println();
			}
			
			{
				ClassMember lclCM = getOrderingMember();
				if (lclCM != null) {
					String lclFirst = lclCM.isInverted() ? "argSecond" : "this";
					String lclSecond = lclCM.isInverted() ? "this" : "argSecond";
					
					lclBW.println("\t@Override");
					lclBW.println("\tpublic int compareTo(" + lclICN + " argSecond) {");
					if (lclCM.isNullAllowed()) {
						lclBW.println("\t\treturn com.siliconage.util.NullSafeComparator.nullSafeCompare(" + lclFirst + '.' + lclCM.getObjectAccessorName() + "(), " + lclSecond + '.' + lclCM.getObjectAccessorName() + "());");
					} else {
						lclBW.println("\t\treturn " + lclFirst + '.' + lclCM.getObjectAccessorName() + "().compareTo(" + lclSecond + '.' + lclCM.getObjectAccessorName() + "());");
					}
					lclBW.println("\t}");
					lclBW.println();
				}
			}
			
			/* This set may no longer be relevant now that Impl classes have an inheritance structure that mimics that of the UserFacings. */
			
			for (ClassMember lclCM : getClassMembers()) {
				if (lclCM.isMapped()) {
					final Class<?> lclMemberType = lclCM.getMemberType();
					final String lclMemberTypeName = lclCM.getMemberParameterizedTypeName();
					final Class<?> lclPrimitiveType = ClassUtils.wrapperToPrimitive(lclMemberType);
					
					boolean lclInUF = lclCM.appearsInUserFacing(this);
					String lclAccess = lclInUF ? "public" : "protected";
					
					lclCM.outputAnnotations(lclBW, this);
					if (lclInUF) {
						lclBW.println("\t@Override");
					}
					lclBW.println("\t" + lclAccess + ' ' + lclMemberTypeName + ' ' + lclCM.getObjectAccessorName() + "() {");
					lclBW.println("\t\treturn " + lclIOAN + '.' + lclCM.getObjectAccessorName() + "();");
					lclBW.println("\t}");
					lclBW.println();
					
					if (lclInUF == false) {
						lclBW.print("\tprotected " + lclPrimitiveType.getName() + ' ' + lclCM.getPrimitiveAccessorName() + "()");
						if (lclCM.isNullAllowed()) {
							lclBW.print(" throws " + NullValueException.class.getName());
						}
						lclBW.println(" {");
						lclBW.println("\t\t" + lclMemberTypeName + " lclO = " + lclCM.getObjectAccessorName() + "();");
						if (lclCM.isNullAllowed()) {
							lclBW.println("\t\tif (lclO == null) {");
							lclBW.println("\t\t\tthrow new " + NullValueException.class.getName() + "(\"The internal value is null and cannot be returned as a primitive.\");");
							lclBW.println("\t\t}");
						}
						lclBW.println("\t\treturn lclO." + TypeUtility.getPrimitiveAccessor(lclMemberType) + "();");
						lclBW.println("\t}");
						lclBW.println();
						lclBW.println("\tprotected " + lclPrimitiveType.getName() + ' ' + lclCM.getPrimitiveAccessorName() + "(" + lclPrimitiveType.getName() + " argStringToSubstituteIfNull) {");
						lclBW.println("\t\t" + lclMemberTypeName + " lclO = " + lclCM.getObjectAccessorName() + "();");
						lclBW.println("\t\treturn lclO != null ? lclO." + TypeUtility.getPrimitiveAccessor(lclMemberType) + "() : argStringToSubstituteIfNull;");
						lclBW.println("\t}");
						lclBW.println();
					}
					
					/* FIXME: Add inverse methods that don't appear in the UserFacing (due to polymorphism). */
					
					if ((isCreatable() || (isUpdatable() && lclCM.isUpdatable())) && (lclCM.isComputed() == false)) {
						lclCM.outputAnnotations(lclBW, this);
						if (lclInUF) {
							lclBW.println("\t@Override");
						}
						lclBW.println("\t" + lclAccess + ' ' + determineMutatorReturnType(getFullyQualifiedImplementationClassName()) + ' ' + lclCM.getObjectMutatorName() + '(' + lclMemberTypeName + ' ' + lclCM.getObjectMutatorArgumentName() + ") {");
						lclBW.println("\t\t" + lclIOAN + '.' + lclCM.getObjectMutatorName() + '(' + lclCM.getObjectMutatorArgumentName() + ");");
						lclBW.println("\t\t" + getMutatorReturnValue());
						lclBW.println("\t}");
						lclBW.println();
						
						if (lclPrimitiveType != null) {
							if (lclInUF) {
								lclBW.println("\t@Override");
							}
							lclBW.println("\t" + lclAccess + ' ' + determineMutatorReturnType(getFullyQualifiedImplementationClassName()) + ' ' + lclCM.getPrimitiveMutatorName() + '(' + lclPrimitiveType.getName() + ' ' + lclCM.getPrimitiveMutatorArgumentName() + ") {");
							lclBW.println("\t\t" + lclIOAN + '.' + lclCM.getPrimitiveMutatorName() + '(' + lclCM.getPrimitiveMutatorArgumentName() + ");");
							lclBW.println("\t\t" + getMutatorReturnValue());
							lclBW.println("\t}");
							lclBW.println();
						}
					}
				}
			}
			
			/* Large Members */
			for (ClassMember lclCM : getLargeClassMembers()) {
				if (lclCM.isMapped()) {
					final Class<?> lclMemberType = lclCM.getMemberType();
					final String lclMemberTypeName = OpalUtility.generateTypeName(lclMemberType);
					
					lclBW.println("\tpublic " + lclMemberTypeName + ' ' + lclCM.getPrimitiveAccessorName() + "() {");
					lclBW.println("\t\treturn " + lclIOAN + '.' + lclCM.getPrimitiveAccessorName() + "();");
					lclBW.println("\t}");
					lclBW.println();
					if (isUpdatable()) {
						lclBW.println("\tpublic " + determineMutatorReturnType(getFullyQualifiedImplementationClassName()) + ' ' + lclCM.getPrimitiveMutatorName() + '(' + lclMemberTypeName + ' ' + lclCM.getObjectMutatorArgumentName() + ") {");
						lclBW.println("\t\t" + lclIOAN + '.' + lclCM.getPrimitiveMutatorName() + '(' + lclCM.getObjectMutatorArgumentName() + ");");
						lclBW.println("\t\t" + getMutatorReturnValue());
						lclBW.println("\t}");
						lclBW.println();
					}
				}
			}
			
			/* References (Foreign Keys) */
			lclBW.println("\t/* The following methods allow direct access to the user objects to which");
			lclBW.println("\tthis object has references in the database. */");
			lclBW.println();
			
			Iterator<MappedForeignKey> lclMFKI = createForeignKeyIterator();
			while (lclMFKI.hasNext()) {
				MappedForeignKey lclMFK = lclMFKI.next();
				
				if (lclMFK.representsPolymorphism()) {
					continue;
				}
				
				MappedClass lclT = lclMFK.getTargetMappedClass();
				
				lclBW.println("\t/** @return the " + lclT.getTypeName() + " object created from " + getTableName().getFullyQualifiedTableName() + " through reference " + lclMFK.getForeignKey().getName() + " */");
				lclBW.println();
				
				lclBW.println("\t@" + Nullability.class.getName() + "(nullable = " + (!lclMFK.isRequired()) + ")");
				
				boolean lclInUF = true;
				if ("Protected".equalsIgnoreCase(lclMFK.getSourceFieldAccess())) {
					lclInUF = false;
				} else if (lclMFK.appearsInSourceUserFacing() == false) {
					lclInUF = false;
				}
				
				String lclAccess = lclInUF ? "public" : "protected";
				
				String lclDeprecated = lclT.isDeprecated() && (isDeprecated() == false) ? "\t@Deprecated" + System.lineSeparator() : "";
				if (lclInUF) {
					lclBW.println("\t@Override");
				}
				lclBW.print(lclDeprecated);
				lclBW.println("\t" + lclAccess + ' ' + lclT.getFullyQualifiedTypeName() + " get" + lclMFK.getRoleSourceFieldName() + "() {");
				lclBW.println("\t\t" + lclT.getOpalClassName() + " lcl" + lclT.getOpalClassName() + " = " + lclIOAN + ".get" + lclMFK.getRoleSourceOpalFieldName() + "();");
				lclBW.println("\t\treturn lcl" + lclT.getOpalClassName() + " == null ? null : lcl" + lclT.getOpalClassName() + ".getUserFacing();");
				lclBW.println("\t}");
				lclBW.println();
				
				if (isCreatable() || isUpdatable()) {
					String lclA = "arg" + lclT.getTypeName();
					if (lclInUF) {
						lclBW.println("\t@Override");
					}
					lclBW.print(lclDeprecated);
					lclBW.println("\t@SuppressWarnings(\"unchecked\")"); // FIXME: Should we check to make sure it's an OpalBacked at this point?  How expensive is that?
					lclBW.println("\t" + lclAccess + ' ' + determineMutatorReturnType(getFullyQualifiedInterfaceClassName()) + " set" + lclMFK.getRoleSourceFieldName() + "(" + lclT.getFullyQualifiedTypeName() + ' ' + lclA + ") {");
					lclBW.println("\t\t" + lclIOAN + ".set" + lclMFK.getRoleSourceOpalFieldName() + "(" + lclA + " == null ? null : ((" + OpalBacked.class.getName() + "<" + lclT.getFullyQualifiedTypeName() + ", " + lclT.getFullyQualifiedOpalClassName() + ">) " + lclA + ").getOpal());");
					lclBW.println("\t\t" + getMutatorReturnValue());
					lclBW.println("\t}");
					lclBW.println();
				}
			}
			
			/* One-to-one foreign keys */
			for (MappedForeignKey lclMFK : getForeignKeysTo()) {
				if (lclMFK.representsPolymorphism()) {
					continue;
				}
				if (lclMFK.representsManyToOneRelationship()) {
					continue;
				}
				
				MappedClass lclSMC = lclMFK.getSourceMappedClass();
				
				// lclBW.println("\t/* Access to the " + lclSMC.getTypeName() + " object created from the table " + getTableName().getFullyQualifiedTableName() + " through foreign key " + lclMFK.getForeignKey().getName() + " */");
				// lclBW.println();
				
				String lclAccess = "Protected".equalsIgnoreCase(lclMFK.getTargetFieldAccess()) ? "protected" : "public";
				
				String lclDeprecated = lclSMC.isDeprecated() && (isDeprecated() == false) ? "\t@Deprecated" + System.lineSeparator() : "";
				lclBW.println("\t@Override");
				lclBW.print(lclDeprecated);
				lclBW.println("\t" + lclAccess + ' ' + lclSMC.getFullyQualifiedTypeName() + " get" + lclMFK.getRoleTargetFieldName() + "() {");
				lclBW.println("\t\t" + lclMFK.getSourceOpalClassName() + " lclO = " + lclIOAN + ".get" + lclMFK.getRoleTargetOpalFieldName() + "();");
				lclBW.println("\t\treturn lclO == null ? null : lclO.getUserFacing();");
				lclBW.println("\t}");
				lclBW.println();
				
				String lclA = "arg" + lclSMC.getTypeName();
				lclBW.println("\t@Override");
				lclBW.print(lclDeprecated);
				lclBW.println("\t" + lclAccess + ' ' + determineMutatorReturnType(getFullyQualifiedInterfaceClassName()) + " set" + lclMFK.getRoleTargetFieldName() + "(" + lclSMC.getFullyQualifiedTypeName() + ' ' + lclA + ") {");
				lclBW.println("\t\t" + lclIOAN + ".set" + lclMFK.getRoleTargetOpalFieldName() + "(" + lclA + " == null ? null : ((" + lclSMC.getImplementationClassName() + ") " + lclA + ")." + lclSMC.getImplOpalAccessorName() + "());");
				lclBW.println("\t\t" + getMutatorReturnValue());
				lclBW.println("\t}");
				lclBW.println();
			}
			
			lclBW.println("\t/* The following methods allow access to the user objects that have references");
			lclBW.println("\tto this object. */");
			lclBW.println();
			
			/* Collections (resulting from incoming, one-to-many foreign keys) */
			
			for (MappedForeignKey lclMFK : getForeignKeysTo()) {
				if (lclMFK.representsPolymorphism()) {
					continue;
				}
				if (lclMFK.representsOneToOneRelationship()) {
					continue;
				}
				
				final MappedClass lclSMC = lclMFK.getSourceMappedClass();
				
				if (lclMFK.hasBackCollection() == false) {
					continue;
				}
				
				String lclAccess = "Protected".equalsIgnoreCase(lclMFK.getTargetCollectionAccess()) ? "protected" : "public";
				
				String lclDeprecated = lclSMC.isDeprecated() && (isDeprecated() == false) ? "\t@Deprecated" + System.lineSeparator() : "";
				
				lclBW.println("\t@Override");
				lclBW.print(lclDeprecated);
				lclBW.println("\t" + lclAccess + ' ' + USER_FACING_SET_CLASS.getName() + "<" + lclSMC.getFullyQualifiedTypeName() + "> " + lclMFK.getRoleCollectionAccessorName() +"() {");
				lclBW.println("\t\treturn new " + UserFacingBackCollectionSet.class.getName() + "<>(" + lclIOAN + "." + lclMFK.getRoleOpalCollectionAccessorName() + "());");
				lclBW.println("\t}");
				lclBW.println();				
			}
			
			/* Done with superclasses. */
			
			if (isEphemeral() == false) {
				lclBW.println("\t@Override");
				lclBW.println("\tpublic void unlink() {");
				lclBW.println("\t\t" + lclIOAN + ".unlink();");
				if (hasSuperclass()) {
					lclBW.println("\t\tsuper.unlink();");
				}
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\t@Override");
				lclBW.println("\tpublic void reload() {");
				if (hasSuperclass()) {
					lclBW.println("\t\tsuper.reload();");
				}
				lclBW.println("\t\t" + lclIOAN + ".reload();");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (isCreatable()) {
				if (hasSuperclass() == false) {
					lclBW.println("\t@Override");
					lclBW.println("\tpublic " + lclICN + " copy() {");
					lclBW.println("\t\treturn " + lclIOAN + ".copy().getUserFacing();");
					lclBW.println("\t}");
					lclBW.println();
				} else {
					lclBW.println("\t@Override");
					lclBW.println("\tpublic " + lclICN + " copy() {");
					lclBW.println("\t\tthrow new com.siliconage.util.UnimplementedOperationException();");
					lclBW.println("\t}");
					lclBW.println();
				}
			}
			
			lclBW.println("}");
		} // Closes lclBW via try-with-resources
	}
	
	/* TODO: Move this elsewhere */
	
	public static void ensureDirectoryExists(File argFile) {
		if (!argFile.exists()) {
			if (argFile.mkdirs() == false) {
				throw new RuntimeException("Unable to create directories for " + argFile);
			}
		}
	}
	
	public void createClasses() throws IOException {
		ensureDirectoryExists(new File(StringUtility.makeDirectoryName(getSourceDirectory(), getApplicationPackage())));
		
		createFactory();
		createUserFacing();
		createInterface();
		
		ensureDirectoryExists(new File(StringUtility.makeDirectoryName(getSourceDirectory(), getPersistencePackage())));
		
		createImplementation();
		createOpal();
		createOpalFactory();
		if (isMone().asBooleanPrimitive(false)) {
			createMoneOpalFactory();
		}
		
	}
	
	public Iterator<ClassMember> createClassMemberIterator() {
		return getClassMembers().iterator();
	}
	
	public Iterator<ClassMember> createLargeClassMemberIterator() {
		return getLargeClassMembers().iterator();
	}
	
	protected void createFactory() throws IOException {
		String lclFactoryClassFileName = StringUtility.makeFilename(getSourceDirectory(), getFactoryPackageName(), getFactoryClassName());
		
		File lclFactoryClassFile = new File(lclFactoryClassFileName);
		
		try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclFactoryClassFile)))) {
			lclBW.println("package " + getFactoryPackageName() + ';');
			lclBW.println();

			lclBW.println("import " + getFullyQualifiedOpalFactoryInterfaceName() + ';');
			lclBW.println("import " + getFullyQualifiedOpalClassName() + ';');
			lclBW.println("import " + getOpalFactoryPackageName() + ".OpalFactoryFactory;");
			if (isMone().asBooleanPrimitive(false)) {
				lclBW.println("import " + getFullyQualifiedMoneOpalFactoryClassName() + ';');
			}
			lclBW.println();
			
			final String lclICN = getInterfaceClassName();
			final String lclOCN = getOpalClassName();
			
			Class<?> lclAbstractClass = isEphemeral() ? AbstractFactory.class : AbstractIdentityFactory.class;
			
			Class<?> lclOpalFactoryClass = isEphemeral() ? OpalFactory.class : IdentityOpalFactory.class;
			
			lclBW.println("@javax.annotation.Generated(\"com.opal\")");
			if (isDeprecated()) {
				lclBW.println("@Deprecated");
			}
			lclBW.print("public class " + getFactoryClassName() + " extends " + lclAbstractClass.getName() + "<" + lclICN + ", " + lclOCN + ">");
			
			/* If new Opals of this type can be created, then the Factory also needs to implement FactoryCreator<UserFacing> */
			
			if (implementsPolymorphicCreator()) {
				MappedClass lclUCTD = getPolymorphicData().getUltimateConcreteTypeDeterminer();
				lclBW.print(" implements " + FactoryPolymorphicCreator.class.getName() + "<" + lclICN + ", " + lclUCTD.getFullyQualifiedInterfaceClassName() + ">");
			} else if (implementsCreator()) {
				lclBW.print(" implements " + FactoryCreator.class.getName() + "<" + lclICN + ">");
			} else {
				/* Nothing. */
			}
			
			lclBW.println(" {");
			
			lclBW.println();
			
			/* Static instance variable for holding the Singleton instance of the class */
			lclBW.println("\t/** This static variable holds the Singleton instance of the Factory for application");
			lclBW.println("\t	objects of this type.  It is private, but can be accessed via the getInstance() method.");
			lclBW.println("\t*/");
			lclBW.println();
			lclBW.println("\tprivate static final " + getFactoryClassName() + ' ' + getFactorySingletonInstanceName() + " = new " + getFactoryClassName() + "(OpalFactoryFactory.getInstance().get" + getOpalFactoryInterfaceName() + "());");
			lclBW.println();
			
			/* Static accessor for the Singleton instance of the class */
			
			lclBW.println("\tpublic static " + getFactoryClassName() + ' ' + getFactorySingletonAccessorName() + "() { return " + getFactorySingletonInstanceName() + "; }");
			lclBW.println();
			
			lclBW.println("\tpublic " + getOpalFactoryInterfaceName() + ' ' + getOpalFactoryAccessorName() + "() { return (" + getOpalFactoryInterfaceName() + ") getOpalFactory(); }");
			lclBW.println();
			
			if (isMone().asBooleanPrimitive(false)) {
				lclBW.println("\tprivate static final " + getFactoryClassName() + ' ' + getMoneFactorySingletonInstanceName() + " = new " + getFactoryClassName() + "(" + getMoneOpalFactoryClassName() + ".getInstance());");
				lclBW.println();
				lclBW.println("\tpublic static " + getFactoryClassName() + ' ' + getMoneFactorySingletonAccessorName()+ "() { return " + getMoneFactorySingletonInstanceName() + "; }");
				lclBW.println();
			}
			
			/* Constructor */
			
			lclBW.println("\tprotected " + getFactoryClassName() +"(" + lclOpalFactoryClass.getName() + '<' + lclICN + ", "+ lclOCN + "> argOpalFactory) {");
			lclBW.println("\t\tsuper(argOpalFactory);");
			lclBW.println("\t}");
			lclBW.println();
			
			// THINK: How does this work with the various types of polymorphism?
			
			lclBW.println("\t@Override");
			lclBW.println("\tpublic Class<" + lclICN + "> getUserFacingInterface() {");
			lclBW.println("\t\treturn " + lclICN + ".class;");
			lclBW.println("\t}");
			lclBW.println();
			
			if (generateStaticBindings()) {
				lclBW.println();
				lclBW.println("\t/* These Singleton objects are initialized on system start up to refer to unchanging objects");
				lclBW.println("\t	from the database.");
				lclBW.println("\t*/");
				lclBW.println();
				
				/* TODO:  This fails when the name of the variable differs from the code used (like when it starts with a number
				and needs to have an underscore prepended to make a valid Java identifier. */
				
				for (String lclCode : getStaticBindingList()) {
					lclBW.println("\tpublic static final " + getInterfaceClassName() + ' ' + Mapping.convertToJavaIdentifier(lclCode) + "() { return getInstance().forCode(\"" + lclCode + "\"); }");
				}
				lclBW.println();
			}
			
			/* Factory method for creating a new instance of the object */
			if (implementsPolymorphicCreator()) {
				MappedClass lclUCTD = getPolymorphicData().getUltimateConcreteTypeDeterminer();
				printRequiresActiveTransactionAnnotation(lclBW, 1);
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + lclICN + " create(" + lclUCTD.getFullyQualifiedInterfaceClassName() + " argType) {");
				lclBW.println("\t\treturn " + getOpalFactoryAccessorName() + "().create(argType).getUserFacing();");
				lclBW.println("\t}");
				lclBW.println();
			} else if (implementsCreator()) {
				printRequiresActiveTransactionAnnotation(lclBW, 1);
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + lclICN + " create() {");
				lclBW.println("\t\treturn " + getOpalFactoryAccessorName() + "().create().getUserFacing();");
				lclBW.println("\t}");
				lclBW.println();
			} else {
				/* No create methods. */
			}
			
			/* Create a factory method for each unique key */
			for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
				
				lclBW.print("\tpublic " + getInterfaceClassName() + ' ' + lclMUK.generateFactoryMethodName() + "(");
				lclBW.print(lclMUK.generateFactoryMethodArguments());
				lclBW.println(") {");
				
				String lclVariable = "lcl" + getOpalClassName();
				
				lclBW.print("\t\t" + getOpalClassName() + ' ' + lclVariable + " = ");
				lclBW.print(getOpalFactoryAccessorName() + "()." + lclMUK.generateOpalFactoryMethodName(false) + "(");
				boolean lclFirst = true;
				Iterator<ClassMember> lclCMI = lclMUK.createClassMemberIterator();
				while (lclCMI.hasNext()) {
					ClassMember lclCM = lclCMI.next();
					if (lclFirst) {
						lclFirst = false;
					} else {
						lclBW.print(", ");
					}
					lclBW.print(lclCM.getPrimitiveMutatorArgumentName());
				}
				lclBW.println(");");
				lclBW.println("\t\treturn (" + lclVariable + " == null) ? null : " + lclVariable + ".getUserFacing();");
				lclBW.println("\t}");
				lclBW.println();
				
				if (lclMUK.getClassMembers().size() == 1) {
					ClassMember lclCM = lclMUK.getClassMembers().get(0);
					if (lclCM.getMemberType() == Integer.class) {
						lclBW.print("\tpublic " + getInterfaceClassName() + ' ' + lclMUK.generateFactoryMethodName() + "(");
						lclBW.print("int " + lclCM.getPrimitiveMutatorArgumentName());
						lclBW.println(") {");
						
						lclBW.print("\t\treturn " + lclMUK.generateFactoryMethodName() + "(");
						lclBW.print("Integer.valueOf(" + lclCM.getPrimitiveMutatorArgumentName() + ")");
						lclBW.println(");");
						lclBW.println("\t}");
						lclBW.println();
					}
				}
					
			}
			lclBW.println("\t@Override");
			lclBW.println("\tpublic " + lclICN + "[] createArray(int argSize) {");
			lclBW.println("\t\treturn new " + lclICN + "[argSize];");
			lclBW.println("\t}");
			lclBW.println();
			
			/* Create factory methods for each MultipleLookUp */
			/* FIXME: Previous to this, filter out MultipleLookUps based on unmapped fields */
			
			if (hasMultipleLookUpSpecifications()) {
				for (MultipleLookUpSpecification lclMLUS : getMultipleLookUpSpecifications()) {
					lclBW.print("\tpublic " + MULTIPLE_LOOK_UP_COLLECTION_INTERFACE.getName() + "<" + lclICN + "> ");
					lclBW.print(lclMLUS.getFactoryMethodName());
					lclBW.print('(');
					lclBW.print(lclMLUS.getFactoryMethodArguments());
					lclBW.println(") {");
					lclBW.println("\t\treturn acquireForQuery(");
					lclBW.println("\t\t\tnew " + MULTIPLE_LOOK_UP_COLLECTION_CLASS.getName() + "<>(),");
					lclBW.println("\t\t\tnew "+ ImplicitTableDatabaseQuery.class.getName() + "(");
					lclBW.println("\t\t\t\t\"" + lclMLUS.getQuerySQLClause() + "\",");
					lclBW.println("\t\t\t\t" + lclMLUS.getQueryParameters());
					lclBW.println("\t\t\t\t)");
					lclBW.println("\t\t\t);");
					lclBW.println("\t}");
					lclBW.println();
				}
			}
			
			/* If we are supposed to create factory methods to extract keys from HttpRequests, do so. */
			if (generateHttpRequestFactory()) {
				MappedUniqueKey lclPK = getPrimaryKey();
				Validate.notNull(lclPK, "We could not find a primary key for " + getTableName() + " (possibly some of its columns were unmapped)");
				
				lclBW.println("\tpublic " + lclICN + " fromHttpRequest(" + HTTP_REQUEST_CLASS.getName() + " argRequest) {");
				lclBW.println("\t\torg.apache.commons.lang3.Validate.notNull(argRequest);");
				lclBW.print("\t\treturn fromHttpRequest(argRequest");
				Iterator<ClassMember> lclCMI = lclPK.createClassMemberIterator();
				while (lclCMI.hasNext()) {
					ClassMember lclCM = lclCMI.next();
					String lclColumnName = lclCM.getDatabaseColumn().getName().toLowerCase();
					String lclConvertedClassName = getTableName().getTableName().toLowerCase();
					String lclAN;
					if (lclColumnName.startsWith(lclConvertedClassName) == false) {
						lclAN = lclConvertedClassName + '_' + lclColumnName;
					} else {
						lclAN = lclColumnName;
					}
					lclBW.print(", ");
					lclBW.print('"');
					lclBW.print(lclAN);
					lclBW.print('"');
				}
				lclBW.println(");");
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.print("\tpublic " + lclICN + " fromHttpRequest(" + HTTP_REQUEST_CLASS.getName() + " argRequest");
				lclCMI = lclPK.createClassMemberIterator();
				while (lclCMI.hasNext()) {
					ClassMember lclCM = lclCMI.next();
					String lclAN = "arg" + lclCM.getBaseMemberName() + "ParameterName";
					lclBW.print(", String ");
					lclBW.print(lclAN);
				}
				lclBW.println(") {");
				lclBW.println("\t\torg.apache.commons.lang3.Validate.notNull(argRequest);");
				StringBuilder lclMethodName = new StringBuilder("for");
				StringBuilder lclArguments = new StringBuilder();
				lclCMI = lclPK.createClassMemberIterator();
				while (lclCMI.hasNext()) {
					ClassMember lclCM = lclCMI.next();
					String lclAN = "arg" + lclCM.getBaseMemberName() + "ParameterName";
					String lclVNS = "lcl" + lclCM.getBaseMemberName() + "String";
					lclBW.println("\t\tString " + lclVNS + " = argRequest.getParameter(" + lclAN + ");");
					lclBW.println("\t\tif (" + StringUtils.class.getName() + ".isBlank(" + lclVNS + ")) {");
					lclBW.println("\t\t\treturn null;");
					lclBW.println("\t\t}");
				}
				
				lclBW.println("\t\ttry {");
				lclCMI = lclPK.createClassMemberIterator();
				while (lclCMI.hasNext()) {
					ClassMember lclCM = lclCMI.next();
					String lclVN = "lcl" + lclCM.getBaseMemberName();
					String lclVNS = "lcl" + lclCM.getBaseMemberName() + "String"; // oops, we're redoing something we did in the previous loop
					Class<?> lclT = lclCM.getMemberType();
					String lclTN = OpalUtility.generateTypeName(lclT);
					lclBW.println("\t\t\t" + lclTN + ' ' + lclVN + " = " + OpalUtility.getCodeToConvert(lclT, String.class, lclVNS, true) + ';');
					lclMethodName.append(lclCM.getBaseMemberName());
					if (lclArguments.length() > 0) {
						lclArguments.append(", ");
					}
					lclArguments.append(lclVN);
				}
				lclBW.println("\t\t\treturn " + lclMethodName.toString() + "(" + lclArguments.toString() + ");");
				lclBW.println("\t\t} catch (Exception lclE) {");
				// We could warn, but we'll just get a lot of crap from SQL injection attempts
				lclBW.println("\t\t\treturn null;");
				lclBW.println("\t\t}");
				lclBW.println("\t}");
				lclBW.println();
				
				
				boolean lclUseUniqueStrings = lclPK.sizeClassMember() > 1;
				String lclDefaultKeyName;
				if (lclUseUniqueStrings) {
					lclDefaultKeyName = getTableName().getTableName().toLowerCase() + "_us";
				} else {
					ClassMember lclCM = lclPK.createClassMemberIterator().next();
					String lclColumnName = lclCM.getDatabaseColumn().getName().toLowerCase();
					String lclConvertedClassName = getTableName().getTableName().toLowerCase();
					
					if (lclColumnName.startsWith(lclConvertedClassName)) {
						lclDefaultKeyName = lclColumnName;
					} else {
						lclDefaultKeyName = lclConvertedClassName + '_' + lclColumnName;
					}
				}
				
				lclBW.println("\tpublic <T extends " + ACQUIRE_COLLECTION_INTERFACE.getName() + "<? super " + lclICN + ">> T acquireFromHttpRequest(T argCollection, " + HTTP_REQUEST_CLASS.getName() + " argRequest, String argParameterName) {");
				lclBW.println("\t\torg.apache.commons.lang3.Validate.notNull(argCollection);");
				lclBW.println("\t\torg.apache.commons.lang3.Validate.notNull(argRequest);");
				lclBW.println("\t\torg.apache.commons.lang3.Validate.notEmpty(argParameterName);");
				lclBW.println("\t\tString[] lclValues = argRequest.getParameterValues(argParameterName);");
				lclBW.println("\t\tif (lclValues == null || lclValues.length == 0) {");
				lclBW.println("\t\t\treturn argCollection;");
				lclBW.println("\t\t}");
				lclBW.println("\t\tfor (String lclValueUntrimmed : lclValues) {");
				lclBW.println("\t\t\tString lclValue = " + StringUtils.class.getName() + ".trimToNull(lclValueUntrimmed);");
				lclBW.println("\t\t\tif (lclValue != null) {");
				if (lclUseUniqueStrings) {
					lclBW.println("\t\t\t\t" + lclICN + " lclResult = forUniqueString(lclValue);");
					lclBW.println("\t\t\t\tif (lclResult != null) {");
					lclBW.println("\t\t\t\t\targCollection.add(lclResult);");
					lclBW.println("\t\t\t\t}");
				} else {
					ClassMember lclPrimaryKeyCM = lclPK.createClassMemberIterator().next();
					Class<?> lclT = lclPrimaryKeyCM.getMemberType();
					String lclTN = OpalUtility.generateTypeName(lclT);
					String lclVN = "lcl" + lclPrimaryKeyCM.getBaseMemberName();
					lclBW.println("\t\t\t\ttry {");
					lclBW.println("\t\t\t\t\t" + lclTN + ' ' + lclVN + " = " + OpalUtility.getCodeToConvert(lclT, String.class, "lclValue", true) + ';');
					lclBW.println("\t\t\t\t\t" + lclICN + " lclResult = for" + lclPrimaryKeyCM.getBaseMemberName() + "(" + lclVN + ");");
					lclBW.println("\t\t\t\t\tif (lclResult != null) {");
					lclBW.println("\t\t\t\t\t\targCollection.add(lclResult);");
					lclBW.println("\t\t\t\t\t}");
					lclBW.println("\t\t\t\t} catch (Exception e) {");
					// We could warn, but we'll just get a lot of crap from SQL injection attempts
					lclBW.println("\t\t\t\t\t// Swallow");
					lclBW.println("\t\t\t\t}");
				}
				lclBW.println("\t\t\t}");
				lclBW.println("\t\t}");
				lclBW.println("\t\treturn argCollection;");
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\tpublic <T extends " + ACQUIRE_COLLECTION_INTERFACE.getName() + "<? super " + lclICN + ">> T acquireFromHttpRequest(T argCollection, " + HTTP_REQUEST_CLASS.getName() + " argRequest) {");
				lclBW.println("\t\treturn acquireFromHttpRequest(argCollection, argRequest, \"" + lclDefaultKeyName + "\");");
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\tpublic " + ACQUIRE_COLLECTION_INTERFACE.getName() + '<' + lclICN + "> multipleFromHttpRequest(" + HTTP_REQUEST_CLASS.getName() + " argRequest, String argParameterName) {");
				lclBW.println("\t\treturn acquireFromHttpRequest(new " + HTTP_MULTIPLE_INTERMEDIATE_COLLECTION_CLASS.getName() + "<>(), argRequest, argParameterName); // checks parameters for nullity");
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\tpublic " + ACQUIRE_COLLECTION_INTERFACE.getName() + '<' + lclICN + "> multipleFromHttpRequest(" + HTTP_REQUEST_CLASS.getName() + " argRequest) {");
				lclBW.println("\t\treturn multipleFromHttpRequest(argRequest, \"" + lclDefaultKeyName + "\");");
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\tpublic " + lclICN + "[] arrayFromHttpRequest(" + HTTP_REQUEST_CLASS.getName() + " argRequest, String argParameterName) {");
				lclBW.println("\t\t" + HTTP_MULTIPLE_INTERMEDIATE_COLLECTION_CLASS.getName() + '<' + lclICN + "> lclUs = acquireFromHttpRequest(new " + HTTP_MULTIPLE_INTERMEDIATE_COLLECTION_CLASS.getName() + "<>(), argRequest, argParameterName); // checks parameters for nullity");
				lclBW.println("\t\treturn lclUs.toArray(createArray(lclUs.size()));");
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\tpublic " + lclICN + "[] arrayFromHttpRequest(" + HTTP_REQUEST_CLASS.getName() + " argRequest) {");
				lclBW.println("\t\treturn arrayFromHttpRequest(argRequest, \"" + lclDefaultKeyName + "\");");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (isEphemeral() == false) {
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + lclICN + " forUniqueString(String argUniqueString) {");
				lclBW.println("\t\tif (argUniqueString == null) {");
				lclBW.println("\t\t\treturn null;");
				lclBW.println("\t\t}");
				lclBW.println("\t\t" + lclOCN + " lclOpal = getOpalFactory().forUniqueString(argUniqueString);");
				lclBW.println("\t\treturn lclOpal != null ? lclOpal.getUserFacing() : null;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			lclBW.println("}");
		} // Closes lclBW via try-with-resources
	}
	
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
	
	protected void createInterface() throws IOException {
		String lclInterfaceClassFileName = StringUtility.makeFilename(getSourceDirectory(), getInterfacePackageName(), getInterfaceClassName());
		
		File lclInterfaceClassFile = new File(lclInterfaceClassFileName);
		if (lclInterfaceClassFile.exists()) {
			complain(MessageLevel.Info, lclInterfaceClassFileName + " already exists, so we are not generating it.");
		} else {
			try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclInterfaceClassFile)))) {
				JavadocHelper lclJD = new JavadocHelper(lclBW /*, 80 */);
				
				
				List<Class<?>> lclSuperinterfaces = new ArrayList<>();
				
				for (ClassMember lclCM : getClassMembers()) {
					if (lclCM.isMapped()) {
						if ("getName".equals(lclCM.getObjectAccessorName()) && lclCM.getMemberType() == String.class) {
							lclSuperinterfaces.add(com.opal.types.Named.class);
						} else if ("getShortName".equals(lclCM.getObjectAccessorName()) && lclCM.getMemberType() == String.class) {
							lclSuperinterfaces.add(com.opal.types.ShortNamed.class);
						} else if ("getCode".equals(lclCM.getObjectAccessorName()) && lclCM.getMemberType() == String.class) {
							lclSuperinterfaces.add(com.opal.types.Coded.class);
						}
					}
				}
				
				lclSuperinterfaces.sort(Comparator.comparing(Class::getName));
				
				lclBW.println("package " + getInterfacePackageName() + ';');
				lclBW.println();
				
				StringBuilder lclExtends = new StringBuilder();
				for (Class<?> lclImport : lclSuperinterfaces) {
					lclBW.println("import " + lclImport.getName() + ';');
					
					lclExtends.append(lclImport.getSimpleName()).append(", ");
				}
				
				if (lclSuperinterfaces.isEmpty() == false) {
					lclBW.println();
				}
				
				lclBW.println("import " + getFullyQualifiedUserFacingClassName() + ';');
				lclBW.println();
				
				lclJD.start(0);
				lclJD.para("This interface may be changed at will.");
				lclJD.para("This interface and the {@link " + getFactoryClassName() + "} class are the only two automatically generated files for this type that are intended to be referenced in user code. Other automatically generated files are intended to be invisible to the user's code and may change (or disappear entirely) when newer versions of the Opal generating code is used.");
				lclJD.author(getAuthor());
				lclJD.end();
				
				lclBW.println();
				
				if (isDeprecated()) {
					lclBW.println("@Deprecated");
				}
				lclBW.println("public interface " + getInterfaceClassName() + " extends " + lclExtends + getUserFacingClassName() + " {"); // Note that lclExtneds will always end with ", "
				lclBW.println("\t/* Developers may add default and static methods to this interface without fear of them being overwritten");
				lclBW.println("\tby subsequent re-generation of the Opals (and related classes). */");
				lclBW.println("}");
			}
		}
	}
	
	protected void createUserFacing() throws IOException {
		String lclUserFacingClassFileName = StringUtility.makeFilename(getSourceDirectory(), getUserFacingPackageName(), getUserFacingClassName());
		
		File lclUserFacingClassFile = new File(lclUserFacingClassFileName);
		
		try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclUserFacingClassFile)))) {
			JavadocHelper lclJD = new JavadocHelper(lclBW /*, 80 */);
			
			// System.out.println("Creating interface file for " + getUserFacingPackageName());
			
			lclBW.println("package " + getUserFacingPackageName() + ';');
			lclBW.println();
			lclBW.println();
			
			/* Does this class have an intrinsic order? */
			
			ClassMember lclOrderingMember = getOrderingMember();
			
			/* Class Javadoc */
			
			lclJD.start(0);
			lclJD.para("represents a {@code " + getTypeName() + "} from the persistent store");
			lclJD.para("This interface was automatically generated to represent objects loaded from {@code " + getTableName() + "}.  Do not modify this class directly; instead, alter the permanent storage on which it is based, the Opal configuration file for the project, and/or the {@link " + getFullyQualifiedInterfaceClassName() + "}, and then regenerate the opals.  Any changes made to this file will be lost if the opals are regenerated.");
			lclJD.para("To create objects of this type from persistent data, use one of the various {@code forXXX} methods on the {@link " + getFullyQualifiedFactoryClassName() + "} factory class.  To create new instances, use the {@code create()} method on the factory.");
			lclJD.para("Objects that implement this interface must provide Opal thread semantics; among other things this means that all modification of such objects must be done with an active {@link " + TransactionContext.class.getName() + "}.");
			lclJD.para("This interface is not meant to be referenced in user code.  Only the {@link " + getFullyQualifiedInterfaceClassName() + "} and the {@link " + getFullyQualifiedFactoryClassName() + "} class should be referenced directly.");
			lclJD.author(getAuthor());
			// lclJD.version(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(new Date()) + " (automatically generated)");
			lclJD.end();
			
			final String lclUFCN = getUserFacingClassName();
			final String lclOCN = getOpalClassName();
			
			lclBW.println("@javax.annotation.Generated(\"com.opal\")");
			if (isDeprecated()) {
				lclBW.println("@Deprecated");
			}
			
			lclBW.println("@" + Creatability.class.getName() + "(creatable = " + isCreatable() + ")");
			lclBW.println("@" + Updatability.class.getName() + "(updatable = " + isUpdatable() + ")");
			
			lclBW.print("public interface " + lclUFCN + " extends ");
			if (hasSuperclass()) {
				lclBW.print(getSuperclassKey().getTargetMappedClass().getFullyQualifiedInterfaceClassName());
			} else if (isEphemeral()) {
				lclBW.print(UserFacing.class.getName());
			} else {
				lclBW.print(IdentityUserFacing.class.getName());
			}
			
			if (lclOrderingMember != null) {
				lclBW.print(", Comparable<" + getFullyQualifiedInterfaceClassName() + '>');
			}
			
			/* If this object can act as a supplier of another object (due to the existence of a foreign key), add the
			 * appropriate interface marking that.  Note that these are not instances of Supplier<T>, because an object
			 * can't implement Supplier (or any generic interface) twice with different type variables.
			 */
			for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
				if (lclMFK.shouldSourceExtendSupplierInterface()) {
					lclBW.print(", " + lclMFK.getTargetMappedClass().getFullyQualifiedUserFacingClassName() + "." + lclMFK.getTargetMappedClass().getSupplierInterfaceName()); // FIXME: Helper method
				}
			}
			
			lclBW.println(" {");
			
			lclBW.println();
			
			/* Every UserFacing class will have output methods to display its internal data as a series of
			 * newline-separated FIELD_NAME=field_value pairs.  They are mainly intended to be used
			 * for debugging purposes as the output is utterly unformatted. */ 
			
			/* lclJD.start(1);
			lclJD.para("outputs the internal data of the object to the specified {@link PrintStream} " +
				"in lines of the form \"FIELD_NAME=field_value\".");
			lclJD.para("This is primarily intended as an aid to debugging since the output is unformatted.");
			lclJD.param("argPS", "the {@code PrintStream} to which the object's contents are to be written.", false);
			lclJD.throwstag("IOException", "when problems occur writing to {@code argPS}");
			lclJD.end();
			lclBW.println("\tpublic void output(PrintStream argPS) throws IOException;");
			lclBW.println();
			
			lclJD.start(1);
			lclJD.para("outputs the internal data of the object to the specified {@code PrintWriter} " +
				"in lines of the form \"FIELD_NAME=field_value\".");
			lclJD.para("This is primarily intended as an aid to debugging since the output is unformatted.");
			lclJD.param("argPW", "the {@code PrintWriter} to which the object's contents are to be written.", false);
			lclJD.throwstag("IOException", "when problems occur writing to {@code argPW}");
			lclJD.end();
			lclBW.println("\tpublic void output(PrintWriter argPW) throws IOException;");
			lclBW.println(); */

			if (GENERATE_FIELD_CLASS) {
				lclBW.println("\tpublic static class FIELD extends " + lclOCN + ".FIELD {}");
				lclBW.println();
			}
			
			/* Create the appropriate accessors and mutators for the internal data */
			if (getClassMemberCount() == 0) {
				lclBW.println("\t/* No internal data. */");
			} else {
				lclBW.println("\t/* Accessors and mutators for internal data. */");
			}
			
			/* Loop through the members belonging to this class */
			
			/* FIXME: I think that we do not properly account for members with protected access here, in that we seem
			 * to always create the accessor and mutators, regardless of visibility.  Something else to fix, I guess.
			 */
			
			for (ClassMember lclCM : getClassMembers()) {
				/* If it is mapped...*/
				if (lclCM.isMapped()) {
					if (lclCM.appearsInUserFacing(this)) {
						final Class<?> lclMemberType = lclCM.getMemberType();
						final String lclMemberTypeName = lclCM.getMemberParameterizedTypeName(); // OpalUtility.generateTypeName(lclMemberType);
						final Class<?> lclPrimitiveType = ClassUtils.wrapperToPrimitive(lclMemberType);
						
						/* Generate the accessors.  First we generate the "object accessor" that returns
						 * the value as an Object (e.g., integer-valued fields come back as Integers, not ints */
						lclJD.start(1);
						lclJD.para("object accessor for the {@code " + lclCM.getBaseMemberName() + "}");
						lclJD.para("The {@code " + lclCM.getBaseMemberName() + "} field is a direct mapping of the " +
								"{@code " + lclCM.getDatabaseColumn().getName() + "} field in {@code " + getTableName() + "}.");
						if (lclPrimitiveType != null) {
							lclJD.para("This method returns the current value as an Object.  To retrieve the value as a primitive, use the " + lclCM.getPrimitiveAccessorName() + "() method.");
						}
						lclJD.returntag("an object value of {@code " + lclCM.getBaseMemberName() + "} (of the current {@link " + TransactionContext.class.getName() + "})", lclCM.isNullAllowed());
						lclJD.end();
						lclCM.outputAnnotations(lclBW, this);
						if (lclCM.isDeprecated() && isDeprecated() == false) {
							lclBW.println("\t@Deprecated");
						}
						lclBW.println("\tpublic " + lclMemberTypeName + ' ' + lclCM.getObjectAccessorName() + "();");
						lclBW.println();
						
						if (lclMemberType == Trinary.class) {
							if (lclCM.isDeprecated() && isDeprecated() == false) {
								lclBW.println("\t@Deprecated");
							}
							lclBW.println("\tpublic boolean " + lclCM.getObjectAccessorName() + "(boolean argValueToSubstituteIfUnknown);");
							lclBW.println();
						}
						
						if (lclCM.hasInverseAccessor(false) && lclMemberType == Boolean.class) {
							if (lclCM.isDeprecated() && isDeprecated() == false) {
								lclBW.println("\t@Deprecated");
							}
							lclBW.println("\tdefault " + lclMemberTypeName + ' ' + lclCM.getInverseObjectAccessorMethodNameOrDefault() + "() {");
							lclBW.println("\t\tBoolean lclB = " + lclCM.getObjectAccessorName() + "();");
							if (lclCM.isNullAllowed()) {
								lclBW.println("\t\treturn lclB != null ? (lclB.booleanValue() ? Boolean.FALSE : Boolean.TRUE) : null;");
							} else {
								lclBW.println("\t\treturn lclB.booleanValue() ? Boolean.FALSE : Boolean.TRUE;");
							}
							lclBW.println("\t}");
							lclBW.println();
							
						}
						if (lclCM.isNullAllowed()) {
							lclJD.start(1);
							lclJD.para("object accessor for the {@code " + lclCM.getBaseMemberName() + "} with substitution for a null value");
							lclJD.para("The {@code " + lclCM.getBaseMemberName() + "} field is a direct mapping of the " +
									"{@code " + lclCM.getDatabaseColumn().getName() + "} database column in the " +
									"table {@code " + getTableName() + "}.");
							lclJD.para("This method returns the current value if it is not {@code null}, or {@code argStringToSubstituteIfNull} " +
									"if the current value is {@code null}.");
							lclJD.param("argStringToSubstituteIfNull", "the value to return if the {@code " + lclCM.getBaseMemberName() + "} is {@code null}.");
							lclJD.returntag("an object value of {@code " + lclCM.getBaseMemberName() + "} (of the current {@link " + TransactionContext.class.getName() + "}) " +
									"if it is not {@code null}, or {@code argStringToSubstituteIfNull} if it is {@code null}.");
							lclJD.end();
							if (lclCM.isDeprecated() && isDeprecated() == false) {
								lclBW.println("\t@Deprecated");
							}
							lclBW.println("\tdefault " + String.class.getName() + ' ' + lclCM.getObjectAccessorName() + "(" + String.class.getName() + " argStringToSubstituteIfNull) {");
							lclBW.println("\t\t" + lclMemberTypeName + " lclO = " + lclCM.getObjectAccessorName() + "();");
							String lclNotNullReturn = (lclMemberType == String.class) ? "lclO" : "String.valueOf(lclO)";
							lclBW.println("\t\treturn lclO != null ? " + lclNotNullReturn + " : argStringToSubstituteIfNull;");
							lclBW.println("\t}");
							lclBW.println();
							
							lclBW.println();
						}
						
						/* If the field has a corresponding primitive type (like Integer, unlike String), then
						 * we generate an accessor that returns the primitive type as well. */
						
						if (lclPrimitiveType != null) {
							lclJD.start(1);
							lclJD.para("primitive accessor for the {@code " + lclCM.getBaseMemberName() + "}");
							lclJD.para("The {@code " + lclCM.getBaseMemberName() + "} field is a direct mapping of the " + 
								"{@code " + lclCM.getDatabaseColumn().getName() + "} database column in the " +
								"table {@code " + getTableName() + "}.");
							
							lclJD.para("This method returns the value as a primitive (for example, as an " +
								"{@code int} rather than an {@code Integer}; " +
								"to retrieve the value as an object, use the " + lclCM.getObjectAccessorName() + "() method.");
							
							if (lclCM.isNullAllowed()) {
								lclJD.para("The underlying database table allows a {@code NULL} value for this column; calling this method " +
									"when the value is null will result in an Exception.  To test for a null value, use the Object accessor mentioned above.");
							}
							lclJD.returntag("the primitive value of {@code " + lclCM.getBaseMemberName() + "} (of the current {@link " + TransactionContext.class.getName() + "})");
							if (lclCM.isNullAllowed()) {
								lclJD.throwstag(NullValueException.class.getName(), "when the internal value is null");
							}
							lclJD.end();
							
							if (lclCM.isDeprecated() && isDeprecated() == false) {
								lclBW.println("\t@Deprecated");
							}
							lclBW.print("\tdefault " + lclPrimitiveType.getName() + ' ' + lclCM.getPrimitiveAccessorName() + "()");
							if (lclCM.isNullAllowed()) {
								lclBW.print(" throws " + NullValueException.class.getName());
							}
							lclBW.println(" {");
							lclBW.println("\t\t" + lclMemberTypeName + " lclO = " + lclCM.getObjectAccessorName() + "();");
							if (lclCM.isNullAllowed()) {
								lclBW.println("\t\tif (lclO == null) {");
								lclBW.println("\t\t\tthrow new " + NullValueException.class.getName() + "(\"The internal value is null and cannot be returned as a primitive.\");");
								lclBW.println("\t\t}");
							}
							lclBW.println("\t\treturn lclO." + TypeUtility.getPrimitiveAccessor(lclMemberType) + "();");
							lclBW.println("\t}");
							lclBW.println();
							
							if (lclCM.hasInverseAccessor(false)  && lclMemberType == Boolean.class) {
								if (lclCM.isDeprecated() && isDeprecated() == false) {
									lclBW.println("\t@Deprecated");
								}
								lclBW.print("\tdefault " + lclPrimitiveType.getName() + ' ' + lclCM.getInverseAccessorMethodNameOrDefault() + "()");
								if (lclCM.isNullAllowed()) {
									lclBW.print(" throws " + NullValueException.class.getName());
								}
								lclBW.println(" {");
								lclBW.println("\t\treturn !" + lclCM.getPrimitiveAccessorName() + "();");
								lclBW.println("\t}");
								lclBW.println();
							}
							
							if (lclCM.isNullAllowed()) {
								if (lclCM.isDeprecated() && isDeprecated() == false) {
									lclBW.println("\t@Deprecated");
								}
								lclBW.println("\tdefault " + lclPrimitiveType.getName() + ' ' + lclCM.getPrimitiveAccessorName() + "(" + lclPrimitiveType.getName() + " argValueToSubstituteIfNull) {");
								lclBW.println("\t\t" + lclMemberTypeName + " lclO = " + lclCM.getObjectAccessorName() + "();");
								lclBW.println("\t\treturn (lclO != null) ? lclO." + TypeUtility.getPrimitiveAccessor(lclMemberType) + "() : argValueToSubstituteIfNull;");
								lclBW.println("\t}");
								lclBW.println();
								
								if (lclCM.isDeprecated() && isDeprecated() == false) {
									lclBW.println("\t@Deprecated");
								}
								lclBW.println("\tdefault " + String.class.getName() + ' ' + lclCM.getPrimitiveAccessorName() + "(" + String.class.getName() + " argStringToSubstituteIfNull) {");
								lclBW.println("\t\t" + lclMemberTypeName + " lclO = " + lclCM.getObjectAccessorName() + "();");
								lclBW.println("\t\treturn (lclO != null) ? String.valueOf(lclO) : argStringToSubstituteIfNull;"); // TODO: Handle dates differently?
								lclBW.println("\t}");
								lclBW.println();
							}
							
							/* A primitive-valued accessor has no way to return the correct value if the internal
							 * value is null; they will throw a NullValueException if they are asked to do so.  This
							 * is a checked exception since applications should always deal properly with missing data.
							 * If the underlying persistent store does does not allow null data (like a NOT NULL
							 * column in a  relational database) then the method will never throw a NullValueException
							 * and it will not be marked as such.
							 */
						}
						
						/* If the mapped class as a whole is updatable and the field itself is updatable then
						 * we generate a mutator for the field.
						 */
						if ((isCreatable() || (isUpdatable() && lclCM.isUpdatable())) && (lclCM.isComputed() == false)) {
							/* First we generate the version that takes an Object. */
							lclJD.start(1);
							lclJD.para("sets the {@code " + lclCM.getBaseMemberName() + "} to the value of {@code " + lclCM.getObjectMutatorArgumentName() + "}");
							lclJD.param(lclCM.getObjectMutatorArgumentName(), "the new value of {@code " + lclCM.getBaseMemberName() + "}.", lclCM.isNullAllowed());
							
							if (generateFluentMutators()) {
								lclJD.returntag("itself, so that mutator calls can be chained fluently");
							}
							
							if (lclCM.isNullAllowed()) {
								if (!lclCM.getDatabaseColumn().isNullable()) {
									lclJD.para("Even though this field allows nulls, the database column {@code " + lclCM.getDatabaseColumn().getName() + "} to which it is mapped does not allow nulls.");
									lclJD.para("This is probably a mistake, and comitting transactions that set this field to null may result in exceptions being thrown.");
								}
							} else {
								if (!lclCM.getDatabaseColumn().isNullable()) {
									lclJD.para("The database column {@code " + lclCM.getDatabaseColumn().getName() + "} to which this field is mapped is {@code NOT NULL}.");
								} else {
									lclJD.para("Even though the database column {@code " + lclCM.getDatabaseColumn().getName() + "} \"physically\" allows nulls, this field does not.");
								}
								lclJD.throwstag(IllegalNullArgumentException.class.getName(), "if " + lclCM.getObjectMutatorArgumentName() + " is null");
							}
							
							/* If the field is a string field, then an ArgumentTooLongException may be thrown
							 * if the length of the argument exceeds what may be stored in the field. */
							
							if (lclCM.getMemberType() == String.class && lclCM.getDatabaseColumn().getLength() > 0) {
								lclJD.throwstag(ArgumentTooLongException.class.getName(), "if {@code " + lclCM.getObjectMutatorArgumentName() + "} is longer than " + lclCM.getDatabaseColumn().getLength() + " characters");
								lclJD.para("The database column {@code " + lclCM.getDatabaseColumn().getName() + "} is limited to " + lclCM.getDatabaseColumn().getLength() + " characters.");
							}
							lclJD.end();
							
							lclCM.outputAnnotations(lclBW, this);
							printRequiresActiveTransactionAnnotation(lclBW, 1);
							lclBW.println("\tpublic " + determineMutatorReturnType(getFullyQualifiedInterfaceClassName()) + ' ' + lclCM.getObjectMutatorName() + "(" + lclMemberTypeName + ' ' + lclCM.getObjectMutatorArgumentName() + ");");
							lclBW.println();
							if (lclPrimitiveType != null) {
								/* The mutator that takes a primitive value.  This is significantly less complicated
								 * because we don't have to deal with null values or Strings. */
								
								lclJD.start(1);
								lclJD.para("sets the {@code " + lclCM.getBaseMemberName() + "} to the value of {@code " + lclCM.getPrimitiveMutatorArgumentName() + "}");
								lclJD.param(lclCM.getPrimitiveMutatorArgumentName(), "the new value of {@code " + lclCM.getBaseMemberName() + "}");
								
								if (generateFluentMutators()) {
									lclJD.returntag("itself, so that mutators may be chained fluently");
								}
								
								lclJD.end();
								if (lclCM.isDeprecated() && isDeprecated() == false) {
									lclBW.println("\t@Deprecated");
								}
								printRequiresActiveTransactionAnnotation(lclBW, 1);
								lclBW.println("\tpublic " + determineMutatorReturnType(getFullyQualifiedInterfaceClassName()) + ' ' + lclCM.getPrimitiveMutatorName() + "(" + lclPrimitiveType.getName() + ' ' + lclCM.getPrimitiveMutatorArgumentName() + ");");
								lclBW.println();
							}
						}
					}
				}
			}
			
			for (ClassMember lclCM : getLargeClassMembers()) {
				/* FIXME: Make this work at all */
				/* FIXME: Ensure that class member deprecation works */
				/* If it is mapped . . .*/
				if (lclCM.isMapped()) {
					final Class<?> lclMemberType = lclCM.getMemberType();
					final String lclMemberTypeName = OpalUtility.generateTypeName(lclMemberType);
					
					/* Generate the accessors.  First we generate the "object accessor" that returns
					 * the value as an Object (i.e., integer-valued fields come back as Integers, not ints */
					/* lclJD.start(1);
					lclJD.para("object accessor for the {@code " + lclCM.getBaseMemberName() + "}");
					lclJD.para("The {@code " + lclCM.getBaseMemberName() + "} field is a direct mapping of the " +
							"{@code " + lclCM.getDatabaseColumn().getName() + "} database column in the " +
							"table {@code " + getTableName() + "}" + '.');
					lclJD.returntag("an object value of {@code " + lclCM.getBaseMemberName() + "} (of the current {@link com.opal." + TransactionContext.class.getName() + "})", lclCM.isNullAllowed());
					lclJD.end();
					lclBW.println(); */
					lclBW.println("\tpublic " + lclMemberTypeName + ' ' + lclCM.getPrimitiveAccessorName() + "();");
					
					/* If the mapped class as a whole is updatable and the field itself is updatable then
					 * we generate a mutator for the field. */
					
					if (isUpdatable() && lclCM.isUpdatable()) {
						/* First we generate the version that takes an Object. */
						// lclBW.println("/**");
						// lclBW.println("sets the {@code " + lclCM.getBaseMemberName() + "} to the value of {@code " + lclCM.getObjectMutatorArgumentName() + "}</p>");
						// lclBW.println("@param " + lclCM.getObjectMutatorArgumentName() + " the new value of " + lclCM.getBaseMemberName() + '.');
						// if (lclCM.isNullAllowed()) {
							// lclBW.println("May be null.");
							// if (!lclCM.getDatabaseColumn().isNullable()) {
								// lclBW.println("<p>Even though this field allows nulls, the database column {@code " + lclCM.getDatabaseColumn().getName() + "} to which it is mapped does not allow nulls.");
								// lclBW.println("This is probably a mistake and comitting transactions that set this field to null will result in exceptions being thrown.</p>");
							// }
						// } else {
							// lclBW.println("May not be null.");
							// if (!lclCM.getDatabaseColumn().isNullable()) {
								// lclBW.println("<p>The database column {@code " + lclCM.getDatabaseColumn().getName() + "} to which this field is mapped is {@code NOT NULL}.</p>");
							// } else {
								// lclBW.println("<p>Even though the database column {@code " + lclCM.getDatabaseColumn().getName() + "} \"physically\" allows nulls, this field does not.</p>");
							// }
							// lclBW.println("@throws IllegalNullArgumentException if " + lclCM.getObjectMutatorArgumentName() + " is null</p>");
						// }
						
						// /* If the field is a string field, then an ArgumentTooLongException may be thrown
						 // * if the length of the argument exceeds what may be stored in the field. */
						
						// if (lclCM.getMemberType() == String.class) {
							// lclBW.println("@throws ArgumentTooLongException if {@code " + lclCM.getObjectMutatorName() + "} is longer than " + lclCM.getDatabaseColumn().getLength() + " characters");
							// lclBW.println("<p>The database column {@code  " + lclCM.getDatabaseColumn().getName() + "} is limited to " + lclCM.getDatabaseColumn().getLength() + " characters.</p>");
						// }
						// lclBW.println("*/");
						
						/* The actual mutator */
						lclBW.println("\tpublic " + determineMutatorReturnType(getUserFacingClassName()) + ' ' + lclCM.getPrimitiveMutatorName() + "(" + lclMemberTypeName + ' ' + lclCM.getObjectMutatorArgumentName() + ");");
						lclBW.println();
					}
				}
			}
			/* Create accessors and mutators for this object's member variables that are references to
			 * other UserFacing objects.
			 */
			
			/* Loop through its foreign keys */ 
			
			for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
				/* FIXME: Explain this! */
				if (lclMFK.representsPolymorphism()) {
					continue;
				}
				
				/* If that reference/foreign key is marked as "protected" in the configuration file,
				 * then we won't put accessors (or mutators) in the interface.  We will, however, put them
				 * in the default implementation class so that a subclass can choose to make them public.
				 * 
				 * The idea behind this is that a table may contain rows representing different subclasses
				 * of a "main" type, one of which has a reference.  We don't want the reference to be part
				 * of the overall interface (because not every subclass would support it), but we do want
				 * to generate the "behind-the-scenes" machinery to handle it.  In this case that reference
				 * would be marked as "Protected" and the specific subclass that used it could override
				 * the accessor and mutator from the default implementation to make them public.
				 */
				if ("Protected".equalsIgnoreCase(lclMFK.getSourceFieldAccess())) {
					continue;
				}
				if (lclMFK.appearsInSourceUserFacing() == false) {
					continue;
				}
				
				/* Extract the class to which it is a foreign key */
				MappedClass lclTMC = lclMFK.getTargetMappedClass();
				String lclT = lclTMC.getFullyQualifiedInterfaceClassName();
				String lclA = "arg" + lclMFK.getRoleSourceFieldName();
				
				/* Create the accessor */
				lclJD.start(1);
				lclJD.returntag("the {@code " + lclT + "}");
				lclJD.para("The returned {@code " + lclT + "} is the {@link " + UserFacing.class.getName() + "} object " +
					"corresponding to the entry in {@code " + lclTMC.getTableName().getFullyQualifiedTableName() +
					"} that is referenced by {@code " + lclMFK.getForeignKey().getName() + "}.");
				if (lclMFK.getForeignKey().getJoinQueryFactoryName() == null) {
					/* No JQF */
				} else {
					/* JQF */
				}
				lclJD.end();
				
				if (lclMFK.getTargetMappedClass().isDeprecated() && isDeprecated() == false) {
					lclBW.println("\t@Deprecated");
				}
				if (lclMFK.shouldSourceExtendSupplierInterface() && lclMFK.getForeignKey().getSpecifiedBaseName() == null) {
					lclBW.println("\t@Override");
				}
				lclBW.println("\t@" + Nullability.class.getName() + "(nullable = " + (!lclMFK.isRequired()) + ")");
				lclBW.println("\tpublic " + lclT + " " + lclMFK.getAccessorName() + "();");
				lclBW.println();
				
				if (lclMFK.shouldSourceExtendSupplierInterface()) {
					// We know the MFK role must be blank at this point.
					if (lclMFK.getForeignKey().getSpecifiedBaseName() != null) {
						lclBW.println("\t@Override");
						if (lclMFK.getTargetMappedClass().isDeprecated() && isDeprecated() == false) {
							lclBW.println("\t@Deprecated");
						}
						lclBW.println("\t@" + Nullability.class.getName() + "(nullable = " + (!lclMFK.isRequired()) + ")");
						lclBW.println("\tdefault " + lclT + " get" + lclMFK.getDefaultSourceFieldName() + "() {"); // Role is blank.
						lclBW.println("\t\treturn " + lclMFK.getAccessorName() + "();");
						lclBW.println("\t}");
						lclBW.println();
					}
				}
				
				/* Create the mutator */
				if (isCreatable() || isUpdatable()) {
					if (lclMFK.getTargetMappedClass().isDeprecated() && isDeprecated() == false) {
						lclBW.println("\t@Deprecated");
					}
					
					printRequiresActiveTransactionAnnotation(lclBW, 1);
					
					lclBW.println("\tpublic " + determineMutatorReturnType(getFullyQualifiedInterfaceClassName()) + " " + lclMFK.getMutatorName() + "(" + lclT + ' ' + lclA + ");");
					lclBW.println();
				}
			}
			
			/* Handle the database foreign keys that represent one-to-one relationships. */
			
			for (MappedForeignKey lclMFK : getForeignKeysTo()) {
				MappedClass lclSMC = lclMFK.getSourceMappedClass();
				
				if (lclMFK.representsPolymorphism()) {
					continue;
				}
				
				if (lclMFK.representsManyToOneRelationship()) {
					continue;
				}
				
				String lclA = "arg" + lclMFK.getRoleTargetFieldName();
				
				if ("Protected".equalsIgnoreCase(lclMFK.getTargetFieldAccess())) {
					continue;
				}
				if (lclMFK.appearsInTargetUserFacing() == false) {
					continue;
				}
				
				final String lclT = lclSMC.getFullyQualifiedInterfaceClassName();
				
				if (lclSMC.isDeprecated() && (isDeprecated() == false)) {
					lclBW.println("\t@Deprecated");
				}
				lclBW.println("\t@" + Nullability.class.getName() + "(nullable = " + (!lclMFK.isRequired()) + ")");
				lclBW.println("\tpublic " + lclT + " get" + lclMFK.getRoleTargetFieldName() + "();");
				
				if (lclSMC.isDeprecated() && (isDeprecated() == false)) {
					lclBW.println("\t@Deprecated");
				}
				printRequiresActiveTransactionAnnotation(lclBW, 1);
				lclBW.println("\tpublic " + determineMutatorReturnType(getFullyQualifiedInterfaceClassName()) + " set" + lclMFK.getRoleTargetFieldName() + "(" + lclT + ' ' + lclA + ");");
				lclBW.println();
			}
			
			/* Handle database foreign keys coming into this class that represent many-to-one relationships.  (That is,
			 * the source columns aren't covered by a unique index converting them into a zero/one-to-one relationship.)
			 */
			
			if (createMatchesMethod()) {
				lclBW.println("\tdefault boolean matches(" + getSupplierInterfaceName() + " argSupplier) {");
				lclBW.println("\t\treturn (argSupplier != null) ? argSupplier." + getSupplierAccessorName() + "() == this : false;");
				lclBW.println("\t}");
				lclBW.println();
			}

			for (MappedForeignKey lclMFK : getForeignKeysTo()) {
				MappedClass lclSMC = lclMFK.getSourceMappedClass();
				
				if (lclMFK.representsPolymorphism()) {
					continue;
				}
				
				if (lclMFK.representsOneToOneRelationship()) {
					continue;
				}
				
				if (lclMFK.hasBackCollection() == false) {
					continue;
				}
				
				if ("Protected".equalsIgnoreCase(lclMFK.getTargetCollectionAccess())) {
					continue;
				}
				
				final String lclT = lclSMC.getFullyQualifiedInterfaceClassName();
				
				String lclDeprecated = lclSMC.isDeprecated() && (isDeprecated() == false) ? "\t@Deprecated" + System.lineSeparator() : "";
				
				lclBW.print(lclDeprecated);
				lclBW.println("\tpublic " + USER_FACING_SET_CLASS.getName() + "<" + lclT + "> " + lclMFK.getRoleCollectionAccessorName() + "();");
				lclBW.println();
				
				lclBW.print(lclDeprecated);
				lclBW.println("\tdefault " + USER_FACING_STREAM_CLASS.getName() + "<" + lclT + "> " + lclMFK.getStreamMethodName() + "() {");
				lclBW.println("\t\treturn " + lclMFK.getRoleCollectionAccessorName() + "().stream();");
				lclBW.println("\t}");
				lclBW.println();
				
				if (lclSMC.isEphemeral() == false) {
					lclBW.print(lclDeprecated);
					lclBW.println("\tdefault " + lclT + "[] " + lclMFK.getArrayMethodName() + "() {");
					lclBW.println("\t\t" + USER_FACING_SET_CLASS.getName() + "<" + lclT +"> lclS = " + lclMFK.getRoleCollectionAccessorName() + "();");
					lclBW.println("\t\treturn lclS.toArray(new " + lclT + "[lclS.size()]);"); // Should this be [0]?
					lclBW.println("\t}");
					lclBW.println();
				} else {
					// TODO: What's up with this?  Why HTTP_...?  Why is this different?
					lclBW.print(lclDeprecated);
					lclBW.println("\tdefault " + lclT + "[] " + lclMFK.getArrayMethodName() + "() {");
					lclBW.println("\t\t" + CREATE_ARRAY_INTERMEDIATE_COLLECTION_CLASS.getName() + "<" + lclT + "> lclList = new " + CREATE_ARRAY_INTERMEDIATE_COLLECTION_CLASS.getName() + "<>();");
					lclBW.println("\t\t" + lclMFK.getAcquireMethodName() + "(lclList);");
					lclBW.println("\t\treturn lclList.toArray(new " + lclT + "[lclList.size()]);"); // Should this be [0]?
					lclBW.println("\t}");
					lclBW.println();
				}
			}
			
			if (hasSuperclass() == false) {
				if (isCreatable()) {
					lclBW.println("\tpublic " + getFullyQualifiedInterfaceClassName() + " copy();");
					lclBW.println();
				}
			}
			
			// TODO: Copy annotations
			for (MethodDelegation lclMD : getMethodDelegations()) {
				lclBW.print("\tdefault " + OpalUtility.generateJavaDeclaration(lclMD.getReturnType()) + ' ' + lclMD.getLocalMethodName() + "(");
				Type[] lclParameters = lclMD.getParameters();
				if (lclParameters != null) {
					for (int lclI = 0; lclI < lclParameters.length; ++lclI) {
						if (lclI > 0) {
							lclBW.print(", ");
						}
						lclBW.print(OpalUtility.generateJavaDeclaration(lclParameters[lclI]));
						lclBW.print(' ');
						lclBW.print("arg");
						lclBW.print((char) ('A' + lclI));
					}
				}
				lclBW.print(")");
				Type[] lclExceptions = lclMD.getExceptions();
				if (lclExceptions != null && lclExceptions.length > 0) {
					lclBW.print(" throws ");
					for (int lclI = 0; lclI < lclExceptions.length; ++lclI) {
						if (lclI > 0) {
							lclBW.print(", ");
						}
						lclBW.print(OpalUtility.generateJavaDeclaration(lclExceptions[lclI]));
					}
				}
				lclBW.println(" {");
				lclBW.print("\t\t");
				if (lclMD.getReturnType() != Void.TYPE) {
					lclBW.print("return ");
				}
				lclBW.print(lclMD.getClassName() + '.' + lclMD.getMethodName() + "(this");
				if (lclParameters != null) {
					for (int lclI = 0; lclI < lclParameters.length; ++lclI) {
						lclBW.print(", arg");
						lclBW.print((char) ('A' + lclI));
					}
				}
				lclBW.println(");");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			/* Generate find methods.  If Opal B has foreign keys to Opals A and C such that the union of those foreign keys' columns
			 * cover one of B's unique keys, the we add a method to A like B findB(C) and a method to C like B findB(A).
			 * 
			 * We require that both of B's foreign keys to unprefixed (to avoid problems with generating duplicate methods),
			 * but there's no theoretical reason to do that.  We'll need (or least want) role-prefixed (or -suffixed) matches
			 * methods to implement it, though.  We also require that at least one of the foreign keys have a back collection.
			 * 
			 * The current implementation just streams one of the back collections looking for a match.  In theory, this could
			 * be greatly optimized by using a Factory lookup (if key values haven't changed) and/or checking to see which
			 * back collection might already be loaded.
			 * 
			 * It would be logical to do this in the earlier loop over getForeignKeysTo(), but that loop bails early in the
			 * absence of a back collection.  However, we will want to create a find method if the second key opal has a 
			 * back collection (even if this one doesn't).
			 */
			final HashSet<MappedClass> lclSecondKeysAlreadyUsed = new HashSet<>(); 
			
			for (MappedForeignKey lclMFK1 : getForeignKeysTo()) {
				if (lclMFK1.representsManyToOneRelationship() && (lclMFK1.getSourceRolePrefix().equals(""))) {
					lclSecondKeysAlreadyUsed.clear();
					MappedClass lclMiddleMC = lclMFK1.getSourceMappedClass();
					for (MappedUniqueKey lclMUK : lclMiddleMC.getMappedUniqueKeys()) {
						for (MappedForeignKey lclMFK2 : lclMiddleMC.getForeignKeysFrom()) {
							if ((lclMFK1 != lclMFK2) && lclMFK2.representsManyToOneRelationship() && (lclMFK2.getSourceRolePrefix().equals(""))) {
								if ((lclMFK1.hasBackCollection() == false) && (lclMFK2.hasBackCollection() == false)) {
									continue;
								}
								MappedClass lclSecondKeyMC = lclMFK2.getTargetMappedClass();
								if (lclSecondKeysAlreadyUsed.contains(lclSecondKeyMC)) {
									continue;
								}
								boolean lclEligible = true;
								for (ClassMember lclCM : lclMUK.getClassMembers()) {
									if ((lclMFK1.getSource().contains(lclCM) == false) && (lclMFK2.getSource().contains(lclCM) == false)) {
										lclEligible = false;
									}
								}
								if (lclEligible) {
									lclSecondKeysAlreadyUsed.add(lclSecondKeyMC);
									lclBW.println("\tdefault " + lclMiddleMC.getFullyQualifiedInterfaceClassName() + " find" + lclMiddleMC.getInterfaceClassName() + "(" + lclSecondKeyMC.getFullyQualifiedInterfaceClassName() + " argK) {");
									lclBW.println("\t\tif (argK == null) { return null; }");
									if (lclMFK2.hasBackCollection()) {
										lclBW.println("\t\treturn argK." + lclMFK2.getStreamMethodName() + "().filter(this::matches).findAny().orElse(null);");
									} else {
										lclBW.println("\t\treturn this." + lclMFK1.getStreamMethodName() + "().filter(argK::matches).findAny().orElse(null);");
									}
									lclBW.println("\t}");
									lclBW.println();
								}
							}
						}
					}
				}
			}
			
			/* Create single-field Comparators for fields that were either marked with Comparator="True" or that have a name
			 * that matches the list of column names that get Comparators by default (e.g., "sequence").
			 */
			for (ClassMember lclCM : getClassMembers()) {
				if (lclCM.isComparator()) {
					String lclFQICN = getFullyQualifiedInterfaceClassName();
					
					lclBW.println("\t/** This is a Comparator that can be used to compare " + getInterfaceClassName() + " objects based on their {@code " + lclCM.getBaseMemberName() + "} values. */");
					lclBW.println();
					String lclComparatorClassName = lclCM.getBaseMemberName() + "Comparator";
					lclBW.println("\tpublic static class " + lclComparatorClassName + " extends com.siliconage.util.NullSafeComparator<" + lclFQICN + "> {");
					lclBW.println("\t\tprivate static final " + lclComparatorClassName + " ourInstance = new " + lclComparatorClassName + "();");
					lclBW.println("\t\tpublic static final " + lclComparatorClassName + " getInstance() { return ourInstance; }");
					lclBW.println();
					lclBW.println("\t\tprivate " + lclComparatorClassName + "() { super(); }");
					lclBW.println();
					lclBW.println("\t\t@Override");
					lclBW.println("\t\tpublic int compareInternal(" + lclFQICN + " argFirst, " + lclFQICN + " argSecond) {");
					
					String lclFirst;
					String lclSecond;
					if (lclCM.isInverted()) {
						lclFirst = "argSecond";
						lclSecond = "argFirst";
					} else {
						lclFirst = "argFirst";
						lclSecond = "argSecond";
					}
					
					if (lclCM.getMemberType() == String.class) {
						if (lclCM.isNullAllowed()) {
							lclBW.println("\t\t\treturn nullSafeCompareIgnoreCase("
								+ lclFirst + '.' + lclCM.getObjectAccessorName() + "()"
								+ ", " 
								+ lclSecond + '.' + lclCM.getObjectAccessorName() + "()"
								+ ");");
						} else {
							lclBW.println("\t\t\treturn " + lclFirst + '.' + lclCM.getObjectAccessorName() + "().compareToIgnoreCase(" + lclSecond + '.' + lclCM.getObjectAccessorName() + "());");
						}
					} else {
						if (lclCM.isNullAllowed()) {
							lclBW.println("\t\t\treturn nullSafeCompare("
								+ lclFirst + '.' + lclCM.getObjectAccessorName() + "()"
								+ ", " 
								+ lclSecond + '.' + lclCM.getObjectAccessorName() + "()"
								+ ");");
						} else {
							lclBW.println("\t\t\treturn " + lclFirst + '.' + lclCM.getObjectAccessorName() + "().compareTo(" + lclSecond + '.' + lclCM.getObjectAccessorName() + "());");
						}
					}
					lclBW.println("\t\t}"); // End method
					lclBW.println("\t}"); // End class
					lclBW.println();
				}
			}
			
			/* Create Comparators, if any */
			for (ComparatorSpecification lclCS : getComparatorSpecifications()) {
				String lclComparatorClassName = lclCS.generateClassName();
				String lclFQICN = getFullyQualifiedInterfaceClassName();
				lclBW.println("\tpublic static class " + lclComparatorClassName + " extends com.siliconage.util.NullSafeComparator<" + lclFQICN + "> {");
				lclBW.println("\t\tprivate static final " + lclComparatorClassName + " ourInstance = new " + lclComparatorClassName + "();");
				lclBW.println("\t\tpublic static final " + lclComparatorClassName + " getInstance() { return ourInstance; }");
				lclBW.println();
				lclBW.println("\t\tprivate " + lclComparatorClassName + "() { super(); }");
				lclBW.println();
				lclBW.println("\t\t@Override");
				lclBW.println("\t\tpublic int compareInternal(" + lclFQICN + " argFirst, " + lclFQICN + " argSecond) {");
				for (int lclI = 0; lclI < lclCS.getCriteria().size(); ++lclI) {
					ComparatorCriterion lclCriterion = lclCS.getCriteria().get(lclI);
					boolean lclLast = lclI == lclCS.getCriteria().size() - 1;
					String lclStartString = lclLast ? "return " : lclI == 0 ? "int lclResult = " : "lclResult = ";
					String lclComparisonCode = lclCriterion.generateComparisonCode(this);
					lclBW.println("\t\t\t" + lclStartString + lclComparisonCode + ';');
					if (!lclLast) {
						lclBW.println("\t\t\tif (lclResult != 0) {");
						lclBW.println("\t\t\t\treturn lclResult;");
						lclBW.println("\t\t\t}");
					}
				}
				lclBW.println("\t\t}");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (isTree()) {
				MappedForeignKey lclMFK = getTreeParentKey();
				
				String lclTA = lclUFCN + TreeAdapter.class.getSimpleName();
				String lclFQICN = getFullyQualifiedInterfaceClassName();
				String lclTABase = TreeAdapter.class.getName() + "<" + lclFQICN + ">";
				
				lclBW.println("\tpublic static class " + lclTA + " extends " + lclTABase + " {");
				lclBW.println("\t\t/** This is a singleton class of which only one instance should ever exist.  Clients of this class");
				lclBW.println("\t\tshould not create their own instances using a constructor, but should instead invoke the static");
				lclBW.println("\t\tmethod {@code getInstance()} to access the singleton instance. */");
				lclBW.println();
				lclBW.println("\t\t/** A static reference to the only instance of this class, which is constructed on class load. */");
				lclBW.println();
				lclBW.println("\t\tprivate static final " + lclTA + " ourInstance = new " + lclTA + "();");
				lclBW.println();
				lclBW.println("\t\t/** @return a reference to the singleton instance. */");
				lclBW.println();
				lclBW.println("\t\tpublic static final " + lclTA + " getInstance() {");
				lclBW.println("\t\t\treturn ourInstance;");
				lclBW.println("\t\t}");
				lclBW.println();
				lclBW.println("\t\t@Override");
				lclBW.println("\t\tpublic " + lclFQICN + " getParent(" + lclFQICN + " argChild) {");
				lclBW.println("\t\t\treturn argChild.get" + lclMFK.getRoleSourceFieldName() + "();");
				lclBW.println("\t\t}");
				lclBW.println();
				lclBW.println("\t\t@Override");
				lclBW.println("\t\tpublic " + Set.class.getName() + '<' + lclFQICN + "> getChildSet(" + lclFQICN + " argParent) {");
				lclBW.println("\t\t\treturn argParent." + lclMFK.getRoleCollectionAccessorName() + "();");
				lclBW.println("\t\t}");
				lclBW.println("\t}");
				lclBW.println();
				lclBW.println("\tdefault " + Tree.class.getName() + '<' + lclFQICN + "> asTree() {");
				lclBW.println("\t\treturn new " + Tree.class.getName() + "<>((" + lclFQICN + ") this, " + lclTA + ".getInstance());");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (createSupplierInterface()) {
				lclBW.println("\tpublic interface " + getSupplierInterfaceName() + " {");
				lclBW.println("\t\tpublic " + getFullyQualifiedInterfaceClassName() + " " + getSupplierAccessorName() + "();");
				lclBW.println("\t}");
				lclBW.println();
			}
						
			lclBW.println("}");
		} // Closes lclBW with try-with-resources
	}
	
	protected void createOpal() throws IOException {
		String lclOpalClassFileName = StringUtility.makeFilename(getSourceDirectory(), getOpalPackageName(), getOpalClassName());
		
		File lclOpalClassFile = new File(lclOpalClassFileName);
		
		try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclOpalClassFile)))) {
			lclBW.println("package " + getOpalPackageName() + ';');
			lclBW.println();
			lclBW.println("import " + getFullyQualifiedInterfaceClassName() + ';');
			lclBW.println();
			
			final String lclICN = getInterfaceClassName();
			final String lclOCN = getOpalClassName();
			
			Class<?> lclSuperClass;
			if (isEphemeral()) {
				lclSuperClass = EphemeralOpal.class;
			} else if (isImmutableOpal()) {
				lclSuperClass = ImmutableOpal.class;
			} else if (isUpdatableOpal()) {
				lclSuperClass = UpdatableOpal.class;
			} else {
				throw new IllegalStateException("Can't determine proper superclass.");
			}
			
			Class<?> lclOpalFactoryClass = isEphemeral() ? OpalFactory.class : IdentityOpalFactory.class;
			
			String lclSuperClassName = lclSuperClass.getName();
			
			lclBW.println("@javax.annotation.Generated(\"com.opal\")");
			if (isDeprecated()) {
				lclBW.println("@Deprecated");
			}
			if (getPrimaryKey() != null) {
				boolean lclStoreGeneratedPrimaryKey = false;
				for (ClassMember lclCM : getPrimaryKey().getClassMembers()) {
					if (lclCM.getDatabaseColumn().hasDatabaseGeneratedNumber()) {
						lclStoreGeneratedPrimaryKey = true;
					}
				}
				if (lclStoreGeneratedPrimaryKey) {
					lclBW.println('@' + StoreGeneratedPrimaryKey.class.getName());
				}
			}
			lclBW.print("public final class " + lclOCN + " extends " + lclSuperClassName + "<" + lclICN + ">");
			lclBW.println(" {");
			
			lclBW.println();
			
			/* Any default values used for this object */
			
			Iterator<ClassMember> lclCMI = createClassMemberIterator();
			while (lclCMI.hasNext()) {
				ClassMember lclCM = lclCMI.next();
				DefaultValue lclDefault = lclCM.getDefault();
				if (lclDefault != null) {
					String lclDefaultDefinition = lclDefault.generateDefinition(lclCM.getMemberType(), lclCM.getDefaultValueMemberName());
					if (StringUtils.isNotBlank(lclDefaultDefinition)) {
						lclBW.println(lclDefaultDefinition);
					}
				}
			}
			
			lclBW.println(); /* TODO: Print blank line only if necessary */
			
			/* Constructor */
			
			if (isEphemeral() == false) {
				lclBW.println("\tprivate " + getOpalClassName() + "() {");
				lclBW.println("\t\tsuper();");
				lclBW.println("\t\tsetUserFacing(null);");
				lclBW.println("\t}");
				lclBW.println();
			}
			if (isEphemeral() == false) {
				lclBW.println("\tpublic " + getOpalClassName() + "(" + lclOpalFactoryClass.getName() + '<' + lclICN + ", " + lclOCN + "> argOpalFactory, Object[] argValues) {");
			} else {
				lclBW.println("\tpublic " + getOpalClassName() + "(Object[] argValues) {");
			}
			if (isEphemeral() == false) {
				lclBW.println("\t\tsuper(argOpalFactory, argValues);");
			} else {
				lclBW.println("\t\tsuper(null, argValues);");
				lclBW.println("\t\tinitializeReferences();");
			}
			lclBW.println("\t}");
			lclBW.println();
			
			// TODO: Explain what this is.
			if (GENERATE_FIELD_CLASS) {
				lclBW.println("\tpublic static class FIELD {");
				int lclIndex = 0;
				final String lclFQICN = getFullyQualifiedInterfaceClassName();
				for (ClassMember lclCM : getClassMembers()) { // Do we need to skip unmapped ones or anything?
					Class<?> lclOFC = lclCM.getOpalFieldClass();
					String lclOFTypeArgument;
					if (lclOFC == OpalPlainField.class) { // FIXME: Fix this
						lclOFTypeArgument = "<" + lclICN + ", " + lclCM.getMemberType().getName() + ">";
					} else {
						lclOFTypeArgument = "<" + lclICN + ">";
					}
					String lclOFN = lclOFC.getName();
					boolean lclUseMutableOpalFieldCtor = isUpdatable() && lclCM.isUpdatable() && (lclCM.isComputed() == false); // Or Creatable?  How did that work?
					// FIXME: Why aren't computed fields automatically non-updatable?
					
					lclBW.println("\t\tpublic static final " + lclOFN + lclOFTypeArgument + " " + lclCM.getBaseMemberName() + " = new " + lclOFN + "<>(");
					lclBW.println("\t\t\t\tnew " + com.opal.OpalBaseField.class.getName() + "<>(");
					lclBW.println("\t\t\t\t\t\t" + lclIndex + ",");
					lclBW.println("\t\t\t\t\t\t\"" + lclCM.getBaseMemberName() + "\",");
					lclBW.println("\t\t\t\t\t\t" + lclCM.getMemberType().getName() + ".class,");
					if (lclUseMutableOpalFieldCtor) {
						lclBW.println("\t\t\t\t\t\t" + lclCM.isUpdatable() + ",");
					}
					lclBW.println("\t\t\t\t\t\t" + lclCM.isNullAllowed() + ",");
					lclBW.println("\t\t\t\t\t\t" + "null" + ","); // Supplier<> for default value
					lclBW.println("\t\t\t\t\t\t" + "null" + ","); // Validator
					lclBW.println("\t\t\t\t\t\t" + lclFQICN + "::" + lclCM.getObjectAccessorName() + ","); // If not qualified, problems occur if the UserFacing name overlaps with a column name
					if (lclUseMutableOpalFieldCtor) {
						lclBW.println("\t\t\t\t\t\t" + lclFQICN + "::" + lclCM.getObjectMutatorName() + ",");
					}
					lclBW.println("\t\t\t\t\t\t\"" + lclCM.getDatabaseColumn().getName() + "\"");
					lclBW.println("\t\t\t\t\t\t)");
					lclBW.println("\t\t\t\t);");
					lclBW.println();
					++lclIndex;
				}
				if (getClassMemberCount() == 0) {
					lclBW.println("\t\tpublic static final " + List.class.getName() + "<" + OpalField.class.getName() + "<" + lclICN + ", ?>> ALL = " + List.class.getName() + ".of();");
				} else {				
					lclBW.println("\t\tpublic static final " + List.class.getName() + "<" + OpalField.class.getName() + "<" + lclICN + ", ?>> ALL = " + List.class.getName() + ".of(");
					lclCMI = createClassMemberIterator();
					while (lclCMI.hasNext()) {
						ClassMember lclCM = lclCMI.next();
						lclBW.print("\t\t\t\t" + lclCM.getBaseMemberName());
						if (lclCMI.hasNext()) {
							lclBW.print(",");
						}
						lclBW.println();
					}
					lclBW.println("\t\t\t\t);");
				}
				lclBW.println("\t}"); // end of FIELD class definition
				lclBW.println();
			}
			
			/* Apply Defaults */
			
			if (isCreatable()) {
				boolean lclHasAnyDefaults = false;
				lclCMI = createClassMemberIterator();
				while (lclCMI.hasNext()) {
					ClassMember lclCM = lclCMI.next();
					if (lclCM.hasDefault()) {
						lclHasAnyDefaults = true;
					}
				}
				if (lclHasAnyDefaults || hasAtLeastOneBackCollection()) {
					lclBW.println("\t@Override");
					lclBW.println("\tprotected void applyDefaults() {");
					if (lclHasAnyDefaults) {
						lclBW.println("\t\t/* Initialize fields with their default values. */");
						lclCMI = createClassMemberIterator();
						while (lclCMI.hasNext()) {
							ClassMember lclCM = lclCMI.next();
							if (lclCM.hasDefault()) {
								String lclApplyDefaultCode = lclCM.getDefault().generateCodeToApply(lclCM.getMemberType(), lclCM.getFieldIndex(), lclCM.getDefaultValueMemberName());
								if (StringUtils.isNotBlank(lclApplyDefaultCode)) {
									lclBW.println(lclApplyDefaultCode);
								}
							}
						}
						lclBW.println();
					}
				
					if (hasAtLeastOneBackCollection()) {
						lclBW.println("\t\t/* Initialize the back Collections to empty sets. */");
						lclBW.println();
						for (MappedForeignKey lclMFK : getForeignKeysTo()) {
							if (lclMFK.representsManyToOneRelationship()) {
								if (lclMFK.hasBackCollection()) {
									String lclC = lclMFK.getRoleCollectionMemberName();
									String lclL = lclMFK.getRoleCollectionLoaderName();
									ReferenceType lclRT = lclMFK.getCollectionReferenceType();
									switch (lclRT) {
										case HARD:
											lclBW.println("\t\t" + lclC + " = new " + OpalBackCollectionDoubleSet.class.getName() + "<>(");
											lclBW.println("\t\t\tthis,");
											lclBW.println("\t\t\t" + lclL + ",");
											lclBW.println("\t\t\ttrue"); // Preclude database queries known to load no rows (since the Opal is new)
											lclBW.println("\t\t);");
											break;
										case WEAK: case SOFT:
											Class<?> lclZ = lclRT.getReferenceClass();
											lclBW.println("\t\t" + lclC + " = new " + lclZ.getName() +"<>(new " + OpalBackCollectionDoubleSet.class.getName() + "<>(");
											lclBW.println("\t\t\tthis,");
											lclBW.println("\t\t\t" + lclL + ",");
											lclBW.println("\t\t\ttrue"); // Preclude database queries known to load no rows (since the Opal is new)
											lclBW.println("\t\t));");
											break;
									}
								}
							}
						}
						lclBW.println();
					}
					lclBW.println("\t\treturn;");
					lclBW.println("\t}");
					lclBW.println();
				}
			}
			
			if (hasAtLeastOneReference()) {
				if (isEphemeral() == false) {
					lclBW.println("\t@Override");
				}
				if (hasAtLeastOneDeprecatedReference()) {
					lclBW.println("\t@SuppressWarnings(\"deprecation\")");
				}
				lclBW.println("\tprotected void initializeReferences() {");
				for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
					String lclON = lclMFK.getTargetOpalClassName();
					lclBW.println("\t\t" + lclMFK.getOldRoleSourceOpalFieldName() + " = " + lclON + '.' + NOT_YET_LOADED_STATIC_MEMBER_NAME + ';');
				}
				for (MappedForeignKey lclMFK : getForeignKeysTo()) {
					if (lclMFK.representsPolymorphism()) {
						continue;
					}
					if (lclMFK.representsOneToOneRelationship() == false) {
						continue;
					}
					String lclON = lclMFK.getSourceOpalClassName();
					lclBW.println("\t\t" + lclMFK.getOldRoleTargetOpalFieldName() + " = " + lclON + '.' + NOT_YET_LOADED_STATIC_MEMBER_NAME + ';');
				}
				lclBW.println("\t\treturn;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (hasSuperclass()) {
				lclBW.println("\t@Override");
				lclBW.println("\tprotected void createSuperclassOpals() {");
				MappedForeignKey lclSK = getSuperclassKey();
				assert lclSK != null;
				lclBW.println("\t\tset" + lclSK.getRoleSourceOpalFieldName() + "(" + lclSK.getTargetMappedClass().getFullyQualifiedFactoryClassName() + ".getInstance()." + lclSK.getTargetMappedClass().getOpalFactoryAccessorName() + "().createAsSuperOpal(getUserFacing()));");
				lclBW.println("\t}");
				lclBW.println();
			}
			lclBW.println("\t/* package */ static final String[] ourFieldNames = new String[] {");
			lclCMI = createClassMemberIterator();
			while (lclCMI.hasNext()) {
				ClassMember lclCM = lclCMI.next();
				if (!lclCM.isMapped()) {
					continue;
				}
				lclBW.print("\t\t\"");
				lclBW.print(lclCM.getBaseMemberName());
				lclBW.println("\",");
			}
			lclBW.println("\t};");
			lclBW.println();
			
			lclBW.println("\t/* package */ static final Class<?>[] ourFieldTypes = new Class<?>[] {");
			lclCMI = createClassMemberIterator();
			while (lclCMI.hasNext()) {
				ClassMember lclCM = lclCMI.next();
				if (!lclCM.isMapped()) {
					continue;
				}
				lclBW.print("\t\t");
				lclBW.print(OpalUtility.generateTypeName(lclCM.getMemberType(), false));
				lclBW.println(".class,");
			}
			lclBW.println("\t};");
			lclBW.println();
			
			lclBW.println("\t/* package */ static final boolean[] ourFieldNullability = new boolean[] {");
			lclCMI = createClassMemberIterator();
			while (lclCMI.hasNext()) {
				ClassMember lclCM = lclCMI.next();
				if (!lclCM.isMapped()) {
					continue;
				}
				lclBW.print("\t\t");
				lclBW.println(lclCM.getDatabaseColumn().isNullable() ? "true," : "false,");
			}
			lclBW.println("\t};");
			lclBW.println();
			
			lclBW.println("\t/* package */ static final " + FieldValidator.class.getName() + "[] ourFieldValidators = new " + FieldValidator.class.getName() + "[] {");
			lclCMI = createClassMemberIterator();
			while (lclCMI.hasNext()) {
				ClassMember lclCM = lclCMI.next();
				if (!lclCM.isMapped()) {
					continue;
				}
				lclBW.print("\t\t");
				lclBW.println("null,");
			}
			lclBW.println("\t};");
			lclBW.println();
			
			/* The NOT_YET_LOADED marker.  Aesthetically, I would like to create this at the very top of the file, but we
			 * shouldn't be instantiating instances of a class until all of its static fields have been initialized. */
			
			if (isEphemeral() == false) {
				lclBW.println("\tpublic static final " + lclOCN + ' ' + NOT_YET_LOADED_STATIC_MEMBER_NAME + " = new " + lclOCN + "();");
				lclBW.println();
			}
			
			lclBW.println("\tpublic static String[] getStaticFieldNames() { return ourFieldNames; }");
			lclBW.println();
			lclBW.println("\t@Override");
			lclBW.println("\tpublic final String[] getFieldNames() { return getStaticFieldNames(); }");
			lclBW.println();
			lclBW.println("\tpublic static Class<?>[] getStaticFieldTypes() { return ourFieldTypes; }");
			lclBW.println();
			lclBW.println("\t@Override");
			lclBW.println("\tpublic final Class<?>[] getFieldTypes() { return getStaticFieldTypes(); }");
			lclBW.println();
			lclBW.println("\tpublic static boolean[] getStaticFieldNullability() { return ourFieldNullability; }");
			lclBW.println();
			lclBW.println("\t@Override");
			lclBW.println("\tpublic boolean[] getFieldNullability() { return getStaticFieldNullability(); }");
			lclBW.println();
			lclBW.println("\tpublic static " + FieldValidator.class.getName() + "[] getStaticFieldValidators() { return ourFieldValidators; }");
			lclBW.println();
			lclBW.println("\t@Override");
			lclBW.println("\tpublic final " + FieldValidator.class.getName() + "[] getFieldValidators() { return getStaticFieldValidators(); }");
			lclBW.println();
			lclBW.println();
			
			int lclIndex;
			
			/* Create the accessors and mutators */
			
			/* Accessors */
			
			lclIndex = -1;
			lclCMI = createClassMemberIterator();
			while (lclCMI.hasNext()) {
				++lclIndex;
				ClassMember lclCM = lclCMI.next();
				if (!lclCM.isMapped()) {
					continue;
				}
				
				final String lclMemberTypeName = lclCM.getMemberParameterizedTypeName();
				
				/* This accessor returns the value of the field as an Object. */
				
				if (isCreatable() || isUpdatable()) {
					/* Because these Opals can be created and/or updated, we need to allow for synchronization and
					 * also determine whether the old or new values are being accessed. */
					if (lclCM.getTypeParameter() != null) {
						lclBW.println("\t@SuppressWarnings(\"unchecked\")");
					}
					lclBW.println("\tpublic synchronized " + lclMemberTypeName + ' ' + lclCM.getObjectAccessorName() + "() {");
					if (isEphemeral() == false) { // THINK: Can we ever hit this line for an ephemeral Opal?
						if (getPrimaryKey().getClassMembers().contains(lclCM) == false) {
							lclBW.println("\t\tmarkAsDataRead();"); // TODO: Drop this later
						}
					}
					lclBW.println("\t\treturn (" + lclMemberTypeName + ") getReadValueSet()[" + lclIndex + "];");
					lclBW.println("\t}");
					lclBW.println();
				} else {
					/* Since this Opal is immutable, there is no need to synchronize or to determine whether the old
					 * or new values are being used (since there is only one set. */
					if (lclCM.getTypeParameter() != null) {
						lclBW.println("\t@SuppressWarnings(\"unchecked\")");
					}
					lclBW.println("\tpublic " + lclMemberTypeName + ' ' + lclCM.getObjectAccessorName() + "() {");
					if (isEphemeral() == false) { // THINK: Can we ever hit this line for an ephemeral Opal?
						if (getPrimaryKey().getClassMembers().contains(lclCM) == false) {
							lclBW.println("\t\tmarkAsDataRead();"); // TODO: Drop this later
						}
					}
					lclBW.println("\t\treturn (" + lclMemberTypeName + ") getValues()[" + lclIndex + "];");
					lclBW.println("\t}");
					lclBW.println();
				}
				
			}
			
			/* Mutators */
			
			if (isUpdatable() || isCreatable()) {
				/* If the MappedClass is not Updatable, then no mutators will be built.  TODO:  Simplify internal representation */
				
				lclIndex = -1;
				lclCMI = createClassMemberIterator();
				while (lclCMI.hasNext()) {
					++lclIndex;
					final ClassMember lclCM = lclCMI.next();
					
					if (!lclCM.isMapped()) {
						continue;
					}
					
					/* We don't generate mutators for computed fields.  This will fail if one tries to have chained
					 * computed fields; for instance myB = myA + 1 and myC = myB + 1.  FIXME: Come back and make that
					 * work. */
					
					if (lclCM.isComputed()) {
						continue;
					}
					
					final Class<?> lclMemberType = lclCM.getMemberType();
					final String lclMemberTypeName = lclCM.getMemberParameterizedTypeName(); // {OpalUtility.generateTypeName(lclMemberType);
					
					/* This mutator accepts an Object. */
					lclBW.println("\tpublic synchronized " + determineMutatorReturnType(getOpalClassName()) + ' ' + lclCM.getObjectMutatorName() + "(final " + lclMemberTypeName + ' ' + lclCM.getObjectMutatorArgumentName() + ") {");
					lclBW.println("\t\ttryMutate();");
					
					/* Cache whether or not the field for which we are creating a mutator is nullable. */
					boolean lclNullable = lclCM.getDatabaseColumn().isNullable();
					/* If it can't be null, create code to check whether the argument is null. */
					if (!lclNullable) {
						lclBW.println("\t\tif (" + lclCM.getObjectMutatorArgumentName() + " == null) {");
						lclBW.println("\t\t\tthrow new " + IllegalNullArgumentException.class.getName() + "(\"Cannot set " + lclCM.getMemberName() + " on \" + this + \" to null.\");");
						lclBW.println("\t\t}");
					}
					
					/* If it's a String, generate code to make sure it conforms to the maximum length. */
					if (lclCM.getMemberType() == String.class) {
						/* Is it length-limited? */
						int lclMaxLength = lclCM.getDatabaseColumn().getLength();
						if (0 < lclMaxLength && lclMaxLength < Integer.MAX_VALUE) {
							/* Can it be null? */
							if (lclNullable) {
								/* Yes.  We need to generate an if condition that guards against a NullPointerException
								* from calling length() on the string. */
								lclBW.println("\t\tif ((" + lclCM.getObjectMutatorArgumentName() + " != null) && (" + lclCM.getObjectMutatorArgumentName() + ".length() > " + lclMaxLength + ")) {");
							} else {
								/* No.  We should generate an if condition without that check since it is redundant and
								* will be flagged with a warning by modern compilers. */
								lclBW.println("\t\tif (" + lclCM.getObjectMutatorArgumentName() + ".length() > " + lclMaxLength + ") {");
							}
							lclBW.println("\t\t\tthrow new " + ArgumentTooLongException.class.getName() + "(\"Cannot set " + lclCM.getMemberName() + " on \" + this + \" to \\\"\" + " + lclCM.getObjectMutatorArgumentName() + " + \"\\\" because that field's maximum length is " + lclMaxLength + ".\", " + lclCM.getObjectMutatorArgumentName() + ".length(), " + lclMaxLength + ");");
							lclBW.println("\t\t}");
						}
					}
					
					/* Does this ClassMember have a delegated method for validating the value? */
					if (lclCM.getValidationMethodClassName() != null) {
						/* Yes.  Call that first.  It will throw an Exception if there is a problem, so if execution continues, there was no problem. */
						lclBW.println("\t\t" + lclCM.getValidationMethodClassName() + '.' + lclCM.getValidationMethodName() + "(" + lclCM.getObjectMutatorArgumentName() + ");");
					} else {
						/* No.  We therefore don't call any such method. */
					}
					lclBW.println("\t\tgetNewValues()[" + lclIndex + "] = " + lclCM.getObjectMutatorArgumentName() + ';');
					
					/* After updating this particular field, we need to check to see if any computed fields depend on
					 * the value of this one.  If they do, update those as well. */
					
					for (ClassMember lclDependent : lclCM.getDependents()) {
						if (lclDependent.isMapped() == false) {
							continue;
						}
						Validate.isTrue(lclDependent.isCached());
						lclBW.println("\t\tgetNewValues()[" + lclDependent.getFieldIndex() + "] = " + lclDependent.getSubstitutedComputedExpressionText() + ';');
					}
					
					/* Is this ClassMember *targeted* by any MappedForeignKeys with cascading updates?  If so, we need to update
					 * their values as well.  (And, importantly, bring them into the current TransactionContext.)
					 */					
					for (MappedForeignKey lclMFK : getForeignKeysTo()) {
						if (lclMFK.getUpdateAction() == ReferentialAction.CASCADE) {
							if (lclMFK.getTarget().contains(lclCM)) {
								MappedClass lclSMC = lclMFK.getSourceMappedClass();
								ClassMember lclSCM = lclMFK.getSourceClassMemberMatching(lclCM);
								lclBW.println("\t\t{");
								if (lclMFK.representsOneToOneRelationship()) {
									lclBW.println("\t\t\t" + lclMFK.getSourceOpalClassName() + " lclS = get" + lclMFK.getRoleTargetOpalFieldName() + "();"); // TODO: There should be a method of MFK that returns this
									lclBW.println("\t\t\tif (lclS != null) {"); // We'd really like to be able to check whether it is loaded at all
									lclBW.println("\t\t\t\tSystem.out.println(\"Adding \" + lclS + \" to the TransactionContext.\");");
									lclBW.println("\t\t\t\tlclS." + lclSCM.getObjectMutatorName() + "(" + lclCM.getObjectMutatorArgumentName() + ");");
									lclBW.println("\t\t\t}");
								} else {
									lclBW.println("\t\t\t" + Iterator.class.getName() + "<" + lclSMC.getOpalClassName() + "> lclI = " + lclMFK.getRoleOpalCollectionAccessorName() + "().iterator();");
									lclBW.println("\t\t\twhile (lclI.hasNext()) {");
									/* Note that this depends on the fact that changing the actual field values instead of the references
									 * (e.g., calling setBId() instead of setB()) doesn't try to update/reload the references.
									 */
									lclBW.println("\t\t\t\tSystem.out.println(\"Cascading mutator call!\");");
									lclBW.println("\t\t\t\tlclI.next()." + lclSCM.getObjectMutatorName() + "(" + lclCM.getObjectMutatorArgumentName() + ");");
									lclBW.println("\t\t\t}");
								}
								lclBW.println("\t\t}");
							}
						}
					}

					lclBW.println("\t\t" + getMutatorReturnValue());
					lclBW.println("\t}");
					lclBW.println();
					
					Class<?> lclPrimitiveType = ClassUtils.wrapperToPrimitive(lclMemberType);
					if (lclPrimitiveType == null || lclPrimitiveType.isPrimitive() == false) {
						continue;
					}
					
					/* This mutator accepts a primitive and wraps it as the appropriate Object before calling the
					 * previous Object mutator.  There is no need to synchronize since the Object mutator will be
					 * synchronized. */
					lclBW.println("\tpublic " + determineMutatorReturnType(getOpalClassName()) + ' ' + lclCM.getPrimitiveMutatorName() + "(final " + lclPrimitiveType.getName() + ' ' + lclCM.getPrimitiveMutatorArgumentName() + ") {");
					if (lclMemberType == Boolean.class) {
						lclBW.println("\t\t" + lclCM.getObjectMutatorName() + "(" + lclCM.getPrimitiveMutatorArgumentName() + " ? Boolean.TRUE : Boolean.FALSE);");
					} else {
						lclBW.println("\t\t" + lclCM.getObjectMutatorName() + "(" + OpalUtility.getCodeToConvertToObject(lclPrimitiveType, lclCM.getPrimitiveMutatorArgumentName()) + ");");
					}
					
					lclBW.println("\t\t" + getMutatorReturnValue());
					lclBW.println("\t}");
					lclBW.println();
				}
			}
			
			/* Create the accessors and mutators for the large objects */
			
			lclIndex = -1;
			for (ClassMember lclCM : getLargeClassMembers()) {
				if (!lclCM.isMapped()) {
					continue;
				}
				++lclIndex;
				
				final Class<?> lclMemberType = lclCM.getMemberType();
				final String lclMemberTypeName = OpalUtility.generateTypeName(lclMemberType);
				
				lclBW.println("\tprotected LargeObject " + lclCM.getMemberName() + " = null;");
				lclBW.println();
				
				/* This accessor returns the value of the field as an Object. */
				
				lclBW.println("\tpublic synchronized " + lclMemberTypeName + ' ' + lclCM.getObjectAccessorName() + "() {");
				lclBW.println("\t\tif (" + lclCM.getMemberName() + " == null) {");
				lclBW.println("\t\t\t" + lclCM.getMemberName() + " = new LargeObject();");
				lclBW.println("\t\t}");
				lclBW.println("\t\treturn " + lclCM.getMemberName() + ".get(this, lclIndex);");
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\tpublic synchronized " + determineMutatorReturnType(getOpalClassName()) + ' ' + lclCM.getObjectMutatorName() + "(final " + lclMemberTypeName + ' ' + lclCM.getObjectMutatorArgumentName() + ") {");
				lclBW.println("\t\tif (" + lclCM.getMemberName() + " == null) {");
				lclBW.println("\t\t\t" + lclCM.getMemberName() + " = new LargeObject();");
				lclBW.println("\t\t}");
				lclBW.println("\t\treturn " + lclCM.getMemberName() + ".set(this, lclIndex, " + lclCM.getObjectMutatorArgumentName() + ");");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (hasAtLeastOneBackCollection()) {
				lclBW.println("\tprivate boolean myClearOldCollections = false;");
				lclBW.println();
				lclBW.println("\tprotected boolean needsToClearOldCollections() {");
				lclBW.println("\t\treturn myClearOldCollections;");
				lclBW.println("\t}");
				lclBW.println();
				lclBW.println("\tprotected final void setClearOldCollections(boolean argValue) {");
				lclBW.println("\t\tmyClearOldCollections = argValue;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (isEphemeral() == false) {
				/* Copy old values to new for use when an Opal joins a TransactionContext */
				
				lclBW.println("\t@Override");
				lclBW.println("\tprotected /* synchronized */ void copyOldValuesToNewInternal() {");
				
				/* Copy references that this Opal has to other Opals */
				
				/* "Standard" references created by having a foreign key to another table. */
				for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
					lclBW.println("\t\t" + lclMFK.getNewRoleSourceOpalFieldName() + " = " + lclMFK.getOldRoleSourceOpalFieldName() + ';');
				}
				
				/* Nonstandard references created by another table having a one-to-one foreign key to this table. */
				
				for (MappedForeignKey lclMFK : getForeignKeysTo()) {
					if (lclMFK.representsOneToOneRelationship()) {
						lclBW.println("\t\t" + lclMFK.getNewRoleTargetOpalFieldName() + " = " + lclMFK.getOldRoleTargetOpalFieldName() + ';');
					}
				}
				
				/* Copy collections that this Opal has of other Opals */
				
				lclBW.println("\t\t/* We don't copy Collections of other Opals; they will be cloned as needed. */");
				
				lclBW.println("\t\treturn;");
				lclBW.println("\t}");
				lclBW.println();
				
				/* copyNewValuesToOldInternal is invoked upon a successful commit operation to copy the 
				 * updated ("new") values to the shared, "old" fields. */
				
				lclBW.println("\t@Override");
				lclBW.println("\tprotected /* synchronized */ void copyNewValuesToOldInternal() {");
				
				/* Copy references that this Opal has to other Opals */
				
				for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
					lclBW.println("\t\t" + lclMFK.getOldRoleSourceOpalFieldName() + " = " + lclMFK.getNewRoleSourceOpalFieldName() + ';');
				}
				
				if (getForeignKeyCount() > 0) {
					lclBW.println();
				} else {
					lclBW.println("\t\t/** This Opal has no references to other Opals that need to be copied. */");
				}
				
				/* References that this Opal has to other Opals by virtue of a one-to-one (unique) foreign key originating on
				 * the other table.  In this case the target class also has a field (rather than a Collection).
				 */
				for (MappedForeignKey lclMFK : getForeignKeysTo()) {
					if (lclMFK.representsOneToOneRelationship()) {
						lclBW.println("\t\t" + lclMFK.getOldRoleTargetOpalFieldName() + " = " + lclMFK.getNewRoleTargetOpalFieldName() + ';');
					}
				}
				
				lclBW.println("\t\treturn;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (lclSuperClass == UpdatableOpal.class) {
				/* TODO: Convert this generated code to use the new foreach construct.  This will require an accessor for the
				 * actual collection. */
				lclBW.println("\t@Override");
				if (hasAtLeastOneDeprecatedLink()) {
					lclBW.println("\t@SuppressWarnings(\"deprecation\")");
				}
				lclBW.println("\tprotected void unlinkInternal() {");
				if (getActualTargetForeignKeyCount() > 0) {
					for (MappedForeignKey lclMFK : getForeignKeysTo()) {
						if (lclMFK.representsManyToOneRelationship()) {
							if (lclMFK.hasBackCollection()) {
								if (lclMFK.getSourceMappedClass().isEphemeral() == false) {
									/* THINK: We no longer want to load all collections upon deletion for the sake of looping through and setting foreign keys to null.
									 * However, this means that calling unlink() might lead to a database error if there are still rows that reference the old one.
									 * To be clear, if the Collections are already loaded, we do loop through them, even though setting the reference to NULL is probably
									 * going to fail for most keys (because their columns are NOT NULL).  I'm not really sure what the proper behavior is.
									 */
									
									// Well, that was causing everything to be loaded and deleted, even if we didn't want it to be deleted!  I'm leaving the above code in case R. comes back to it later, but right now I think we're better off checking that the collection isEmpty(), and complaining if it's not. --JHG
									lclBW.println("\t\t" + Validate.class.getName() + ".isTrue(" + lclMFK.getRoleOpalCollectionAccessorName() + "().isEmpty(), \"This object has " + lclMFK.getRoleCollectionItemName() + " children, so it cannot be unlinked.\");");
								}
							} else {
								/* TODO:  If they delete one without a back collection, we'll need to do some complicated
								 * queries to make sure that objects with foreign key references to the deleted object
								 * get set to null (or whatever) */
							}
						}
					}
				}
				
				lclBW.println();
				
				for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
					if (lclMFK.representsOneToOneRelationship()) {
						lclBW.println("\t\tif (get" + lclMFK.getRoleSourceOpalFieldName() + "() != null) {");
						lclBW.println("\t\t\tget" + lclMFK.getRoleSourceOpalFieldName() + "().set" + lclMFK.getRoleTargetOpalFieldName() + "Internal(null);");
						lclBW.println("\t\t}");
					}
				}
				for (MappedForeignKey lclMFK : getForeignKeysTo()) {
					if (lclMFK.representsOneToOneRelationship()) {
						lclBW.println("\t\tif (get" + lclMFK.getRoleTargetOpalFieldName() + "() != null) {");
						lclBW.println("\t\t\tget" + lclMFK.getRoleTargetOpalFieldName() + "().set" + lclMFK.getRoleSourceOpalFieldName() + "Internal(null);");
						lclBW.println("\t\t}");
					}
				}
				for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
					if (lclMFK.representsManyToOneRelationship()) {
						if (lclMFK.hasBackCollection()) {
							lclBW.println("\t\tif (" + lclMFK.getOpalAccessorName() + "() != null) {");
							lclBW.println("\t\t\t" + lclMFK.getOpalAccessorName() + "()." + lclMFK.getRoleOpalCollectionAccessorName() + "().removeInternal(this);");
							lclBW.println("\t\t}");
						}
					}
				}
				
				lclBW.println("\t\treturn;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (isEphemeral() == false) {
				if (lclSuperClass == UpdatableOpal.class) {
					lclBW.println("\t@Override");
					lclBW.println("\tprotected void copyFieldsToInternal(" + UpdatableOpal.class.getName() + '<' + lclICN + "> argTarget) {");
					StringBuilder lclSB = new StringBuilder(1024);
					boolean lclAtLeastOneFieldToCopy = false;
					
					lclCMI = createClassMemberIterator();
					OuterLoop: while (lclCMI.hasNext()) {
						ClassMember lclCM = lclCMI.next();
						if (lclCM.isMapped() == false) {
							continue OuterLoop;
						}
						int lclFI = lclCM.getFieldIndex();
						if (lclCM.getDatabaseColumn().hasDatabaseGeneratedNumber()) {
							lclSB.append("\t\t/* Field " + lclFI + " (" + lclCM.getBaseMemberName() + ") is database generated. */" + System.lineSeparator());
							continue OuterLoop;
						}
						for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
							if (lclMUK.getClassMembers().contains(lclCM)) {
								lclSB.append("\t\t/* Field " + lclFI + " (" + lclCM.getBaseMemberName() + ") is part of a unique key. */" + System.lineSeparator());
								continue OuterLoop;
							}
						}
						if (lclCM.getMemberType() == Timestamp.class) {
							if (lclCM.hasDefault()) {
								lclSB.append("\t\t/* Field " + lclFI + " (" + lclCM.getBaseMemberName() + ") is a date with a default, so we don't copy it. */" + System.lineSeparator());
							} else {
								lclAtLeastOneFieldToCopy = true;
								if (lclCM.isNullAllowed()) {
									lclSB.append("\t\tif (lclValues[" + lclFI + "] == null) {\n");
									lclSB.append("\t\t\tlclTargetNewValues[" + lclFI + "] = null;\n");
									lclSB.append("\t\t} else {\n");
									lclSB.append("\t\t\t");
								} else {
									lclSB.append("\t\t");
								}
								lclSB.append("lclTargetNewValues[" + lclFI + "] = new " + Timestamp.class.getName() + "(((" + Timestamp.class.getName() + ") lclValues[" + lclFI + "]).getTime()); /* " + lclCM.getBaseMemberName() + " (mutable) */" + System.lineSeparator());
								if (lclCM.isNullAllowed()) {
									lclSB.append("\t\t}\n");
								}
							}
						} else {
							/* Assume the field is immutable.  FIXME: We should probably make an explicit list of types here, just so we 
							 * don't get boned by some expecting type in the future.
							 */
							lclAtLeastOneFieldToCopy = true;
							lclSB.append("\t\tlclTargetNewValues[" + lclFI + "] = lclValues[" + lclFI + "]; /* " + lclCM.getBaseMemberName() + " (immutable) */" + System.lineSeparator());
						}
					}
					
					if (lclAtLeastOneFieldToCopy) {
						lclBW.println("\t\tObject[] lclValues = getReadValueSet();");
						lclBW.println("\t\tObject[] lclTargetNewValues = argTarget.getNewValues();");
					}
					
					lclBW.println(lclSB.toString());
					
					lclBW.println("\t\treturn;");
					lclBW.println("\t}");
					lclBW.println();
				}
				
				lclBW.println("\t@Override");
				if (hasAtLeastOneDeprecatedReference()) {
					lclBW.println("\t@SuppressWarnings(\"deprecation\")");
				}
				lclBW.println("\tpublic synchronized void translateReferencesToFields() {");
				if (isUpdatable() || isCreatable()) {
					for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
						lclBW.println("\t\tif (" + lclMFK.getNewRoleSourceOpalFieldName() + " != " + lclMFK.getTargetOpalClassName() + '.' + NOT_YET_LOADED_STATIC_MEMBER_NAME + ") {");
						Iterator<ClassMember> lclJ = lclMFK.getSource().iterator();
						Iterator<ClassMember> lclK = lclMFK.getTarget().iterator();
						while (lclJ.hasNext()) {
							ClassMember lclSCM = lclJ.next();
							ClassMember lclTCM = lclK.next();
							lclBW.println("\t\t\t" + lclSCM.getObjectMutatorName() + "(" + lclMFK.getNewRoleSourceOpalFieldName() + " == null ? null : " + lclMFK.getNewRoleSourceOpalFieldName() + '.' + lclTCM.getObjectAccessorName() + "());");
						}
						lclBW.println("\t\t}");
					}
				}
				lclBW.println("\t\treturn;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			/* Generate the methods that tell the TransactionContext the relative (partial) order in which Opals (technically, TransactionAwares)
			 * need to be committed.
			 */
			
			if (isEphemeral()) {
				/* Nothing to output as EphemeralOpal contains implementations that return empty Sets. */
			} else if (isCreatable() == false && isUpdatable() == false) {
				/* If this Opal cannot be created or updated (or deleted), then it's impossible for it to have any
				 * commitment-ordering issues vis-a-vis other Opals.  There is Nothing to output as ImmutableOpal
				 * contains implementations that return empty Sets. */
			} else {
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + Set.class.getName() + "<" + TransactionAware.class.getName() + "> getRequiredPriorCommits() {");
				
				// .filter(x -> x.getTarget().hasStoreGeneratedClassMember()).
				List<MappedForeignKey> lclFromKeys = getForeignKeysFrom().stream()
						.filter(x -> x.getTargetMappedClass().isUpdatableOpal())
						.sorted()
						.distinct()
						.toList();
				
				if (lclFromKeys.isEmpty()) {
					lclBW.println("\t\treturn "+ Collections.class.getName() + ".emptySet();");
				} else {
					lclBW.println("\t\t" + Set.class.getName() + "<" + TransactionAware.class.getName() + "> lclTAs = null;");
					lclBW.println("\t\t" + UpdatableOpal.class.getName() + "<?> lclUO;");
					boolean lclFirst = true;
					for (MappedForeignKey lclMFK : lclFromKeys) {
						lclBW.println("\t\tlclUO = " + lclMFK.getNewRoleSourceOpalFieldName() + ';');
						lclBW.print("\t\tif ((lclUO != null) ");
						if (lclMFK.getTargetMappedClass() == this) {
							lclBW.print("&& (lclUO != this) ");
						}
						lclBW.println("&& lclUO.isNew()) {");
						if (lclFirst == false) {
							lclBW.println("\t\t\tif (lclTAs == null) {");
						}
						lclBW.println((lclFirst ? "\t\t\t" : "\t\t\t\t") + "lclTAs = new " + Fast3Set.class.getName() + "<>();");
						if (lclFirst == false) {
							lclBW.println("\t\t\t}");
						}
						lclFirst = false;
						lclBW.println("\t\t\tlclTAs.add(lclUO);");
						lclBW.println("\t\t}");
					}
					
					/* I think it's impossible for lclTAs to be non-null but have a size of 0, but we'll check for that anyway. */
					lclBW.println("\t\treturn (lclTAs != null) && (lclTAs.size() > 0) ? lclTAs : " + Collections.class.getName() + ".emptySet();");
				}
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + Set.class.getName() + "<" + TransactionAware.class.getName() + "> getRequiredSubsequentCommits() {");
				if (lclFromKeys.isEmpty()) {
					lclBW.println("\t\treturn " + Collections.class.getName() + ".emptySet();");
				} else {
					if (isCreatable()) {
						lclBW.println("\t\tif (isNew()) {");
						lclBW.println("\t\t\treturn " + Collections.class.getName() + ".emptySet();");
						lclBW.println("\t\t}");
					}
					lclBW.println("\t\t" + Set.class.getName() + "<" + TransactionAware.class.getName() + "> lclTAs = null;");
					lclBW.println("\t\t" + UpdatableOpal.class.getName() + "<?> lclUO;");
					boolean lclFirst = true;
					for (MappedForeignKey lclMFK : lclFromKeys) {
						lclBW.println("\t\tif ((lclUO = " + lclMFK.getOldRoleSourceOpalFieldName() + ") == " + lclMFK.getTargetMappedClass().getOpalClassName() + '.' + NOT_YET_LOADED_STATIC_MEMBER_NAME + ") {");
						lclBW.println("\t\t\tlclUO = " + lclMFK.getOldRoleSourceOpalFieldName() + " = " + lclMFK.getRetrieveOpalMethodName() + "(getOldValues());");
						lclBW.println("\t\t}");
						lclBW.print("\t\tif (lclUO != null ");
						if (lclMFK.getTargetMappedClass() == this) {
							lclBW.print("&& (lclUO != this) ");
						}
						lclBW.println("&& (lclUO.exists() == false)) {");
						if (lclFirst == false) {
							lclBW.println("\t\t\tif (lclTAs == null) {");
						}
						lclBW.println((lclFirst ? "\t\t\t" : "\t\t\t\t") + "lclTAs = new "+ Fast3Set.class.getName() + "<>();");
						if (lclFirst == false) {
							lclBW.println("\t\t\t}");
						}
						lclFirst = false;
						lclBW.println("\t\t\tlclTAs.add(lclUO);");
						lclBW.println("\t\t}");
					}
					
					/* I think it's impossible for lclTAs to be non-null but have a size of 0, but we'll check for that anyway. */
					lclBW.println("\t\treturn (lclTAs != null) && (lclTAs.size() > 0) ? lclTAs : " + Collections.class.getName() + ".emptySet();");
				}
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (isEphemeral() == false) {
				/* getPrimaryKeyWhereClauseValues() */
				lclBW.println("\t@Override");
				lclBW.println("\tpublic Object[] getPrimaryKeyWhereClauseValues() {");
				MappedUniqueKey lclPK = Validate.notNull(getPrimaryKey());
				
				lclBW.println("\t\treturn new Object[] {");
				lclCMI = lclPK.createClassMemberIterator();
				while (lclCMI.hasNext()) {
					ClassMember lclCM = lclCMI.next();
					String lclComment;
					if (isCreatable() || isUpdatable()) {
						if (lclCM.appearsInSourceOfCascadingUpdateForeignKey(this)) {
							lclBW.print("\t\t\t\tgetNewValues()[");
							lclComment = " /* getNewValues since this field appears in a foreign key with cascading update */";
						} else {
							lclBW.print("\t\t\t\tgetOldValues()[");
							lclComment = "";
						}
					} else {
						lclBW.print("\t\t\t\tgetValues()[");
						lclComment = "";
					}
					lclBW.print(lclCM.getFieldIndex());
					lclBW.print("],");
					lclBW.print(lclComment);
					lclBW.println();
				}
				
				lclBW.println("\t\t\t};");
				lclBW.println("\t}");
				lclBW.println();
				
				/* getUniqueStringKeyWhereClauseValues() */
				/* THINK: Should this return values from getNewValues() when it is part of cascading foreign key?  If not, it can't call getPKWCV() */
				lclBW.println("\t@Override");
				lclBW.println("\tpublic Object[] getUniqueStringKeyWhereClauseValues() {");
				MappedUniqueKey lclUSK = Validate.notNull(getUniqueStringKey());
				if (lclUSK == lclPK) {
					lclBW.println("\t\treturn getPrimaryKeyWhereClauseValues();");
				} else {
					lclBW.print("\t\treturn new Object[] {");
					lclCMI = lclUSK.createClassMemberIterator();
					while (lclCMI.hasNext()) {
						ClassMember lclCM = lclCMI.next();
						if (isCreatable() || isUpdatable()) {
							lclBW.print("getOldValues()[");
						} else {
							lclBW.print("getValues()[");
						}
						lclBW.print(lclCM.getFieldIndex());
						lclBW.print("], ");
					}
					
					lclBW.println("};");
				}
				lclBW.println("\t}");
				lclBW.println();
			} else {
				/* THINK: Do we need to generate some sort of analogous method for use with non-ephemeral opals? */
			}
			
			for (Class<?> lclOutput : OUTPUT_CLASSES) {
				lclBW.println("\t@Override");
				lclBW.println("\tpublic synchronized void output(final " + lclOutput.getName() + " argOutput) {");
				
				lclCMI = createClassMemberIterator();
				while (lclCMI.hasNext()) {
					final ClassMember lclCM = lclCMI.next();
					// TODO: Eventually we should store whether or not a field is a large type in ClassMember
					if (lclCM.isMapped() && (lclCM.getMemberType().isArray() == false)) {
						lclBW.println("\t\targOutput.println(\"" + lclCM.getBaseMemberName() + " = \" + " + lclCM.getObjectAccessorName() + "());");
					}
				}
				lclBW.println("\t}");
				lclBW.println();
			}
			
			/* Foreign Keys */
			
			/* Create the internal member variables for the foreign keys */
			
			for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
				
				// ourLogger.debug("...generating member reference for outgoing foreign key " + lclFK);
				MappedClass lclTMC = lclMFK.getTargetMappedClass();
				
				// String lclT = lclTMC.getTypeName();
				String lclON = lclTMC.getOpalClassName();
				String lclV = "lcl" + lclON;
				
				/* The "old" reference will be initialized to the NOT_YET_LOADED object during the superclass
				 * constructor's call to initializeReferences.  The "new" one defaults to null.  If we access
				 * the new values before it is set, the NOT_YET_LOADED reference will be copied over during
				 * the call to copyOldFieldsToNew that occurs when an Opal joins a TransactionContext. */
				
				/* It might seem harmless, but you can't set these references to null.  Those are compiled as instance
				 * initializers which will run *after* the superclass's call to initializeReferences; they will thus
				 * overwrite the setting of the "old" references to the NOT_YET_LOADED objects.
				 */
				lclBW.print(lclMFK.sourceMethodDeprecation());
				lclBW.println("\tprivate " + lclON + ' ' + lclMFK.getOldRoleSourceOpalFieldName() + ';');
				
				/* The new one is null so that new objects do not try lazy evaluation */
				
				if (isEphemeral() == false) {
					lclBW.print(lclMFK.sourceMethodDeprecation());
					lclBW.println("\tprivate " + lclON + ' ' + lclMFK.getNewRoleSourceOpalFieldName() + ';');
					lclBW.println();
				}
				
				lclBW.print(lclMFK.sourceMethodDeprecation());
				lclBW.println("\tprotected " + lclON + ' ' + lclMFK.getRetrieveOpalMethodName() + "(Object[] argValueSet) {");
				lclBW.println("\t\tassert argValueSet != null;");
				lclBW.print("\t\tif (");
				boolean lclFirst = true;
				Iterator<ClassMember> lclJ = lclMFK.getSource().iterator();
				while (lclJ.hasNext()) {
					ClassMember lclCM = lclJ.next();
					if (lclFirst) {
						lclFirst = false;
					} else {
						lclBW.print(" || ");
					}
					lclBW.print("(argValueSet[" + lclCM.getFieldIndex() + "] == null)");
				}
				lclBW.println(") {");
				lclBW.println("\t\t\treturn null;");
				lclBW.println("\t\t}");
				String lclJQFN = lclMFK.getForeignKey().getJoinQueryFactoryName();
				if (lclJQFN == null) {
					lclBW.println("\t\treturn OpalFactoryFactory.getInstance().get" + lclTMC.getOpalFactoryInterfaceName() + "()." + lclMFK.generateUniqueFactoryFunctionCall() + ';');
				} else {
					lclBW.println("\t\treturn OpalFactoryFactory.getInstance().get" + lclTMC.getOpalFactoryInterfaceName() + "().getForQuery(" + lclJQFN + ".getInstance().createSourceToTargetQuery(this.getUserFacing()));");
				}
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.print(lclMFK.sourceMethodDeprecation());
				lclBW.println("\tpublic synchronized " + lclON + " get" + lclMFK.getRoleSourceOpalFieldName() + "() {");
				lclBW.println("\t\t" + lclON + ' ' + lclV + ';');
				if (isEphemeral() == false) {
					lclBW.println("\t\tboolean lclAccess = tryAccess();");
					lclBW.println("\t\t" + lclV + " = lclAccess ? " + lclMFK.getNewRoleSourceOpalFieldName() + " : " + lclMFK.getOldRoleSourceOpalFieldName() + ';');
					lclBW.println("\t\tif (" + lclV + " == " + lclON + '.' + NOT_YET_LOADED_STATIC_MEMBER_NAME + ") {");
					if (isCreatable() == false && isUpdatable() == false) {
						lclBW.println("\t\t\t" + lclV + " = retrieve" + lclMFK.getRoleSourceOpalFieldName() +"(getValues());");
					} else {
						lclBW.println("\t\t\t" + lclV + " = retrieve" + lclMFK.getRoleSourceOpalFieldName() +"(getReadValueSet());");
					}
					lclBW.println("\t\t\tif (lclAccess) {");
					lclBW.println("\t\t\t\t" + lclMFK.getNewRoleSourceOpalFieldName() + " = " + lclV + ';');
					lclBW.println("\t\t\t} else {");
					lclBW.println("\t\t\t\t" + lclMFK.getOldRoleSourceOpalFieldName() + " = " + lclV + ';');
					lclBW.println("\t\t\t}");
					lclBW.println("\t\t}");
				} else {
					lclBW.println("\t\t" + lclV + " = " + lclMFK.getOldRoleSourceOpalFieldName() + ';');
					lclBW.println("\t\tif (" + lclV + " == " + lclON + '.' + NOT_YET_LOADED_STATIC_MEMBER_NAME + ") {");
					lclBW.println("\t\t\t" + lclV + " = retrieve" + lclMFK.getRoleSourceOpalFieldName() +"(getValues());");
					lclBW.println("\t\t\t" + lclMFK.getOldRoleSourceOpalFieldName() + " = " + lclV + ';');
					lclBW.println("\t\t}");
				}
				lclBW.println("\t\treturn " + lclV + ';');
				lclBW.println("\t}");
				lclBW.println();
				
				/* CHECK: We don't need to generate these at all in many cases; can we detect those? */
				if (isEphemeral() == false) {
					String lclMutatorAccessibility = (isCreatable() || isUpdatable()) ? "public" : "/* package */"; // CHECK: Some are not needed at all!
					lclBW.print(lclMFK.sourceMethodDeprecation());
					lclBW.println("\t" + lclMutatorAccessibility + " synchronized " + determineMutatorReturnType(getOpalClassName()) + " set" + lclMFK.getRoleSourceOpalFieldName() + "(" + lclON + " arg" + lclON + ") {");
					lclBW.println("\t\ttryMutate();");
					
					if (lclMFK.representsManyToOneRelationship()) {
						if (lclMFK.hasBackCollection()) {
							lclBW.println("\t\t" + lclON + ' ' + lclV + " = get" + lclMFK.getRoleSourceOpalFieldName() + "();");
							lclBW.println("\t\tif (" + lclV + " == arg" + lclON + ") { " + getMutatorReturnValue() + " }");
							lclBW.println("\t\tif (" + lclV + " != null) {");
							lclBW.println("\t\t\t" + lclV + '.' + lclMFK.getRoleOpalCollectionAccessorName() + "().removeInternal(this);");
							lclBW.println("\t\t}");
							
							lclBW.println("\t\t" + lclMFK.getNewRoleSourceOpalFieldName() + " = arg" + lclON + ';');
							lclBW.println("\t\tif (arg" + lclON + " != null) {");
							lclBW.println("\t\t\targ" + lclON + '.' + lclMFK.getRoleOpalCollectionAccessorName() +"().addInternal(this);");
							lclBW.println("\t\t}");
						} else {
							lclBW.println("\t\t" + lclMFK.getNewRoleSourceOpalFieldName() + " = arg" + lclON + ';');
						}
					} else if (lclMFK.representsOneToOneRelationship()) {
						// Does any of this need to check for whether NULLs are allowed?
						// FIXME: Once we allow Opals to be put in different packages, this may need to specify the package
						lclBW.println("\t\tif (" + lclMFK.getNewRoleSourceOpalFieldName() + " != null && " + lclMFK.getNewRoleSourceOpalFieldName() + " != "  + lclMFK.getTargetMappedClass().getOpalClassName() + '.' + NOT_YET_LOADED_STATIC_MEMBER_NAME + ") {");
						lclBW.println("\t\t\t" + lclMFK.getNewRoleSourceOpalFieldName() + ".set" + lclMFK.getRoleTargetOpalFieldName() + "Internal(null);");
						lclBW.println("\t\t}");
						lclBW.println("\t\t" + lclMFK.getNewRoleSourceOpalFieldName() + " = arg" + lclON + ';');
						lclBW.println("\t\tif (arg" + lclON + " != null) {");
						lclBW.println("\t\t\targ" + lclON + ".set" + lclMFK.getRoleTargetOpalFieldName() + "Internal(this);");
						lclBW.println("\t\t}");
					} else {
						System.out.println("*** Unsure of how to generate set" + lclMFK.getRoleSourceOpalFieldName() + " for " + this + " ***");
					}
					
					lclBW.println("\t\t" + getMutatorReturnValue());
					lclBW.println("\t}");
					lclBW.println();
				
					boolean lclNeedsSetInternal = lclMFK.representsOneToOneRelationship() || lclMFK.representsPolymorphism() || (lclMFK.representsManyToOneRelationship() && lclMFK.hasBackCollection());
					
					if (lclNeedsSetInternal) {
						lclBW.println("\tprotected synchronized void set" + lclMFK.getRoleSourceOpalFieldName() + "Internal(" + lclON + " arg" + lclON + ") {");
						lclBW.println("\t\ttryMutate();");
						lclBW.println("\t\t" + lclMFK.getNewRoleSourceOpalFieldName() + " = arg" + lclON + ';');
						lclBW.println("\t}");
						lclBW.println();
					}
				}
			}
			
			for (MappedForeignKey lclMFK : getForeignKeysTo()) {
				if (lclMFK.representsOneToOneRelationship()) {
					Validate.isTrue(lclMFK.hasBackCollection() == false);
					MappedClass lclSMC = lclMFK.getSourceMappedClass();
					
					String lclON = lclSMC.getOpalClassName();
					String lclA = "arg" + lclON;
					
					lclBW.println("\tprivate " + lclON + ' ' + lclMFK.getOldRoleTargetOpalFieldName() + ';');
					
					/* The new one is null so that new objects do not try lazy evaluation */
					
					lclBW.println("\tprivate " + lclON + ' ' + lclMFK.getNewRoleTargetOpalFieldName() + ';');
					lclBW.println();
					
					lclBW.print(lclMFK.targetMethodDeprecation());
					lclBW.println("\tprotected " + lclON + " retrieve" + lclMFK.getRoleTargetOpalFieldName() + "(Object[] argValueSet) {");
					lclBW.println("\t\tassert argValueSet != null;");
					lclBW.print("\t\tif (");
					boolean lclFirst = true;
					Iterator<ClassMember> lclJ = lclMFK.getTarget().iterator();
					while (lclJ.hasNext()) {
						ClassMember lclCM = lclJ.next();
						if (lclFirst) {
							lclFirst = false;
						} else {
							lclBW.print(" || ");
						}
						lclBW.print("(argValueSet[" + lclCM.getFieldIndex() + "] == null)");
					}
					lclBW.println(") {");
					lclBW.println("\t\t\treturn null;");
					lclBW.println("\t\t}");
					String lclJQFN = lclMFK.getForeignKey().getJoinQueryFactoryName();
					if (lclJQFN == null) {
						lclBW.println("\t\treturn OpalFactoryFactory.getInstance().get" + lclSMC.getOpalFactoryInterfaceName() + "()." + lclMFK.generateSourceUniqueFactoryFunctionCall() + ';');
					} else {
						lclBW.println("\t\treturn OpalFactoryFactory.getInstance().get" + lclSMC.getOpalFactoryInterfaceName() + "().getForQuery(" + lclJQFN + ".getInstance().createTargetToSourceQuery(this.getUserFacing()));");
					}
					lclBW.println("\t}");
					lclBW.println();
					
					String lclV = "lcl" + lclON;
		
					lclBW.print(lclMFK.targetMethodDeprecation());
					lclBW.println("\tpublic synchronized " + lclON + " get" + lclMFK.getRoleTargetOpalFieldName() + "() {");
					lclBW.println("\t\t" + lclON + ' ' + lclV + ';');
					lclBW.println("\t\tboolean lclAccess = tryAccess();");
					lclBW.println("\t\t" + lclV + " = lclAccess ? " + lclMFK.getNewRoleTargetOpalFieldName() + " : " + lclMFK.getOldRoleTargetOpalFieldName() + ';');
					lclBW.println("\t\tif (" + lclV + " == " + lclON + '.' + NOT_YET_LOADED_STATIC_MEMBER_NAME + ") {");
					if (isCreatable() == false && isUpdatable() == false) {
						lclBW.println("\t\t\t" + lclV + " = retrieve" + lclMFK.getRoleTargetOpalFieldName() +"(getValues());");
					} else {
						lclBW.println("\t\t\t" + lclV + " = retrieve" + lclMFK.getRoleTargetOpalFieldName() +"(getReadValueSet());");
					}
					lclBW.println("\t\t\tif (lclAccess) {");
					lclBW.println("\t\t\t\t" + lclMFK.getNewRoleTargetOpalFieldName() + " = " + lclV + ';');
					lclBW.println("\t\t\t} else {");
					lclBW.println("\t\t\t\t" + lclMFK.getOldRoleTargetOpalFieldName() + " = " + lclV + ';');
					lclBW.println("\t\t\t}");
					lclBW.println("\t\t}");
					lclBW.println("\t\treturn " + lclV + ';');
					lclBW.println("\t}");
					lclBW.println();
					
					if (isCreatable() || isUpdatable()) {
						/* THINK: What is the proper way to handle this when we are setting it to null?  If the existing reference X
						 * is not null, do we need to call setXOpalInternal(null) to null it out?
						 */
						lclBW.print(lclMFK.targetMethodDeprecation());
						lclBW.println("\tpublic synchronized " + determineMutatorReturnType(getOpalClassName()) + " set" + lclMFK.getRoleTargetOpalFieldName() + "(" + lclON + ' ' + lclA + ") {");
						lclBW.println("\t\ttryMutate();");
						lclBW.println("\t\t" + lclMFK.getNewRoleTargetOpalFieldName() + " = " + lclA + ';');
						lclBW.println("\t\tif (" + lclA + " != null) {");
						lclBW.println("\t\t\t" + lclA + ".set" + lclMFK.getRoleSourceOpalFieldName() + "Internal(this);");
						lclBW.println("\t\t}");
						lclBW.println("\t\t" + getMutatorReturnValue());
						lclBW.println("\t}");
						lclBW.println();
						
						lclBW.println("\tpublic synchronized void set" + lclMFK.getRoleTargetOpalFieldName() + "Internal(" + lclON + ' ' + lclA + ") {");
						lclBW.println("\t\ttryMutate();");
						lclBW.println("\t\t" + lclMFK.getNewRoleTargetOpalFieldName() + " = " + lclA + ';');
						lclBW.println("\t}");
						lclBW.println();
					}
				}
			}
			
			/* Create member Collections for foreign keys that have this class as their target */
			
			if (isEphemeral() == false) {
				for (MappedForeignKey lclMFK : getForeignKeysTo()) {
					MappedClass lclSMC = lclMFK.getSourceMappedClass();
					
					if (lclMFK.representsManyToOneRelationship()) {
						if (lclMFK.hasBackCollection()) {
							String lclON = lclSMC.getOpalClassName();
							String lclDeclaredTypeName = OpalBackCollectionSet.class.getName();
							ReferenceType lclRT = lclMFK.getCollectionReferenceType();
							// String lclA = "arg" + lclON;
							String lclC = lclMFK.getRoleCollectionMemberName();
							String lclCA = lclMFK.getRoleOpalCollectionAccessorName();
							String lclLoaderType = OpalBackCollectionLoader.class.getName();
							String lclTA = "<" + lclON + ", " + getOpalClassName() + ">";
							String lclFieldTypeWithReference;
							Class<?> lclReferenceClass = lclRT.getReferenceClass(); // null for hard references
							switch (lclRT) {
								case HARD:
									lclFieldTypeWithReference = lclDeclaredTypeName + lclTA;
									break;
								case SOFT:
									lclFieldTypeWithReference = lclReferenceClass.getName() + "<" + lclDeclaredTypeName + lclTA + ">";
									break;
								case WEAK:
									lclFieldTypeWithReference = lclReferenceClass.getName() + "<" + lclDeclaredTypeName + lclTA + ">";
									break;
								default: throw new IllegalStateException();
							}
							String lclLoaderMemberName = lclMFK.getRoleCollectionLoaderName();
							String lclChildFactoryAccessorName = "get" + lclSMC.getOpalFactoryInterfaceName();
							String lclLoaderMethodName = lclMFK.getOpalFactoryOpalLoaderMethodName();
							String lclCounterMethodName = lclMFK.getOpalFactoryOpalCounterMethodName();
							String lclNoArgSetCreatorMethodName = lclSMC.getApplicationPackage() + ".FactoryMap." + AbstractFactoryMap.NO_ARG_CTOR_SET_CREATOR_METHOD_NAME;
							String lclCollectionSetCreatorMethodName = lclSMC.getApplicationPackage() + ".FactoryMap." + AbstractFactoryMap.COLLECTION_ARG_CTOR_SET_CREATOR_METHOD_NAME;
							
							if (lclSMC.isEphemeral() == false) {
								lclBW.print(lclMFK.targetMethodDeprecation());
								lclBW.println("\tprivate " + lclFieldTypeWithReference + ' ' + lclC + " = null;");
								lclBW.println();
								lclBW.print(lclMFK.targetMethodDeprecation());
								lclBW.println("\tprivate static final " + lclLoaderType + lclTA + ' ' + lclLoaderMemberName + " = ");
								lclBW.println("\t\t\tnew " + lclLoaderType + "<>(");
								lclBW.println("\t\t\t\t\t" + "OpalFactoryFactory.getInstance()." + lclChildFactoryAccessorName + "()::" + lclLoaderMethodName + ",");
								lclBW.println("\t\t\t\t\t" + "OpalFactoryFactory.getInstance()." + lclChildFactoryAccessorName + "()::" + lclCounterMethodName + ",");
								lclBW.println("\t\t\t\t\t" + lclON + "::" + lclMFK.getSafeOpalMutatorName() + ",");
								lclBW.println("\t\t\t\t\t" + lclON + "::" + lclMFK.getUnsafeOpalMutatorName() + ",");
								lclBW.println("\t\t\t\t\t" + lclON + "::" + lclMFK.getOpalAccessorName() + ",");
								lclBW.println("\t\t\t\t\t" + lclNoArgSetCreatorMethodName + "(),");
								lclBW.println("\t\t\t\t\t" + lclCollectionSetCreatorMethodName + "(),");
								lclBW.println("\t\t\t\t\t" + String.valueOf(lclMFK.areAllSourceClassMembersNullable()));
								lclBW.println("\t\t\t\t\t);");
								lclBW.println();
								
								lclBW.print(lclMFK.targetMethodDeprecation());
								lclBW.println("\t/* package */ synchronized " + lclDeclaredTypeName + lclTA + ' ' + lclCA + "() {");
								switch (lclRT) {
									case HARD:
										lclBW.println("\t\tif (" + lclC + " == null) {");
										lclBW.println("\t\t\t" + lclC + " = new " + OpalBackCollectionDoubleSet.class.getName() + "<>(");
										lclBW.println("\t\t\t\tthis,");
										lclBW.println("\t\t\t\t" + lclLoaderMemberName + ",");
										lclBW.println("\t\t\t\tisNew()"); // Preclude database queries known to load no rows (since the Opal is new)
										lclBW.println("\t\t\t);");
										lclBW.println("\t\t}");
										lclBW.println("\t\treturn " + lclC + ";");
										break;
									case SOFT:
									case WEAK:
										Validate.notNull(lclReferenceClass);
										lclBW.println("\t\t" + lclDeclaredTypeName + lclTA + " lclS;");
										lclBW.println("\t\t" + lclFieldTypeWithReference + " lclR = " + lclC + ";");
										lclBW.println("\t\tif (lclR == null || ((lclS = lclR.get()) == null)) {");
										lclBW.println("\t\t\tlclS = new " + OpalBackCollectionDoubleSet.class.getName() + "<>(");
										lclBW.println("\t\t\t\tthis,");
										lclBW.println("\t\t\t\t" + lclLoaderMemberName + ",");
										lclBW.println("\t\t\t\tisNew()"); // Preclude database queries known to load no rows (if the Opal is new)
										lclBW.println("\t\t\t);");
										lclBW.println("\t\t\t" + lclC + " = new " + lclReferenceClass.getName() + "<>(lclS);");
										lclBW.println("\t\t}");
										lclBW.println("\t\treturn lclS;");
																				
								}
								lclBW.println("\t}");
								lclBW.println();
							}
							
						}
					}
				}
			}
			
			/* TODO: Class invariant-checking method */
			
			if (getPrimaryKey() != null) {
				lclBW.println("\t@Override");
				lclBW.println("\tpublic java.lang.String toString() {");
				lclBW.println("\t\tjava.lang.StringBuilder lclSB = new java.lang.StringBuilder(64);");
				lclBW.println("\t\tlclSB.append(\"" + getOpalClassName() + "[\");");
				MappedUniqueKey lclMUK = getPrimaryKey();
				lclCMI = lclMUK.createClassMemberIterator();
				boolean lclFirst = true;
				while (lclCMI.hasNext()) {
					ClassMember lclCM = lclCMI.next();
					if (lclFirst) {
						lclFirst = false;
					} else {
						lclBW.println("\t\tlclSB.append(',');");
					}
					lclBW.println("\t\tlclSB.append(\"" + lclCM.getMemberName() + "=\");");
					lclBW.println("\t\tlclSB.append(toStringField(" + lclCM.getFieldIndex() + "));");
				}
				
				lclBW.println("\t\tlclSB.append(']');");
				lclBW.println("\t\treturn lclSB.toString();");
				lclBW.println("\t}");
				lclBW.println();
			} else {
				/* FIXME: Implement some sort of usable toString in the absence of a primary key. */
			}
			
			if (isEphemeral() == false) {
				if (getForeignKeyCount() > 0) {
					lclBW.println("\t@Override");
					lclBW.println("\tprotected void updateReferencesAfterReload() {");
					for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
						String lclTargetOpal = lclMFK.getTargetOpalClassName();
						String lclNewMember = lclMFK.getNewRoleSourceOpalFieldName();
						lclBW.println("\t\tif (" + lclNewMember + " != " + lclTargetOpal + '.' + NOT_YET_LOADED_STATIC_MEMBER_NAME + ") {");
						if (isCreatable() == false && isUpdatable() == false) {
							lclBW.println("\t\t\tset" + lclMFK.getRoleSourceOpalFieldName() + "(retrieve" + lclMFK.getRoleSourceOpalFieldName() + "(getValues()));");
						} else {
							lclBW.println("\t\t\tset" + lclMFK.getRoleSourceOpalFieldName() + "(retrieve" + lclMFK.getRoleSourceOpalFieldName() + "(getNewValues()));");
						}
						lclBW.println("\t\t}");
					}
			
					lclBW.println("\t}");
					lclBW.println();
				}
				
				if (getActualTargetForeignKeyCount() > 0) {
					lclBW.println("\t@Override");
					lclBW.println("\tprotected void updateCollectionsAfterReload() {");
					lclBW.println("\t\tassert needsToClearOldCollections() == false;");
					lclBW.println("\t\tsetClearOldCollections(true);");
					lclBW.println("\t}");
					lclBW.println();
				}
			}
			
			lclBW.println('}');
			
			lclBW.close();
		}
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
	
	protected void createOpalFactory() throws IOException {
		/* Create the Opal factory */
		
		String lclOpalFactoryClassFileName = StringUtility.makeFilename(getSourceDirectory(), getOpalFactoryPackageName(), getOpalFactoryInterfaceName());
		
		File lclOpalFactoryClassFile = new File(lclOpalFactoryClassFileName);
		
		try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclOpalFactoryClassFile)))) {
			final String lclICN = getInterfaceClassName();
			final String lclOCN = getOpalClassName();
			
			lclBW.println("package " + getOpalFactoryPackageName() + ';');
			lclBW.println("import " + getFullyQualifiedInterfaceClassName() + ';');
			if (getMappedUniqueKeys().isEmpty() == false || hasAtLeastOneBackCollection()) {
				lclBW.println("import " + PersistenceException.class.getName() + ';');
			}
			lclBW.println();
			
			Class<?> lclOpalFactoryClass = isEphemeral() ? OpalFactory.class : IdentityOpalFactory.class;
			
			lclBW.println("@javax.annotation.Generated(\"com.opal\")");
			if (isDeprecated()) {
				lclBW.println("@Deprecated");
			}
			lclBW.print("public interface " + getOpalFactoryInterfaceName() + " extends " + lclOpalFactoryClass.getName() + "<" + lclICN + ", " +lclOCN + ">");
			if (implementsPolymorphicCreator()) {
				MappedClass lclUCTD = getPolymorphicData().getUltimateConcreteTypeDeterminer();
				lclBW.print(", " + OpalFactoryPolymorphicCreator.class.getName() + "<" + lclICN + ", " + lclOCN + ", " + lclUCTD.getFullyQualifiedInterfaceClassName() + ">");
			} else if (implementsCreator()) {
				lclBW.print(", " + OpalFactoryCreator.class.getName() + "<" + lclICN + ", " + lclOCN + ">");
			} else {
				/* Nothing. */
			}
			lclBW.println(" {");
			
			lclBW.println();
			
			/* Create a factory method for each foreign key */
			for (MappedForeignKey lclMFK : getForeignKeysFrom()) {
				if (lclMFK.hasBackCollection() == false) {
					continue;
				}
				lclMFK.generateOpalFactoryFieldLoader(lclBW);
				lclMFK.generateOpalFactoryOpalLoader(lclBW);
				lclMFK.generateOpalFactoryOpalCounter(lclBW);
			}
			
			/* Create a factory method for each unique key */
			for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
				lclBW.println("\t" + lclMUK.generateOpalFactoryMethodDefinition(false));
				
				/* If this class has subclasses, then we also need to create a factory method for the primary key that knows that if it needs to instantiate
				 * the object, it actually *is* of this exact type (and not a subtype).
				 */
				
				if (needsConcreteFactoryMethod()) {
					if (lclMUK.isPrimaryKey()) {
						lclBW.println("\t" + lclMUK.generateOpalFactoryMethodDefinition(true));
					}
				}
			}
			
			/* Create a createSuperclassOpal() method if we have creatable subclasses.  This method is not required by any of the interfaces
			 * that this OpalFactory implements, it is called directly by the createSuperclassOpals() method of Opals that map subtables of
			 * that mapped by this Opal. */
			
			if (isCreatable() && hasSubclasses()) {
				lclBW.println("\tpublic " + lclOCN + " createAsSuperOpal("+ lclICN + " argUF);");
				lclBW.println();
			}
			/* If there were no other content, output a comment to avoid warnings. */
			
			if (hasAtLeastOneBackCollection() == false && getMappedUniqueKeys().isEmpty()) {
				lclBW.println("\t/* This Opal has neither references nor single lookups, so this class has ");
				lclBW.println("\t   no actual content.");
				lclBW.println("\t*/");
			}
			
			/* End of the class */
			
			lclBW.println("}");
		} // Closes lclBW via try-with-resources
	}
	
	public void createMoneOpalFactory() throws IOException {
		/* Create the Opal factory */
		String lclMoneOpalFactoryClassName = getMoneOpalFactoryClassName();
		String lclMoneOpalFactoryPackageName = getMoneOpalFactoryPackageName();
		
		String lclDirectory = StringUtility.makeDirectoryName(getSourceDirectory(), lclMoneOpalFactoryPackageName);
		
		MappedClass.ensureDirectoryExists(new File(lclDirectory));
		
		String lclMoneOpalFactoryClassFileName = StringUtility.makeFilename(
			getSourceDirectory(),
			lclMoneOpalFactoryPackageName,
			lclMoneOpalFactoryClassName
		);
		
		File lclMoneOpalFactoryClassFile = new File(lclMoneOpalFactoryClassFileName);
		
		try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclMoneOpalFactoryClassFile)))) {
			final String lclICN = getInterfaceClassName();
			final String lclOCN = getOpalClassName();
			
			lclBW.println("package " + lclMoneOpalFactoryPackageName + ';');
			lclBW.println();
			if (isEphemeral() == false) {
				lclBW.println("import " + OpalCache.class.getName() + ';');
				lclBW.println("import " + OpalKey.class.getName() + ';');
				lclBW.println("import " + PersistenceException.class.getName() + ';');
				lclBW.println("import " + TransactionParameter.class.getName() + ';');
			}
			
			lclBW.println();
			
			if (getPolymorphicData() instanceof SubtablePolymorphicData) {
				lclBW.println("import " +  getOpalFactoryPackageName() + ".OpalFactoryFactory;");
			}
			lclBW.println("import " + getFullyQualifiedInterfaceClassName() + ';');
			if (isAbstract() == false) {
				lclBW.println("import " + getFullyQualifiedImplementationClassName() + ';');
			}
			// FIXME: Sometimes we'll also need to import the SpecifiedImplementationClassName
			lclBW.println("import " + getFullyQualifiedOpalClassName() + ';');
			lclBW.println("import " + getFullyQualifiedOpalFactoryInterfaceName() + ';');
			lclBW.println();
			
			lclBW.println("@javax.annotation.Generated(\"com.opal\")");
			if (isDeprecated()) {
				lclBW.println("@Deprecated");
			}
			
			/* At some point, we need to figure out whether ephemeral Mone opals are allowed. */
			Class<?> lclFactoryClass = isEphemeral() ? null : AbstractMoneIdentityOpalFactory.class; 
			assert lclFactoryClass != null;
			
			lclBW.println("public class " + lclMoneOpalFactoryClassName + " extends " + lclFactoryClass.getName() + "<" + lclICN + ", " + lclOCN + "> implements " + getOpalFactoryInterfaceName() + " {");
			
			lclBW.println("\tprivate static final " + lclMoneOpalFactoryClassName + " ourInstance = new " + lclMoneOpalFactoryClassName + "();");
			lclBW.println();
			lclBW.println("\tpublic static final " + lclMoneOpalFactoryClassName + " getInstance() { return ourInstance; }");
			lclBW.println();
			
			/* Create the do-nothing constructor.  It's protected because all of these classes are Singletons. */
			lclBW.println("\tprotected " + lclMoneOpalFactoryClassName + "() {");
			lclBW.println("\t\tsuper();");
			lclBW.println("\t}");
			lclBW.println();
			
			Iterator<ClassMember> lclI;
			
			MappedUniqueKey lclPK = getPrimaryKey();
			
			lclBW.println("\t@Override");
			lclBW.println("\tprotected String[] getFieldNames() { return " + getOpalClassName() + ".getStaticFieldNames(); }");
			lclBW.println();
			
			lclBW.println("\t@Override");
			lclBW.println("\tprotected Class<?>[] getFieldTypes() { return " + getOpalClassName() + ".getStaticFieldTypes(); }");
			lclBW.println();
			
			lclBW.println("\t@Override");
			lclBW.println("\tprotected boolean[] getFieldNullability() { return " + getOpalClassName() + ".getStaticFieldNullability(); }");
			lclBW.println();
			
			lclBW.println("\t@Override");
			lclBW.println("\tprotected com.opal.FieldValidator[] getFieldValidators() { return " + getOpalClassName() + ".getStaticFieldValidators(); }");
			lclBW.println();
			
			lclBW.println("\t@Override");
			lclBW.println("\tprotected " + lclOCN + " instantiate(Object[] argValues) {");
			lclBW.println("\t\treturn new " + lclOCN + "(this, argValues);");
			lclBW.println("\t}");
			lclBW.println();
				
			if (isCreatable() && hasSubclasses()) {
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + lclOCN + " createAsSuperOpal(" + lclICN + " argUF) {");
				lclBW.println("\t\tassert argUF != null;");
				lclBW.println("\t\t" + lclOCN + " lclOpal;");
				lclBW.println("\t\tsynchronized (lclOpal = instantiate((Object[]) null)) {");
				// setUserFacing must be called first so that createSuperclassOpals (called by lclOpal.newObject() can propagate it upward
				lclBW.println("\t\t\tassert lclOpal.getUserFacing() == null;");
				lclBW.println("\t\t\tlclOpal.setUserFacing(argUF);");
				lclBW.println("\t\t\tlclOpal.newObject();");
				lclBW.println("\t\t\tnewObject(lclOpal);");
				lclBW.println("\t\t}");
				lclBW.println("\t\treturn lclOpal;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (isCreatable() && isConcrete()) {
				if (getPolymorphicData() == null) {
					lclBW.println("\t@Override");
				}
				lclBW.println("\tpublic " + lclOCN + " create() {");
				lclBW.println("\t\t" + lclOCN + " lclOpal;");
				lclBW.println("\t\tsynchronized (lclOpal = instantiate((Object[]) null)) {");
				// setUserFacing must be called first so that createSuperclassOpals (called by lclOpal.newObject() can propagate it upward
				lclBW.println("\t\t\tassert lclOpal.getUserFacing() == null;");
				lclBW.println("\t\t\tlclOpal.setUserFacing(new " + getImplementationClassName() + "(lclOpal));");
				lclBW.println("\t\t\tlclOpal.newObject();");
				lclBW.println("\t\t\tnewObject(lclOpal);");
				lclBW.println("\t\t}");
				lclBW.println("\t\treturn lclOpal;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (requiresTypedCreate() && implementsPolymorphicCreator()) {
				PolymorphicData lclPD = getPolymorphicData();
				Validate.notNull(lclPD);
				MappedClass lclTDMC = lclPD.getUltimateConcreteTypeDeterminer();
				Validate.notNull(lclTDMC);
				String lclTDMCICN = lclTDMC.getFullyQualifiedInterfaceClassName();
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + lclOCN + " create(" + lclTDMCICN + " argT) {");
				if (lclPD instanceof SingleTablePolymorphicData) {
					lclBW.println("\t\tassert argT != null;");
					lclBW.println("\t\t" + lclOCN + " lclOpal;");
					lclBW.println("\t\tsynchronized (lclOpal = instantiate((Object[]) null)) {");
					lclBW.println("\t\t\tassert lclOpal.getUserFacing() == null;");
					lclBW.println("\t\t\t" + JavaClass.class.getName() + "<" + lclICN + "> lclJavaClass = argT.getClassName();"); /* FIXME: This won't always be the method name! */
					lclBW.println("\t\t\t" + lclICN + " lclUF = lclJavaClass.newInstance(lclOpal);");
					lclBW.println("\t\t\tlclOpal.setUserFacing(lclUF);");
					lclBW.println("\t\t\tlclOpal.newObject();");
					lclBW.println("\t\t\tnewObject(lclOpal);");
					SingleTablePolymorphicData lclSTPD = (SingleTablePolymorphicData) lclPD;
					if (lclSTPD.getDereferenceKeys().size() == 1) {
						MappedForeignKey lclMFK = lclSTPD.getDereferenceKeys().get(0);
						lclBW.println("\t\t\tlclUF." + lclMFK.getMutatorName() + "(argT);");
					}
					lclBW.println("\t\t}");
					lclBW.println("\t\treturn lclOpal;");
				} else {
					lclBW.println("\t\tthrow new IllegalStateException(\"This overload of create should only be used with SingleTablePolymorphism.\");");
				}
				lclBW.println("\t}");
				lclBW.println();
			}
			
			/* FIXME: Test Mone Opals with being ephemeral. */
			/* FIXME: Test Mone Opals with being polymorphic (both ways). */
			/* FIXME: This huge determineUserFacing method is shared with RelationalDatabaseAdaptor.  We should share code. */
			lclBW.println("\t@Override");
			lclBW.println("\tprotected void determineUserFacing(" + lclOCN + " argO, boolean argConcrete) {");
			lclBW.println("\t\tassert argO != null;");
			PolymorphicData lclPD = getPolymorphicData();
			if (lclPD == null) {
				// lclBW.println("\t\tassert argO.getUserFacing() == null;");
				if (hasSubclasses() == false) {
					lclBW.println("\t\tassert argConcrete == false;");
					lclBW.println("\t\tassert argO.getUserFacing() == null;");
					if (getSpecifiedImplementationClassName() != null) {
						lclBW.println("\t\targO.setUserFacing(new " + getSpecifiedImplementationClassName() + "(argO));");
					} else {
						lclBW.println("\t\targO.setUserFacing(new " + getImplementationClassName() + "(argO));");
					}
				} else {
					if (isAbstract()) {
						lclBW.println("\t\tassert argConcrete == false; // This type is abstract");
						lclBW.println("\t\targO.setUserFacing((" + lclICN + ") argO.get" + getSuperclassKey().getRoleSourceOpalFieldName() + "().getUserFacing());");
					} else {
						lclBW.println("\t\tif (argConcrete) {");
						lclBW.println("\t\t\tif (argO.getUserFacing() != null) {");
						lclBW.println("\t\t\t\treturn;");
						lclBW.println("\t\t\t} else {");
						if (getSpecifiedImplementationClassName() != null) {
							lclBW.println("\t\t\t\targO.setUserFacing(new " + getSpecifiedImplementationClassName() + "(argO));");
						} else {
							lclBW.println("\t\t\t\targO.setUserFacing(new " + getImplementationClassName() + "(argO));");
						}
						lclBW.println("\t\t\t}");
						lclBW.println("\t\t} else {");
						lclBW.println("\t\t\tassert argO.getUserFacing() == null;");
						lclBW.println("\t\t\targO.setUserFacing((" + lclICN + ") argO.get" + getSuperclassKey().getRoleSourceOpalFieldName() + "().getUserFacing());");
						lclBW.println("\t\t}");
					}
					/* FIXME: Would it be better to "trace up and over" like we do when we have a direct Polymorphism record? */
				}
			} else if (lclPD instanceof SingleTablePolymorphicData) {
				lclBW.println("\t\tassert argConcrete == false;");
				lclBW.println("\t\tassert argO.getUserFacing() == null;");
				SingleTablePolymorphicData lclSPD = (SingleTablePolymorphicData) lclPD;
				String lclNV = null;
				MappedClass lclMappedClassWithActualTypeField = this;
				for (int lclJ = 0; lclJ < lclSPD.getDereferenceKeys().size(); ++lclJ) {
					MappedForeignKey lclMFK = lclSPD.getDereferenceKeys().get(lclJ);
					assert lclMFK.getSourceMappedClass() == lclMappedClassWithActualTypeField;
					lclMappedClassWithActualTypeField = lclMFK.getTargetMappedClass();
					String lclOV = (lclJ == 0) ? "argO" : lclNV;
					lclNV = "lcl" + lclJ;
					lclBW.println("\t\t" + lclMFK.getTargetMappedClass().getFullyQualifiedOpalClassName() + ' ' + lclNV + " = " + lclOV + ".get" + lclMFK.getRoleSourceOpalFieldName() + "();");
					lclBW.println("\t\tif (" + lclNV + " == null) {");
					lclBW.println("\t\t\tthrow new IllegalStateException();");
					lclBW.println("\t\t}");
				}
				ClassMember lclCM = lclMappedClassWithActualTypeField.getClassMemberByColumnName(lclSPD.getColumnName());
				if (lclCM == null) {
					String lclS = "Could not find column \"" + lclSPD.getColumnName() + "\" for table " + lclMappedClassWithActualTypeField.getTableName() + '.';
					complain(MessageLevel.Fatal, lclS);
					throw new IllegalStateException(lclS);
				}
				
				int lclFieldIndex = lclCM.getFieldIndex();
				lclBW.println("\t\tObject lclV = " + lclNV + ".getField(" + lclFieldIndex + ");");
				lclBW.println("\t\tif (lclV == null) {");
				lclBW.println("\t\t\tthrow new IllegalStateException(\"Column that should contain information about which concrete Class to instantiate was null.\");");
				lclBW.println("\t\t}");
				switch (lclSPD.getMethod()) {
				case "Class":
					if (lclCM.getMemberType() == String.class) {
						lclBW.println("\t\tString lclClassName = (String) " + lclNV + ".getField(" + lclFieldIndex + ");");
						/* FIXME: Check for not a string */
						/* FIXME: Build some sort of lookup table on class creation */
						/* FIXME: Check for not being a class */
						/* FIXME: Check for not having the right interface */
						/* FIXME: Compile-time checks for missing classes or classes with the wrong interface? */
						lclBW.println("\t\tlclClass = Class.forName(lclClassName);");
						lclBW.println("\t\tlclUF = lclClass.newInstance(argO);");
					} else if (lclCM.getMemberType() == JavaClass.class) {
						lclBW.println("\t\tif ((lclV instanceof " + JavaClass.class.getName() + ") == false) {");
						lclBW.println("\t\t\tthrow new IllegalStateException(\"Class column contained \" + lclV + \", which was not a JavaClass.\");");
						lclBW.println("\t\t}");
						lclBW.println("\t\t@SuppressWarnings(\"unchecked\")");
						lclBW.println("\t\t" + JavaClass.class.getName() + "<" + lclICN + "> lclJavaClass = (" + JavaClass.class.getName() + "<" + lclICN + ">) lclV;");
					} else {
						throw new IllegalStateException("Could not use type " + lclCM.getMemberType() + " to figure out the class to construct when doing single-table polymorphism for " + this);
					}
					break;
				default:
					throw new UnimplementedOperationException();
				}
				/* FIXME: Check for NULL */
				lclBW.println("\t\targO.setUserFacing(lclUF);");
			} else if (lclPD instanceof SubtablePolymorphicData) {
				lclBW.println("\t\tassert argConcrete == false;");
				lclBW.println("\t\tassert argO.getUserFacing() == null;");
				SubtablePolymorphicData lclSPD = (SubtablePolymorphicData) lclPD;
				String lclNV = null;
				MappedClass lclMappedClassWithActualTypeField = this;
				for (int lclJ = 0; lclJ < lclSPD.getDereferenceKeys().size(); ++lclJ) {
					MappedForeignKey lclMFK = lclSPD.getDereferenceKeys().get(lclJ);
					assert lclMFK.getSourceMappedClass() == lclMappedClassWithActualTypeField;
					lclMappedClassWithActualTypeField = lclMFK.getTargetMappedClass();
					String lclOV = (lclJ == 0) ? "argO" : lclNV;
					lclNV = "lcl" + lclJ;
					lclBW.println("\t\t" + lclMFK.getTargetMappedClass().getFullyQualifiedOpalClassName() + ' ' + lclNV + " = " + lclOV + ".get" + lclMFK.getRoleSourceOpalFieldName() + "();");
					lclBW.println("\t\tif (" + lclNV + " == null) {");
					lclBW.println("\t\t\tthrow new IllegalStateException();");
					lclBW.println("\t\t}");
				}
				ClassMember lclCM = lclMappedClassWithActualTypeField.getClassMemberByColumnName(lclSPD.getColumnName());
				if (lclCM == null) {
					String lclS = "Could not find column \"" + lclSPD.getColumnName() + "\" for table " + lclMappedClassWithActualTypeField.getTableName() + '.';
					complain(MessageLevel.Fatal, lclS);
					throw new IllegalStateException(lclS);
				}
				
				int lclFieldIndex = lclCM.getFieldIndex();
				lclBW.println("\t\t" + lclICN + " lclUF;");
				switch (lclSPD.getMethod()) {
				case "Table":
					if (lclCM.getMemberType() == String.class) {
						lclBW.println("\t\tString lclTableName = (String) " + lclNV + ".getField(" + lclFieldIndex + ");");
						/* FIXME: Check for null */
						/* FIXME: Check for not a string */
						/* FIXME: Build some sort of lookup table on class creation */
						boolean lclFirstClause = true;
						for (MappedClass lclSubclass : getSubclasses()) { /* FIXME: What about intermediate abstract ones? */
							if (lclSubclass.isAbstract()) {
								continue;
							}
							lclBW.print("\t\t");
							if (lclFirstClause) {
								lclFirstClause = false;
							} else {
								lclBW.print("} else ");
							}
							/* FIXME: Is there a better way to get access to the table name? */
							lclBW.println("if (lclTableName.equalsIgnoreCase(\"" + lclSubclass.getTableName().getTableName() + "\")) {");
							if (lclSubclass == this) {
								/* FIXME: The following line needs to be augmented to work with SingleTablePolymorphism */
								lclBW.println("\t\t\tlclUF = new " + getImplementationClassName() + "(argO);");
							} else {
								lclBW.println("\t\t\t" + lclSubclass.getOpalFactoryPackageName() + '.' + lclSubclass.getOpalFactoryInterfaceName() + " lclOF = OpalFactoryFactory.getInstance().get" + lclSubclass.getOpalFactoryInterfaceName() + "();");
								lclBW.println("\t\t\tassert lclOF != null;");
								lclBW.print("\t\t\t" + lclSubclass.getFullyQualifiedOpalClassName() + " lclO = lclOF.");
								StringBuilder lclArgs = new StringBuilder(128);
								MappedUniqueKey lclSuperclassPK = getPrimaryKey();
								MappedUniqueKey lclSubclassPK = lclSubclass.getPrimaryKey();
								lclBW.print(lclSubclassPK.generateOpalFactoryMethodName(lclSubclass.needsConcreteFactoryMethod()));
								lclBW.print('(');
								
								assert lclSuperclassPK.getClassMembers().size() == lclSubclassPK.getClassMembers().size();
								boolean lclFirst = true;
								for (int lclJ = 0; lclJ < lclSuperclassPK.getClassMembers().size(); ++lclJ) {
									ClassMember lclCM1 = lclSuperclassPK.getClassMembers().get(lclJ);
									ClassMember lclCM2 = lclSubclassPK.getClassMembers().get(lclJ);
									if (lclCM2.getMemberType().isAssignableFrom(lclCM1.getMemberType()) == false) {
										throw new IllegalStateException("Argument type " + lclCM2.getMemberType().getName() + " is not assignable from value " + lclCM1.getMemberType().getName() + '.');
									}
									if (lclFirst) {
										lclFirst = false;
									} else {
										lclArgs.append(", ");
									}
									lclArgs.append('(');
									lclArgs.append(lclCM2.getMemberType().getName());
									lclArgs.append(") ");
									lclArgs.append("argO.getField(");
									lclArgs.append(lclCM1.getFieldIndex());
									lclArgs.append(')');
								}
								lclBW.println("(" + lclArgs.toString() + "));");
								lclBW.println("\t\t\tif (lclO == null) {");
								lclBW.println("\t\t\t\tthrow new PersistenceException(\"Polymorphism data for \" + argO + \" suggested that there should be a subclass row in " + lclSubclass.getTableName() + ", but none was found.\");");
								lclBW.println("\t\t\t}");
								lclBW.println("\t\t\tlclUF = lclO.getUserFacing();");
							}
						}
						lclBW.println("\t\t} else {");
						lclBW.println("\t\t\tthrow new PersistenceException(\"While instantiating Opal \" + argO + \" it was necessary to populate subclass information from table \" + lclTableName + \" but that table doesn't have an associated Opal (that is properly configured as a subclass).\");");
						lclBW.println("\t\t}");
						lclBW.println("\t\tif (lclUF == null) {");
						lclBW.println("\t\t\tthrow new PersistenceException(\"While instantiating Opal \" + argO + \" it was necessary to populate subclass information from table \" + lclTableName + \" but no row corresponding to the superclass primary key values was found.\");");
						lclBW.println("\t\t}");
					}
					break;
				default:
					throw new UnimplementedOperationException();
				}
				/* FIXME: Check for NULL */
				lclBW.println("\t\targO.setUserFacing(lclUF);");
			}
			
			lclBW.println("\t}");
			lclBW.println();
			
			if (isEphemeral() == false) {
				
				lclBW.println("\tprotected void newObject(@SuppressWarnings(\"unused\") " + lclOCN + " argOpal) {");
				lclBW.println("\t\treturn;");
				lclBW.println("\t}");
				lclBW.println();
				
				if (hasDatabaseGeneratedColumns()) {
					Iterator<ClassMember> lclCMI = createClassMemberIterator();
					while (lclCMI.hasNext()) {
						ClassMember lclCM = lclCMI.next();
						if (lclCM.getDatabaseColumn().hasDatabaseGeneratedNumber()) {
							lclBW.println("\tprivate int myNext" + lclCM.getBaseMemberName() + " = 1000;"); // FIXME: Not always int, not always 1000
							lclBW.println();
							lclBW.println("\tprotected synchronized int next" + lclCM.getBaseMemberName() + "() {");
							lclBW.println("\t\treturn myNext" + lclCM.getBaseMemberName() + "++;");
							lclBW.println("\t}");
							lclBW.println();
						}
					}
				}
				lclBW.println("\t@Override");
				lclBW.println("\tprotected void insertInternal(TransactionParameter argTP, " + lclOCN + " argOpal) {"); // THINK: Also updateInternal?
				lclBW.println("\t\targOpal.translateReferencesToFields();"); // THINK: Always?
				if (hasDatabaseGeneratedColumns()) {
					Iterator<ClassMember> lclCMI = createClassMemberIterator();
					while (lclCMI.hasNext()) {
						ClassMember lclCM = lclCMI.next();
						if (lclCM.getDatabaseColumn().hasDatabaseGeneratedNumber()) {
							lclBW.println("\t\targOpal." + lclCM.getPrimitiveMutatorName() + "(next" + lclCM.getBaseMemberName() + "());");
						}
					}
				}
				lclBW.println("\t\treturn;");
				lclBW.println("\t}");
				lclBW.println();
				lclBW.println("\t@Override");
				lclBW.println("\tprotected void updateInternal(TransactionParameter argTP, " + lclOCN + " argOpal) {"); // THINK: Also updateInternal?
				lclBW.println("\t\targOpal.translateReferencesToFields();"); // THINK: Always?
				lclBW.println("\t\treturn;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (isEphemeral() == false) {
				/* As in the case of the afterInsert method, this is expected to be delegated to a concrete
				 * subclass that will know how to interpret the concrete Table class to generate a FQTN. */
				/* registerOpal */
				lclBW.println("\t@Override");
				lclBW.println("\tprotected void registerNewOpal(" + lclOCN + " argOpal) {");
				if (isCreatable() || isUpdatable()) {
					lclBW.println("\t\tregisterOpal(argOpal, argOpal.getNewValues());");
				} else {
					lclBW.println("\t\tregisterOpal(argOpal, argOpal.getValues());");
				}
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\t@Override");
				lclBW.println("\tprotected void registerOldOpal(" + lclOCN + " argOpal) {");
				if (isCreatable() || isUpdatable()) {
					lclBW.println("\t\tregisterOpal(argOpal, argOpal.getOldValues());");
				} else {
					lclBW.println("\t\tregisterOpal(argOpal, argOpal.getValues());");
				}
				lclBW.println("\t}");
				lclBW.println();
				
				/* registerOpal */
				lclBW.println("\tprotected void registerOpal(" + lclOCN + " argOpal, Object[] argValues) {");
				lclBW.println("\t\tif (argValues == null) { throw new IllegalStateException(); }");
				lclBW.println("\t\tif (argValues.length != " + getClassMemberCount() + ") { throw new IllegalStateException(); }");
				lclBW.println("\t\tOpalCache<" + lclOCN + "> lclOC = getCache();");
				lclBW.println("\t\tsynchronized (lclOC) {");
				for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
					if (lclMUK.couldHaveNullComponent()) {
						lclBW.println("\t\t\tif (" + lclMUK.generateNotNullJavaCondition("argValues") + ") {");
						lclBW.print("\t\t\t\tlclOC.addOpal(new " + lclMUK.getOpalKeyClassName() + "(");
						lclBW.print(lclMUK.generateOpalKeyConstructorArguments("argValues"));
						lclBW.println("), argOpal, true);"); /* true = use SoftReference (not WeakReference) */
						lclBW.println("\t\t\t}");
					} else {
						lclBW.print("\t\t\tlclOC.addOpal(new " + lclMUK.getOpalKeyClassName() + "(");
						lclBW.print(lclMUK.generateOpalKeyConstructorArguments("argValues"));
						lclBW.println("), argOpal, true);"); /* true = use SoftReference (not WeakReference) */
					}
				}
				lclBW.println("\t\t}");
				lclBW.println("\t}");
				lclBW.println();
		
				/* unregisterOpal */				
				lclBW.println("\t@Override");
				lclBW.println("\tprotected void unregisterOpal(" + lclOCN + " argOpal) {");
				if (isCreatable() || isUpdatable()) {
					lclBW.println("\t\tObject[] lclOldValues = argOpal.getOldValues();");
				} else {
					lclBW.println("\t\tObject[] lclOldValues = argOpal.getValues();");
				}
				lclBW.println("\t\tif (lclOldValues == null) { throw new IllegalStateException(); }");
				lclBW.println("\t\tif (lclOldValues.length != " + getClassMemberCount() + ") { throw new IllegalStateException(); }");
				
				lclBW.println("\t\tOpalCache<" + lclOCN + "> lclOC = getCache();");
				lclBW.println("\t\tsynchronized (lclOC) {");
				for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
					if (lclMUK.couldHaveNullComponent()) {
						lclBW.println("\t\t\tif (" + lclMUK.generateNotNullJavaCondition("lclOldValues") + ") {");
						lclBW.print("\t\t\t\tlclOC.removeOpal(new " + lclMUK.getOpalKeyClassName() + "(");
						lclBW.print(lclMUK.generateOpalKeyConstructorArguments("lclOldValues"));
						lclBW.println("));");
						lclBW.println("\t\t\t}");
					} else {
						lclBW.print("\t\t\tlclOC.removeOpal(new " + lclMUK.getOpalKeyClassName() + "(");
						lclBW.print(lclMUK.generateOpalKeyConstructorArguments("lclOldValues"));
						lclBW.println("));");
					}
				}
				lclBW.println("\t\t}");
				lclBW.println("\t}");
				lclBW.println();
				
				/* updateKeys */ // TODO:  This seems like it shouldn't go here
				/* FIXME:  As written, this is kind of silly for immutable opals */
				lclBW.println("\t@Override");
				lclBW.println("\tpublic void updateKeys(" + lclOCN + " argOpal) {");
				if (isCreatable() || isUpdatable()) {
					lclBW.println("\t\torg.apache.commons.lang3.Validate.notNull(argOpal);");
					if (isCreatable() || isUpdatable()) {
						lclBW.println("\t\tObject[] lclOldValues = argOpal.getOldValues();");
					} else {
						lclBW.println("\t\tObject[] lclOldValues = argOpal.getValues();");
					}
					lclBW.println("\t\tif (lclOldValues == null) { throw new IllegalStateException(); }");
					lclBW.println("\t\tif (lclOldValues.length != " + getClassMemberCount() + ") { throw new IllegalStateException(); }");
					if (isCreatable() || isUpdatable()) {
						lclBW.println("\t\tObject[] lclNewValues = argOpal.getNewValues();");
					} else {
						lclBW.println("\t\tObject[] lclNewValues = argOpal.getValues();");
					}
					lclBW.println("\t\tif (lclNewValues == null) { throw new IllegalStateException(); }");
					lclBW.println("\t\tif (lclNewValues.length != " + getClassMemberCount() + ") { throw new IllegalStateException(); }");
					lclBW.println("\t\tOpalCache<" + lclOCN + "> lclOC = getCache();");
					lclBW.println("\t\tsynchronized (lclOC) {");
					lclBW.println("\t\t\tOpalKey<" + lclOCN + "> lclOldKey = null;");
					lclBW.println("\t\t\tOpalKey<" + lclOCN + "> lclNewKey = null;");
					
					for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
						lclBW.print("\t\t\tif (");
						lclBW.print(lclMUK.generateNotNullJavaCondition("lclNewValues"));
						lclBW.println(") {");
						lclBW.print("\t\t\t\tif (!(");
						lclBW.print(lclMUK.generateKeyEqualityCondition("lclNewValues", "lclOldValues"));
						lclBW.println(")) {");
						lclBW.println("\t\t\t\t\tlclNewKey = " + lclMUK.generateOpalKeyConstructorCall("lclNewValues") + ';');
						lclBW.println("\t\t\t\t\tif (" + lclMUK.generateNotNullJavaCondition("lclOldValues") + ") {");
						lclBW.println("\t\t\t\t\t\tlclOldKey = " + lclMUK.generateOpalKeyConstructorCall("lclOldValues") + ';');
						lclBW.println("\t\t\t\t\t}");
						lclBW.println("\t\t\t\t}");
						if (lclMUK.couldHaveNullComponent()) {
							lclBW.println("\t\t\t} else {");
							lclBW.println("\t\t\t\tif (" + lclMUK.generateNotNullJavaCondition("lclOldValues") + ") {");
							lclBW.println("\t\t\t\t\tlclOldKey = " + lclMUK.generateOpalKeyConstructorCall("lclOldValues") + ';');
							lclBW.println("\t\t\t\t}");
						}
						lclBW.println("\t\t\t}");
						lclBW.println("\t\t\tif (lclOldKey != null) { lclOC.removeOpal(lclOldKey); lclOldKey = null; }");
						lclBW.println("\t\t\tif (lclNewKey != null) { lclOC.addOpal(lclNewKey, argOpal, true); lclNewKey = null; } /* true = SoftReference */");
					}
					lclBW.println("\t\t}");
					lclBW.println("\t\treturn;");
				} else {
					lclBW.println("\t\t/* No keys to update for ImmutableOpals */");
				}
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (isEphemeral() == false) {
				/* getPrimaryKeyWhereClauseColumns() */
				
				lclBW.println("\t@Override");
				lclBW.println("\tprotected OpalKey<" + lclOCN +"> createOpalKeyForReloading(" + lclOCN + " argOpal) {");
				if (isCreatable() || isUpdatable()) {
					lclBW.println("\t\tObject[] lclValues = argOpal.getNewValues();");
				} else {
					lclBW.println("\t\tObject[] lclValues = argOpal.getValues();");
				}
				
				// MappedUniqueKey lclPK = getPrimaryKey();
				lclBW.println("\t\treturn " + lclPK.generateOpalKeyConstructorCall("lclValues") + ';');
				lclBW.println("\t}");
				lclBW.println();
			}
			
			/* Create a factory method for each foreign key */
			Iterator<MappedForeignKey> lclFKI = createForeignKeyIterator();
			while (lclFKI.hasNext()) {
				MappedForeignKey lclFK = lclFKI.next();
				
				if (lclFK.hasBackCollection() == false) {
					continue;
				}
				
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + lclFK.getCollectionType().getName() + "<" + lclOCN + "> " + lclFK.getSource().generateOpalFactoryFunctionDefinition() + " /* throws PersistenceException */ {");
				lclBW.println("\t\treturn new " + lclFK.getCollectionType().getName() + "<>();");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			/* Create a factory method for each unique key */
			for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
				lclBW.println("\t@Override");
				lclBW.print("\tpublic " + lclOCN + ' ' + lclMUK.generateOpalFactoryMethodName(false) + "(");
				
				boolean lclFirst = true;
				Iterator<ClassMember> lclJ = lclMUK.createClassMemberIterator();
				while (lclJ.hasNext()) {
					ClassMember lclCM = lclJ.next();
					if (lclFirst) {
						lclFirst = false;
					} else {
						lclBW.print(", ");
					}
					lclBW.print(lclCM.getMemberParameterizedTypeName() + ' ' + lclCM.getObjectMutatorArgumentName());
				}
				lclBW.println(") throws PersistenceException {");
				
				/* The body of the method will be different depending on whether it is an Identity Opal or an Ephemeral Opal. */
				if (isEphemeral() == false) {
					/* This is for a Persistent Opal. */
					lclBW.print("\t\tOpalKey<" + lclOCN +"> lclOpalKey = new " + lclMUK.getOpalKeyClassName(/* lclOCN */) + "(");
					lclFirst = true;
					lclJ = lclMUK.createClassMemberIterator();
					while (lclJ.hasNext()) {
						ClassMember lclCM = lclJ.next();
						if (lclFirst) {
							lclFirst = false;
						} else {
							lclBW.print(", ");
						}
						lclBW.print(lclCM.getObjectMutatorArgumentName());
					}
					lclBW.println(");");
					lclBW.println("\t\treturn forOpalKey(lclOpalKey);");
					lclBW.println("\t}");
					lclBW.println();
					
					if (needsConcreteFactoryMethod()) {
						if (lclMUK.isPrimaryKey()) {
							lclBW.println("\t@Override");
							lclBW.print("\tpublic " + lclOCN + ' ' + lclMUK.generateOpalFactoryMethodName(true) + "(");
							
							lclFirst = true;
							lclJ = lclMUK.createClassMemberIterator();
							while (lclJ.hasNext()) {
								ClassMember lclCM = lclJ.next();
								if (lclFirst) {
									lclFirst = false;
								} else {
									lclBW.print(", ");
								}
								lclBW.print(lclCM.getMemberType().getName() + ' ' + lclCM.getObjectMutatorArgumentName());
							}
							lclBW.println(") throws PersistenceException {");
							lclBW.print("\t\tOpalKey<" + lclOCN +"> lclOpalKey = new " + lclMUK.getOpalKeyClassName(/* lclOCN */) + "(");
							lclFirst = true;
							lclJ = lclMUK.createClassMemberIterator();
							while (lclJ.hasNext()) {
								ClassMember lclCM = lclJ.next();
								if (lclFirst) {
									lclFirst = false;
								} else {
									lclBW.print(", ");
								}
								lclBW.print(lclCM.getObjectMutatorArgumentName());
							}
							lclBW.println(");");
							lclBW.println("\t\treturn forOpalKey(lclOpalKey, true);");
							lclBW.println("\t}");
							lclBW.println();
						}
					}
				} else {
					/* This is for an Ephemeral Opal */
					lclBW.print("\t\treturn getOpalForQuery(new ImplicitTableDatabaseQuery(");
					StringBuilder lclSBColumns = new StringBuilder("\"");
					StringBuilder lclSBValues = new StringBuilder("new Object[] { ");
					lclJ = lclMUK.createClassMemberIterator();
					lclFirst = true;
					while (lclJ.hasNext()) {
						ClassMember lclCM = lclJ.next();
						if (lclFirst) {
							lclFirst = false;
						} else {
							lclSBColumns.append(" AND ");
							lclSBValues.append(", ");
						}
						lclSBColumns.append(lclCM.getDatabaseColumn().getName());
						lclSBColumns.append(" = ?");
						
						lclSBValues.append(lclCM.getObjectMutatorArgumentName());
					}
					lclSBColumns.append('"');
					lclSBValues.append("}");
					lclBW.print(lclSBColumns);
					lclBW.print(", ");
					lclBW.print(lclSBValues);
					lclBW.println(" ));");
					lclBW.println("\t}");
				}
			}
			
			/* Static nested classes for representing keys for this object */
			Iterator<ClassMember> lclJ;
			
			if (isEphemeral() == false) {
				for (MappedUniqueKey lclMUK : getMappedUniqueKeys()) {
					boolean lclSingleValue = lclMUK.getClassMembers().size() == 1;
					Class<?> lclDOKClass = lclSingleValue  ? SingleValueDatabaseOpalKey.class : MultipleValueDatabaseOpalKey.class;
					lclBW.println("\t/* package */ static class " + lclMUK.getOpalKeyClassName() + " extends " + lclDOKClass.getName() + "<" + lclOCN + "> {");
					
					lclBW.print("\t\tprivate static final String[] ourKeyColumnNames = new String[] {");
					lclJ = lclMUK.createClassMemberIterator();
					while (lclJ.hasNext()) {
						ClassMember lclCM = lclJ.next();
						lclBW.print("\"" + lclCM.getDatabaseColumn().getName() + "\", ");
					}
					lclBW.println("};");
					lclBW.println();
					
					lclBW.print("\t\tpublic " + lclMUK.getOpalKeyClassName() + "(");
					
					boolean lclFirst = true;
					
					lclJ = lclMUK.createClassMemberIterator();
					while (lclJ.hasNext()) {
						ClassMember lclCM = lclJ.next();
						if (lclFirst) {
							lclFirst = false;
						} else {
							lclBW.print(", ");
						}
						lclBW.print(lclCM.getMemberParameterizedTypeName(false) + ' ' + lclCM.getPrimitiveMutatorArgumentName());
					}
					lclBW.println(") {");
					
					if (lclSingleValue) {
						lclBW.print("\t\t\tsuper(");
						ClassMember lclCM = lclMUK.getClassMembers().get(0);
						assert lclCM != null;
						lclBW.print(OpalUtility.getCodeToConvertToObject(lclCM.getMemberType(), lclCM.getPrimitiveMutatorArgumentName()));
						lclBW.println(");");
					} else {
						lclBW.print("\t\t\tsuper(new Object[] {");
						
						lclJ = lclMUK.createClassMemberIterator();
						while (lclJ.hasNext()) {
							ClassMember lclCM = lclJ.next();
							lclBW.print(OpalUtility.getCodeToConvertToObject(lclCM.getMemberType(), lclCM.getPrimitiveMutatorArgumentName()));
							lclBW.print(", ");
						}
						lclBW.println("});");
					}
					lclBW.println("\t\t}");
					lclBW.println();
					lclBW.println("\t\t@Override");
					lclBW.println("\t\tpublic Object[] getParameters() {");
					if (lclSingleValue) {
						lclBW.println("\t\t\treturn new Object[] { getKeyValue(), };");
					} else {
						lclBW.println("\t\t\treturn getFields();");
					}
					lclBW.println("\t\t}");
					lclBW.println();
					lclBW.println("\t\t@Override");
					lclBW.println("\t\tpublic String[] getColumnNames() { return ourKeyColumnNames; }");
					lclBW.println();
					
					/* End of the inner class */
					lclBW.println("\t}");
					lclBW.println();
				}
			}
			
			if (isEphemeral() == false) {
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + lclOCN + " forUniqueString(String argUniqueString) {");
				lclBW.println("\t\tif (argUniqueString == null || \"\".equals(argUniqueString)) {");
				lclBW.println("\t\t\treturn null;");
				lclBW.println("\t\t}");
				lclBW.println("\t\tString[] lclArgs = argUniqueString.split(\"\\\\|\");");
				lclBW.println("\t\tassert lclArgs.length == " + lclPK.sizeClassMember() + ';');
				lclBW.println("\t\treturn forOpalKey(");
				lclBW.println("\t\t\tnew " + lclPK.getOpalKeyClassName() + "(");
				lclI = lclPK.createClassMemberIterator();
				int lclIndex = 0;
				while (lclI.hasNext()) {
					ClassMember lclCM = lclI.next();
					lclBW.print("\t\t\t\t" + OpalUtility.getCodeToConvert(lclCM.getMemberType(), String.class, "lclArgs[" + lclIndex + "]", false));
					if (lclIndex < lclPK.sizeClassMember() - 1) {
						lclBW.print(',');
					}
					++lclIndex;
					lclBW.println();
				}
				lclBW.println("\t\t\t)");
				lclBW.println("\t\t);");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			/* End of the class */
			lclBW.println("}");
			
			lclBW.close();
		}
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
