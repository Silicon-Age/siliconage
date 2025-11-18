package com.opal.creator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Arrays;
import java.util.ArrayList;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.siliconage.database.DatabaseUtility;
import com.siliconage.util.Trinary;
import com.siliconage.xml.XMLElement;
import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.TableName;
import com.opal.OpalUtility;

import static com.opal.creator.XMLCreator.complain;

public class Mapping extends OpalXMLElement {
	
	protected MappedClass myMappedClass;
	
	private static final String DEFAULT_DELEGATE_CLASS_NAME = "DelegateUtil";
	
	private static final String DEFAULT_DELEGATE_SUFFIX = "Delegate";
	
	public Mapping(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
	}
	
	protected MappedClass getMappedClass() {
		return myMappedClass;
	}
	
	@Override
	protected void preChildren(OpalParseContext argContext) throws Exception {
		String lclTableString = getAttributeValue("Table");
		String lclViewString = getAttributeValue("View");
		String lclEntityString = getAttributeValue("Entity");

		/* How many of Table, View, and Entity were specified for the <Mapping> element? */
		int lclCount = (lclTableString != null ? 1 : 0) + (lclViewString != null ? 1 : 0) + (lclEntityString != null ? 1 : 0);
		
		String lclEntityName;
		Trinary lclDeclaredAsView;
		
		if (lclCount == 0) {
			/* If zero were specified, that's an error. */
			complain(MessageLevel.Error, "<Mapping> element specified neither a Table, View, nor Entity attribute.  Skipping.");
			return;
		} else if (lclCount > 1) {
			/* If more than one were specified, that's an error. */
			complain(MessageLevel.Error, "<Mapping> element specified more than one of Table" + (lclTableString != null ? " (" + lclTableString + ")" : "") +
					", View" + (lclViewString != null ? " (" + lclViewString + ")" : "") +
					" or Entity" + (lclEntityString != null ? " (" + lclEntityString + ")" : "") + ".  Skipping.");
			return;
		} else {
			/* Exactly one was specified. */
			if (lclTableString != null) {
				lclEntityName = lclTableString;
				lclDeclaredAsView = Trinary.FALSE;
			} else if (lclViewString != null) {
				lclEntityName = lclViewString;
				lclDeclaredAsView = Trinary.TRUE;
			} else {
				lclEntityName = lclEntityString;
				lclDeclaredAsView = Trinary.UNKNOWN;
			}
		}
		
		assert lclEntityName != null;
		
		String lclTypeName = getAttributeValue("Type", OpalUtility.convertUnderscoreIdentifierToJavaIdentifier(lclEntityName));
//		String lclSchemaName = getAttributeValue("Schema", argContext.getDefaultSchema());
		String lclExplicitPackage = getAttributeValue("Package"); // FIXME: Allow Subpackage as well.
		String lclPackageRoot;
		if (lclExplicitPackage != null) {
			lclPackageRoot = lclExplicitPackage; // FIXME: Validate in some way?
		} else {
			lclPackageRoot = argContext.getDefaultPackage();
		}
		String lclSubpackage = argContext.getCurrentSubpackage();
		if (lclSubpackage != null) {
			lclSubpackage = '.' + lclSubpackage;
		} else {
			lclSubpackage = "";
		}
		
		complain(MessageLevel.Debug, lclTypeName + " is being put into package \"" + lclPackageRoot + "\" and subpackage \"" + lclSubpackage + "\".");
		
		Trinary lclDefaultBackCollections = Trinary.fromStringCaseInsensitive(getAttributeValue("BackCollection", "Unknown"));
		
		Trinary lclMone = Trinary.fromStringCaseInsensitive(getAttributeValue("Mone", "Unknown"));
		
		/* Is this table actually a view?  If so, we don't store that fact anywhere, but we default
		 * Updatable and Creatable to false. */ /* We do actually store it! */ /* I don't see where we actually store this. */
		
		Trinary lclTreatAsView = Trinary.fromStringCaseInsensitive(getAttributeValue("TreatAsView", "Unknown")); // CHECK Catch Exception?
		
		if (lclTreatAsView == Trinary.UNKNOWN) {
			lclTreatAsView = lclDeclaredAsView;
		}
		
		Trinary lclCreatable = Trinary.fromStringCaseInsensitive(getAttributeValue("Creatable", "Unknown"));
		Trinary lclUpdatable = Trinary.fromStringCaseInsensitive(getAttributeValue("Updatable", "Unknown"));

		Trinary lclCodeConstants = Trinary.fromStringCaseInsensitive(getAttributeValue("StaticBindings", "Unknown"));
		
		/* Remember that this really means "is this table potentially polymorphic" rather than "is definitely polymorphic." */
		/* FIXME: So this should also be a Trinary. */
		boolean lclPolymorphic = "True".equalsIgnoreCase(getAttributeValue("Polymorphic", "True"));
		
		Mappings lclMappingsXE = null;
		XMLElement lclXE = getParent();
		while (lclXE != null) {
			if (lclXE instanceof Mappings) {
				lclMappingsXE = (Mappings) lclXE;
				break;
			}
			lclXE = lclXE.getParent();
		}
		// THINK: Should we warn about lclMappingsXE being null?  We'll get one such warning for every Mapping element.
		
		boolean lclGenerateHttpRequestFactory;
		String lclHttpRequestFactoryPreference = getAttributeValue("HttpRequestFactory");
		if (lclHttpRequestFactoryPreference == null) { // If you want an HttpRequestFactory for a view, you must explicitly request it!
			if (lclMappingsXE != null) {
				lclGenerateHttpRequestFactory = lclMappingsXE.generateHttpRequestFactories();
			} else {
				lclGenerateHttpRequestFactory = false;
			}
		} else {
			lclGenerateHttpRequestFactory = "True".equals(lclHttpRequestFactoryPreference);
		}
		
		/* We will generate fluent mutators if it is specified in the Mappings 
		 * element and not falsified in the individual Mapping element.  But
		 * for polymorphic tables, you have to specify it in the individual 
		 * Mapping element because there's some screwiness with covariant 
		 * return types and it might not work at all.
		 */
		boolean lclDefaultFluentMutators = (lclMappingsXE != null) ? lclMappingsXE.generateFluentMutators() : false;
		String lclMutatorsPreference = getAttributeValue("FluentMutators");
		boolean lclForceFluentMutators = "True".equals(lclMutatorsPreference);

//		boolean lclSampleCollections;
//		String lclSampleCollectionsPreference = getAttributeValue("SampleCollections");
//		if ("True".equalsIgnoreCase(lclSampleCollectionsPreference)) {
//			lclSampleCollections = true;
//		} else if ("False".equalsIgnoreCase(lclSampleCollectionsPreference)) {
//			lclSampleCollections = false;
//		} else {
//			lclSampleCollections = ((Mappings) getParent()).sampleCollections();
//		}
		// complain(MessageLevel.Warning, "SampleCollections is " + lclSampleCollections + " for table " + lclEntityName);
		
		String lclCollections = (lclMappingsXE != null) ? lclMappingsXE.getCollections() : "Unknown";
		
		boolean lclGetAll = "True".equals(getAttributeValue("GetAll", "False"));
		
		boolean lclAbstract = "True".equals(getAttributeValue("Abstract", "False"));
		
		/* FIXME: This should also be a Trinary */
		boolean lclTree = "True".equals(getAttributeValue("Tree", "True"));
		
		boolean lclDeprecated = "True".equals(getAttributeValue("Deprecated", "False"));
		
		Trinary lclEphemeral = Trinary.fromStringCaseInsensitive(getAttributeValue("Ephemeral", "Unknown"));
		
		String lclMessageLevelString = getAttributeValue("MessageLevel", "Warning");
		MessageLevel lclMessageLevel;
		try {
			 lclMessageLevel = MessageLevel.valueOf(lclMessageLevelString);
		} catch (IllegalArgumentException lclE) {
			System.out.println("The MessageLevel attribute for the Mapping of " + lclEntityName + " is \"" + lclMessageLevelString + "\", which is unrecognized.");
			lclMessageLevel = MessageLevel.Warning;
		}
		
		String lclCacheString = getAttributeValue("Cache");
		boolean lclSoftReferences;
		if (lclCacheString == null) {
			lclSoftReferences = true;
		} else if ("Soft".equalsIgnoreCase(lclCacheString)) {
			complain(MessageLevel.Debug, "Specifying SoftReferences for the cache.");
			lclSoftReferences = true;
		} else if ("Weak".equalsIgnoreCase(lclCacheString)) {
			complain(MessageLevel.Debug, "Specifying WeakReferences for the cache.");
			lclSoftReferences = false;
		} else {
			complain(MessageLevel.Error, "Unknown Cache value \"" + lclCacheString + "\".  Defaulting to SoftReferences.");
			lclSoftReferences = true;
		}
		
		String lclPoolName = getAttributeValue("Pool");
		if (lclPoolName == null) {
			lclPoolName = OpalParseContext.DEFAULT_POOL_NAME;
		}
		complain(MessageLevel.Debug, "Specifying pool name \"" + lclPoolName + "\".");
		if (argContext.getPoolMap().get(lclPoolName) == null) {
			complain(MessageLevel.Error, "Unknown pool name \"" + lclPoolName + "\".");
		}
		
		TableName lclTableName = argContext.getRelationalDatabaseAdapter().createTableName((Element) getNode());
		
		MappedClass lclMC = new MappedClass(
			lclTableName,
			lclTypeName,
			lclPackageRoot + '.' + argContext.getApplicationSubpackage() + lclSubpackage,
			lclPackageRoot + '.' + argContext.getPersistenceSubpackage() + lclSubpackage,
			lclPackageRoot + '.' + argContext.getCMASubpackage() + lclSubpackage,
			argContext.getSourceDirectory(),
			argContext.getAuthor(),
			lclTreatAsView,
			lclCreatable,
			lclUpdatable,
			lclAbstract,
			lclEphemeral,
			lclTree,
			lclGenerateHttpRequestFactory,
			lclDefaultBackCollections,
			lclCollections,
			lclDefaultFluentMutators,
			lclForceFluentMutators,
//			lclSampleCollections,
			lclDeprecated,
			lclMessageLevel
		);
		
		lclMC.setMone(lclMone);
		lclMC.setPolymorphic(lclPolymorphic);
		lclMC.setStaticBindings(lclCodeConstants);
		lclMC.setSoftReferences(lclSoftReferences);
		lclMC.setPoolName(lclPoolName);
		
		setMappedClass(lclMC);
		
		argContext.getMappedClasses().put(lclTableName, lclMC);
		
		/* TODO:  Figure out how to use multiple schemata */
		
		// lclMC.setSchemaName(lclSchemaName);
		
		lclMC.setGetAll(lclGetAll);
		
		Iterator<DatabaseColumn> lclI = argContext.getRelationalDatabaseAdapter().getColumns(lclTableName).iterator();
		ClassMember lclCM;
		while (lclI.hasNext()) {
			DatabaseColumn lclDC = lclI.next();
			Class<?> lclType = argContext.getRelationalDatabaseAdapter().determineJavaType(lclDC);
			boolean lclLargeType = argContext.getRelationalDatabaseAdapter().isLargeDatabaseType(lclDC);
			
			/* Does the name of the column suggest that we should use an alternate type? */
			if (lclType != null) {
				Class<?> lclSpecialType = determineSpecialJavaType(lclDC, lclType);
				if (lclSpecialType != null) {
					lclType = lclSpecialType;
				}
			}
			
			if (lclType != null) {
				if (lclLargeType) {
					lclMC.addLargeClassMember(
						lclCM = new ClassMember(lclDC, lclType)
					);
				} else {
					lclMC.addClassMember(
						lclCM = new ClassMember(lclDC, lclType)
					);
				}
				
				// TODO: Make sure type is Comparable
				
				if (argContext.getComparatorColumnNameList().contains(lclDC.getName())) {
					lclCM.setComparator(true);
				}
				
				if (argContext.getUnmappedColumnNameList().contains(lclDC.getName())) {
					lclCM.setMapped(false);
				}
			} else {
				complain(MessageLevel.Error, lclMC, "Unable to map database column " + lclDC);
			}
		}
		
		generateAutomaticDelegates(argContext);
	}
	
	protected String determinePackage(OpalParseContext argContext) {
		StringBuilder lclSB = new StringBuilder(argContext.getDefaultPackage());
		String lclCurrentSubpackage = argContext.getCurrentSubpackage();
		if (lclCurrentSubpackage != null) {
			lclSB.append('.'); // FIXME: Unless it already ends in a period
			lclSB.append(lclCurrentSubpackage);
		}
		return lclSB.toString();
	}
	
	@Override
	protected void postChildren(OpalParseContext argContext) throws Exception {
		if (getMappedClass().getStaticBindings(true)) {
			generateCodes(argContext);
		} else {
			System.out.println("Not generating static bindings for " + getMappedClass());
		}
		// generateDependencies(argContext);
	}
	
	protected void generateCodes(OpalParseContext argContext) throws Exception {
		/* We want to give the user the ability to write code to reference specific Opals
		 * by unique, alphanumeric names.  If the database has a unique index on a single
		 * (string) column called "code," then we will make static variables for each
		 * element of that table that instance objects (and load their data) at start up.
		 * This allows for compile-time checking of some of the links between source code
		 * and the database.  The references will be created in the factory.
		 */
		
		/* To find candidates for building these, we loop through the class' unique keys ... */
		
		Iterator<ClassMember> lclI = getMappedClass().createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			
			/* And whose single column is named "code" and has type String. */
			if (lclCM.isMapped() && lclCM.getDatabaseColumn().getName().equalsIgnoreCase("Code") && lclCM.getMemberType() == String.class) {
				/* We've found a suitable index to support compile-time list of objects. */
				
				ArrayList<String> lclCodes = new ArrayList<>();
				
				/* Load the alphanumeric identifiers ("codes") from the database. */
				
				Connection lclConn = null;
				ResultSet lclRS = null;
				try {
					lclConn = argContext.getRelationalDatabaseAdapter().getDataSource().getConnection();
					lclRS = DatabaseUtility.select(lclConn, "SELECT " + lclCM.getDatabaseColumn().getName() + " FROM " + getMappedClass().getTableName() + " ORDER BY " + lclCM.getDatabaseColumn().getName());
					
					/* Make sure that each one is a valid Java identifier */
					
					while (lclRS.next()) {
						String lclCode = lclRS.getString("code");
						lclCodes.add(lclCode);
					}
					
					/* Store the list in the MappedClass object so that the it can be referenced
					 * when the source code for the interface is generated. */
					
					if (lclCodes.isEmpty() == false) {
						getMappedClass().setStaticBindings(lclCodes);
					}
				} finally {
					DatabaseUtility.cleanUp(lclRS, lclConn);
				}
			}
		}
	}
	
	protected void generateAutomaticDelegates(OpalParseContext argContext) {
		Validate.notNull(argContext);
		
		generateAutomaticDelegatesFromClass(argContext, DEFAULT_DELEGATE_CLASS_NAME);
		
		MappedClass lclMC = getMappedClass();
		String lclDelegateClassName = lclMC.getInterfaceClassName() + DEFAULT_DELEGATE_SUFFIX; 
		
		generateAutomaticDelegatesFromClass(argContext, lclDelegateClassName); 
		
		generateValidateDelegatesFromClass(argContext, lclDelegateClassName);
		
		return;
	}
	
	protected void generateAutomaticDelegatesFromClass(OpalParseContext argContext, String argClassName) {
		Validate.notNull(argContext);
		
		if (argClassName == null) {
			return;
		}
		
		MappedClass lclMC = getMappedClass();
		
		String lclDelegateClassName = lclMC.getInterfacePackageName() + '.' + argClassName;
		
		complain(MessageLevel.Debug, lclMC, "Looking for delegated methods for " + lclMC.getTypeName() + " in " + lclDelegateClassName + " . . .");
		try {
			Class<?> lclClass = Class.forName(lclDelegateClassName);
			
			complain(MessageLevel.Info, lclMC, "Found delegate class for " + lclMC.getTypeName() + "; checking its methods");
			Method[] lclMethods = lclClass.getDeclaredMethods();
			
			Arrays.sort(lclMethods, MethodNameComparator.getInstance());
			
			MethodLoop:
			for (Method lclM : lclMethods) {
				if (lclM.isSynthetic()) {
					continue;
				}
				if (Modifier.isAbstract(lclM.getModifiers())) {
					continue;
				}
				if (Modifier.isStatic(lclM.getModifiers()) == false) {
					continue;
				}
				if (Modifier.isPrivate(lclM.getModifiers())) {
					continue;
				}
				Type[] lclParameters = lclM.getGenericParameterTypes();
				if (lclParameters.length == 0) {
					continue;
				}
				String lclFirstParameterTypeName = OpalUtility.generateJavaDeclaration(lclParameters[0]);
				String lclParameterTypeName = lclParameters[0].getTypeName();
				String lclFullTypeName = lclMC.getInterfacePackageName() + '.' + lclMC.getTypeName();
				if (lclFirstParameterTypeName.equals(lclFullTypeName) == false) {
					complain(MessageLevel.Debug, lclMC, lclParameterTypeName + " did not match " + lclFullTypeName);
					continue;
				}
				
				Type[] lclRemainingParameters = Arrays.copyOfRange(lclParameters, 1, lclParameters.length);
				
				Type[] lclExceptions = lclM.getGenericExceptionTypes();
				
				complain(MessageLevel.Debug, lclMC, "Found method " + lclM.getName() + " with appropriate first parameter");
				
				for (MethodDelegation lclMD : lclMC.getMethodDelegations()) {
					if (lclMD.getLocalMethodName().equals(lclM.getName())) {
						if (Arrays.equals(lclMD.getParameters(), lclRemainingParameters)) {
							continue MethodLoop;
						}
					}
				}
				
				complain(MessageLevel.Debug, lclMC, "Found automatic delegated method " + lclM.getName() + " for " + lclMC.getTypeName() + ".");
				lclMC.add(
					new MethodDelegation(
						lclDelegateClassName,
						lclM.getName(),
						lclM.getName(),
						lclM.getGenericReturnType(),
						lclRemainingParameters,
						lclExceptions
					)
				);
			}
		} catch (ClassNotFoundException lclE) {
			/* No class of automatic delegation methods found; not a problem. */
			complain(MessageLevel.Debug, lclMC, "No class found with name \"" + argClassName + "\" from which to import delegated methods.");
		}
		
		return;
	}
	
	protected void generateValidateDelegatesFromClass(OpalParseContext argContext, String argClassName) {
		Validate.notNull(argContext);
		
		if (argClassName == null) {
			return;
		}
		
		MappedClass lclMC = getMappedClass();
		
		String lclDelegateClassName = lclMC.getInterfacePackageName() + '.' + argClassName; 
		try {
			Class<?> lclClass = Class.forName(lclDelegateClassName);
			
			Method[] lclMethods = lclClass.getDeclaredMethods();
			
//			MethodLoop:
			for (Method lclM : lclMethods) {
				if (lclM.getName().startsWith("validate") == false) {
					continue;
				}
				if (lclM.isSynthetic()) {
					continue;
				}
				if (Modifier.isAbstract(lclM.getModifiers())) {
					continue;
				}
				if (Modifier.isStatic(lclM.getModifiers()) == false) {
					continue;
				}
				if (Modifier.isPrivate(lclM.getModifiers())) {
					continue;
				}
				Type[] lclParameters = lclM.getGenericParameterTypes();
				if (lclParameters.length != 1) {
					continue;
				}
				
				String lclPotentialMemberName = lclM.getName().substring("validate".length());
				
				ClassMember lclCM = lclMC.getClassMemberByBaseName(lclPotentialMemberName);
				
				if (lclCM != null) {
					if (lclParameters[0] == lclCM.getMemberType()) {
						lclCM.setValidationMethodName(lclM.getName());
						lclCM.setValidationMethodClassName(lclDelegateClassName);
					} else {
						complain(MessageLevel.Warning, lclMC, "Found method " + lclM.getName() + " in " + lclDelegateClassName + " that looks like it is intended to be a field validation method, but its argument type is " + lclParameters[0] + " whereas the field " + lclPotentialMemberName + " would require an argument of type " + lclCM.getMemberType().getName() + ".");
					}
				} else {
					complain(MessageLevel.Warning, lclMC, "Found method " + lclM.getName() + " in " + lclDelegateClassName + " that looks like it is intended to be a field validation method, but the field \"" + lclPotentialMemberName + "\" doesn't match an actual field.");
				}
			}
		} catch (ClassNotFoundException lclE) {
			complain(MessageLevel.Debug, lclMC, "No class found with name \"" + argClassName + "\" from which to import delegated validation methods.");
			/* No class of automatic delegation methods found; not a problem. */
		}
		
		return;
	}
	
	protected void setMappedClass(MappedClass argMappedClass) {
		myMappedClass = argMappedClass;
	}
	
	public static String convertToJavaIdentifier(String argString) {
		if (argString == null) {
			return "NULL";
		}
		if (argString.equals("")) {
			return "_EMPTY_STRING";
		}
		StringBuilder lclSB = new StringBuilder(argString.length());
		if (!Character.isJavaIdentifierStart(argString.charAt(0))) {
			lclSB.append('_');
		}
		for (int lclI = 0; lclI < argString.length(); ++lclI) {
			char lclC = argString.charAt(lclI);
			if (Character.isJavaIdentifierPart(lclC)) {
				lclSB.append(lclC);
			} else {
				lclSB.append('_');
			}
		}
		return lclSB.toString();
	}
	
	protected Class<?> determineSpecialJavaType(DatabaseColumn argDC, Class<?> argType) {
		Validate.notNull(argDC);
		
		if (argType == null) {
			return null;
		}
		String lclS = argDC.getName().toUpperCase();
		if (argType == String.class) {
			if (lclS.indexOf("CLASS_NAME") != -1) {
				return Class.class;
			}
		}
		return null;
	}
}
