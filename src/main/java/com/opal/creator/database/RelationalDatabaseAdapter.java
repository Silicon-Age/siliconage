package com.opal.creator.database;

import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.function.Function;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Element;

import com.siliconage.database.DatabaseUtility;
import com.siliconage.util.Fast3Set;
import com.siliconage.util.StringUtility;
import com.siliconage.util.Trinary;
import com.siliconage.util.UnimplementedOperationException;
import com.opal.creator.ClassGenerator;
import com.opal.creator.ClassMember;
import com.opal.creator.MappedClass;
import com.opal.creator.MappedForeignKey;
import com.opal.creator.MappedUniqueKey;
import com.opal.creator.MessageLevel;
import com.opal.creator.OpalParseContext;
import com.opal.creator.PolymorphicData;
import com.opal.creator.SingleTablePolymorphicData;
import com.opal.creator.SubtablePolymorphicData;
import com.opal.creator.UniqueKeyType;
import com.opal.types.JavaClass;
import com.opal.AbstractDatabaseEphemeralOpalFactory;
import com.opal.AbstractDatabaseIdentityOpalFactory;
import com.opal.AbstractFactoryMap;
import com.opal.ImplicitTableDatabaseQuery;
import com.opal.MultipleValueDatabaseOpalKey;
import com.opal.Opal;
import com.opal.OpalCache;
import com.opal.OpalUtility;
import com.opal.OpalKey;
import com.opal.PersistenceException;
import com.opal.SingleValueDatabaseOpalKey;

// FIXME: Name this something better
public abstract class RelationalDatabaseAdapter {

	protected static int DECIMAL_DIGITS_IN_MAX_LONG = 19;
	protected static int MAXIMUM_SAFE_DIGITS_FOR_LONG = DECIMAL_DIGITS_IN_MAX_LONG - 1;
	
	protected static int DECIMAL_DIGITS_IN_MAX_INT = 10;
	protected static int MAXIMUM_SAFE_DIGITS_FOR_INT = DECIMAL_DIGITS_IN_MAX_INT - 1;

	private static final boolean VALUE_CLASSES = false;
	
	private final DataSource myDataSource;
	
	private ArrayList<Index> myIndexes;
	private ArrayList<PrimaryKey> myPrimaryKeys;
	private ArrayList<ForeignKey> myForeignKeys;
	private ArrayList<ForeignKey> myArtificialForeignKeys = new ArrayList<>();
	private ArrayList<DatabaseColumn> myColumns;
	
	private String myDefaultSchema;
	
	private String myDataSourceName;
	
	private Map<String, String> myUserTypeMappings = new HashMap<>();
	
	public String getDefaultSchema() {
		return myDefaultSchema;
	}
	
	public void setDefaultSchema(String argDefaultSchema) {
		myDefaultSchema = argDefaultSchema;
	}
	
	
	protected RelationalDatabaseAdapter(DataSource argDataSource) {
		super();
		
		Validate.notNull(argDataSource);
		myDataSource = argDataSource;
	}
	
	protected String createSpecificOpalFactoryClassName(MappedClass argMC) {
		return getDatabaseOpalPrefix() + argMC.getOpalFactoryInterfaceName();
	}
	
	protected abstract String getDatabaseOpalPrefix();
	
	protected String createSpecificOpalFactoryPackageName(MappedClass argMC) {
		return argMC.getOpalFactoryPackageName() + '.' + getDatabasePackageSuffix();
	}
	
	protected abstract String getDatabasePackageSuffix();
	
	protected void createSpecificOpalFactory(OpalParseContext argOPC, MappedClass argMC) throws IOException {
		/* Create the Opal factory */
		String lclSpecificOpalFactoryClassName = createSpecificOpalFactoryClassName(argMC);
		String lclSpecificOpalFactoryPackageName = createSpecificOpalFactoryPackageName(argMC);
		
		String lclDirectory = StringUtility.makeDirectoryName(argOPC.getSourceDirectory(), lclSpecificOpalFactoryPackageName);
		
		MappedClass.ensureDirectoryExists(new File(lclDirectory));
		
		String lclOpalFactoryClassFileName = StringUtility.makeFilename(
				argOPC.getSourceDirectory(),
				lclSpecificOpalFactoryPackageName,
				lclSpecificOpalFactoryClassName
				);
		
		File lclOpalFactoryClassFile = new File(lclOpalFactoryClassFileName);
		
		try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclOpalFactoryClassFile)))) {
			final String lclICN = argMC.getInterfaceClassName();
			final String lclOCN = argMC.getOpalClassName();
			
			lclBW.println("package " + lclSpecificOpalFactoryPackageName + ';');
			lclBW.println();
			lclBW.println("import " + InitialContext.class.getName() + ';');
			lclBW.println("import " + NamingException.class.getName() + ';');
			lclBW.println("import " + DataSource.class.getName() + ';');
			if (argMC.isEphemeral() == false) {
				lclBW.println("import " + ResultSet.class.getName() + ';');
				lclBW.println("import " + SQLException.class.getName() + ';');
				lclBW.println();
				lclBW.println("import " + OpalCache.class.getName() + ';');
				lclBW.println("import " + OpalKey.class.getName() + ';');
				lclBW.println("import " + OpalUtility.class.getName() + ';');
			} else if (argMC.hasAtLeastOneUniqueKey()) {
				lclBW.println("import " + ImplicitTableDatabaseQuery.class.getName() + ';');
			}
			lclBW.println("import " + PersistenceException.class.getName() + ';');
			
			lclBW.println();
			
			if (argMC.getPolymorphicData() instanceof SubtablePolymorphicData) {
				lclBW.println("import " +  argMC.getOpalFactoryPackageName() + ".OpalFactoryFactory;");
			}
			lclBW.println("import " + argMC.getFullyQualifiedInterfaceClassName() + ';');
			if (argMC.isAbstract() == false) {
				lclBW.println("import " + argMC.getFullyQualifiedImplementationClassName() + ';');
			}
			// FIXME: Sometimes we'll also need to import the SpecifiedImplementationClassName
			lclBW.println("import " + argMC.getFullyQualifiedOpalClassName() + ';');
			lclBW.println("import " + argMC.getFullyQualifiedOpalFactoryInterfaceName() + ';');
			lclBW.println();
			
			lclBW.println("@javax.annotation.Generated(\"com.opal\")");
			if (argMC.isDeprecated()) {
				lclBW.println("\t@Deprecated");
			}
			
			Class<?> lclFactoryClass = argMC.isEphemeral() ? AbstractDatabaseEphemeralOpalFactory.class : AbstractDatabaseIdentityOpalFactory.class; 
			
			lclBW.println("public class " + lclSpecificOpalFactoryClassName + " extends " + lclFactoryClass.getName() + "<" + lclICN + ", " + lclOCN + "> implements " + argMC.getOpalFactoryInterfaceName() + " {");
			
			lclBW.println("\tprivate static final " + lclSpecificOpalFactoryClassName + " ourInstance = new " + lclSpecificOpalFactoryClassName + "();");
			lclBW.println();
			lclBW.println("\tpublic static final " + lclSpecificOpalFactoryClassName + " getInstance() { return ourInstance; }");
			lclBW.println();
			
			/* Create the do-nothing constructor.  It's protected because all of these classes are Singletons. */
			lclBW.println("\tprotected " + lclSpecificOpalFactoryClassName + "() {");
			lclBW.println("\t\tsuper();");
			lclBW.println("\t}");
			lclBW.println();
			
			if (argMC.usesSoftReferences() == false) {
				lclBW.println("\t@Override");
				lclBW.println("\tprotected boolean usesSoftReferences() {");
				lclBW.println("\t\treturn " + argMC.usesSoftReferences() + ";");
				lclBW.println("\t}");
				lclBW.println();
			}

			String lclDataSourceJNDIName = argOPC.getPoolMap().get(argMC.getPoolName());
			
			lclBW.println("\tprivate static final String DATA_SOURCE_JNDI_NAME = \"" + lclDataSourceJNDIName + "\";");
			lclBW.println("\tprivate DataSource myDataSource;");
			lclBW.println();
			lclBW.println("\tprivate static String getDataSourceJNDIName() {");
			lclBW.println("\t\treturn DATA_SOURCE_JNDI_NAME;");
			lclBW.println("\t}");
			lclBW.println();
			
			lclBW.println("\t@Override");
			lclBW.println("\tpublic synchronized DataSource getDataSource() throws PersistenceException {");
			lclBW.println("\t\tif (myDataSource == null) {");
			lclBW.println("\t\t\ttry {");
			lclBW.println("\t\t\t\tInitialContext lclC = new InitialContext();");
			lclBW.println("\t\t\t\tmyDataSource = (DataSource) lclC.lookup(getDataSourceJNDIName());");
			lclBW.println("\t\t\t\tif (myDataSource == null) {");
			lclBW.println("\t\t\t\t\tthrow new IllegalStateException(\"Could not get DataSource of name \" + getDataSourceJNDIName() + \" from the InitialContext\");");
			lclBW.println("\t\t\t\t}");
			lclBW.println("\t\t\t} catch (NamingException lclE) {");
			lclBW.println("\t\t\t\tthrow new PersistenceException(\"Could not get DataSource of name \" + getDataSourceJNDIName() + \".\", lclE);");
			lclBW.println("\t\t\t}");
			lclBW.println("\t\t}");
			lclBW.println("\t\treturn myDataSource;");
			lclBW.println("\t}");
			lclBW.println();

			Iterator<ClassMember> lclI;
			
			/* Database column names */
			lclBW.println("\tprivate static final String[] ourColumnNames = new String[] {");
			lclI = argMC.createClassMemberIterator();
			while (lclI.hasNext()) {
				ClassMember lclCM = lclI.next();
				if (!lclCM.isMapped()) {
					continue;
				}
				lclBW.print("\t\t\"");
				lclBW.print(lclCM.getDatabaseColumn().getName());
				lclBW.println("\", ");
			}
			lclBW.println("\t};");
			lclBW.println();
			
			lclBW.println("\tprotected static String[] getStaticColumnNames() { return ourColumnNames; }");
			lclBW.println();
			lclBW.println("\t@Override");
			lclBW.println("\tprotected String[] getColumnNames() { return ourColumnNames; }");
			lclBW.println();
	
			MappedUniqueKey lclPK = argMC.getPrimaryKey();
			
			if (argMC.isEphemeral() == false) {
				/* Database columns for the primary key; this will overlap with one of the inner classes.
				They should be consolidated.  It would also be nice to "reuse" the Strings from the other
				array, just for show. */
				lclBW.print("\tprivate static final String[] ourPrimaryKeyWhereClauseColumns = new String[] {");
				
				Validate.notNull(lclPK);
				lclI = lclPK.createClassMemberIterator();
				while (lclI.hasNext()) {
					ClassMember lclCM = lclI.next();
					lclBW.print('\"');
					lclBW.print(lclCM.getDatabaseColumn().getName());
					lclBW.print("\",");
				}
				lclBW.println("};");
				lclBW.println();
			}
	
			lclBW.println("\t@Override");
			lclBW.println("\tprotected String[] getFieldNames() { return " + argMC.getOpalClassName() + ".getStaticFieldNames(); }");
			lclBW.println();
			
			lclBW.println("\t@Override");
			lclBW.println("\tprotected Class<?>[] getFieldTypes() { return " + argMC.getOpalClassName() + ".getStaticFieldTypes(); }");
			lclBW.println();
			
			lclBW.println("\t@Override");
			lclBW.println("\tprotected boolean[] getFieldNullability() { return " + argMC.getOpalClassName() + ".getStaticFieldNullability(); }");
			lclBW.println();
			
			lclBW.println("\t@Override");
			lclBW.println("\tprotected com.opal.FieldValidator[] getFieldValidators() { return " + argMC.getOpalClassName() + ".getStaticFieldValidators(); }");
			lclBW.println();
						
			lclBW.println("\t@Override");
			lclBW.println("\tprotected " + lclOCN + " instantiate(Object[] argValues) {");
			if (argMC.isEphemeral() == false) {
				lclBW.println("\t\treturn new " + lclOCN + "(this, argValues);");
			} else {
				lclBW.println("\t\treturn new " + lclOCN + "(argValues);");
			}
			lclBW.println("\t}");
			lclBW.println();
				
			if (argMC.isCreatable() && argMC.hasSubclasses()) {
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
			
			if (argMC.isCreatable() && argMC.isConcrete()) {
				if (argMC.getPolymorphicData() == null) {
					lclBW.println("\t@Override");
				}
				lclBW.println("\tpublic " + lclOCN + " create() {");
				lclBW.println("\t\t" + lclOCN + " lclOpal;");
				lclBW.println("\t\tsynchronized (lclOpal = instantiate((Object[]) null)) {");
				// setUserFacing must be called first so that createSuperclassOpals (called by lclOpal.newObject() can propagate it upward
				lclBW.println("\t\t\tassert lclOpal.getUserFacing() == null;");
				lclBW.println("\t\t\tlclOpal.setUserFacing(new " + argMC.getImplementationClassName() + "(lclOpal));");
				lclBW.println("\t\t\tlclOpal.newObject();");
				lclBW.println("\t\t\tnewObject(lclOpal);");
				lclBW.println("\t\t}");
				lclBW.println("\t\treturn lclOpal;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (argMC.requiresTypedCreate() && argMC.implementsPolymorphicCreator()) {
				PolymorphicData lclPD = argMC.getPolymorphicData();
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
				
			lclBW.println("\t@Override");
			lclBW.println("\tprotected void determineUserFacing(" + lclOCN + " argO, boolean argConcrete) {");
			lclBW.println("\t\tassert argO != null;");
			PolymorphicData lclPD = argMC.getPolymorphicData();
			if (lclPD == null) {
	//			lclBW.println("\t\tassert argO.getUserFacing() == null;");
				if (argMC.hasSubclasses() == false) {
					lclBW.println("\t\tassert argConcrete == false;");
					lclBW.println("\t\tassert argO.getUserFacing() == null;");
					if (argMC.getSpecifiedImplementationClassName() != null) {
						lclBW.println("\t\targO.setUserFacing(new " + argMC.getSpecifiedImplementationClassName() + "(argO));");
					} else {
						lclBW.println("\t\targO.setUserFacing(new " + argMC.getImplementationClassName() + "(argO));");
					}
				} else {
					if (argMC.isAbstract()) {
						lclBW.println("\t\tassert argConcrete == false; // This type is abstract");
						lclBW.println("\t\targO.setUserFacing((" + lclICN + ") argO.get" + argMC.getSuperclassKey().getRoleSourceOpalFieldName() + "().getUserFacing());");
					} else {
						lclBW.println("\t\tif (argConcrete) {");
						lclBW.println("\t\t\tif (argO.getUserFacing() != null) {");
						lclBW.println("\t\t\t\treturn;");
						lclBW.println("\t\t\t} else {");
						if (argMC.getSpecifiedImplementationClassName() != null) {
							lclBW.println("\t\t\t\targO.setUserFacing(new " + argMC.getSpecifiedImplementationClassName() + "(argO));");
						} else {
							lclBW.println("\t\t\t\targO.setUserFacing(new " + argMC.getImplementationClassName() + "(argO));");
						}
						lclBW.println("\t\t\t}");
						lclBW.println("\t\t} else {");
						lclBW.println("\t\t\tassert argO.getUserFacing() == null;");
						lclBW.println("\t\t\targO.setUserFacing((" + lclICN + ") argO.get" + argMC.getSuperclassKey().getRoleSourceOpalFieldName() + "().getUserFacing());");
						lclBW.println("\t\t}");
					}
					/* FIXME: Would it be better to "trace up and over" like we do when we have a direct Polymorphism record? */
				}
			} else if (lclPD instanceof SingleTablePolymorphicData) {
				lclBW.println("\t\tassert argConcrete == false;");
				lclBW.println("\t\tassert argO.getUserFacing() == null;");
				SingleTablePolymorphicData lclSPD = (SingleTablePolymorphicData) lclPD;
				String lclNV = null;
				MappedClass lclMappedClassWithActualTypeField = argMC;
				for (int lclJ = 0; lclJ < lclSPD.getDereferenceKeys().size(); ++lclJ) {
					MappedForeignKey lclMFK = lclSPD.getDereferenceKeys().get(lclJ);
					assert lclMFK.getSourceMappedClass() == lclMappedClassWithActualTypeField;
					lclMappedClassWithActualTypeField = lclMFK.getTargetMappedClass();
					String lclOV = (lclJ == 0) ? "argO" : lclNV;
					lclNV = "lcl" + lclJ;
					lclBW.println("\t\t" + lclMFK.getTargetMappedClass().getFullyQualifiedOpalClassName() + " " + lclNV + " = " + lclOV + ".get" + lclMFK.getRoleSourceOpalFieldName() + "();");
					lclBW.println("\t\tif (" + lclNV + " == null) {");
					lclBW.println("\t\t\tthrow new IllegalStateException();");
					lclBW.println("\t\t}");
				}
				ClassMember lclCM = lclMappedClassWithActualTypeField.getClassMemberByColumnName(lclSPD.getColumnName());
				if (lclCM == null) {
					String lclS = "Could not find column \"" + lclSPD.getColumnName() + "\" for table " + lclMappedClassWithActualTypeField.getTableName() + "."; 
					argMC.complain(MessageLevel.Fatal, lclS);
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
						lclBW.println("\t\t" + lclICN + " lclUF = lclJavaClass.newInstance(argO);");
					} else {
						throw new IllegalStateException("Could not use type " + lclCM.getMemberType() + " to figure out the class to construct when doing single-table polymorphism for " + argMC);
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
				MappedClass lclMappedClassWithActualTypeField = argMC;
				for (int lclJ = 0; lclJ < lclSPD.getDereferenceKeys().size(); ++lclJ) {
					MappedForeignKey lclMFK = lclSPD.getDereferenceKeys().get(lclJ);
					assert lclMFK.getSourceMappedClass() == lclMappedClassWithActualTypeField;
					lclMappedClassWithActualTypeField = lclMFK.getTargetMappedClass();
					String lclOV = (lclJ == 0) ? "argO" : lclNV;
					lclNV = "lcl" + lclJ;
					lclBW.println("\t\t" + lclMFK.getTargetMappedClass().getFullyQualifiedOpalClassName() + " " + lclNV + " = " + lclOV + ".get" + lclMFK.getRoleSourceOpalFieldName() + "();");
					lclBW.println("\t\tif (" + lclNV + " == null) {");
					lclBW.println("\t\t\tthrow new IllegalStateException();");
					lclBW.println("\t\t}");
				}
				ClassMember lclCM = lclMappedClassWithActualTypeField.getClassMemberByColumnName(lclSPD.getColumnName());
				if (lclCM == null) {
					String lclS ="Could not find column \"" + lclSPD.getColumnName() + "\" for table " + lclMappedClassWithActualTypeField.getTableName() + ".";
					argMC.complain(MessageLevel.Fatal, lclS);
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
						for (MappedClass lclSubclass : argMC.getSubclasses()) { /* FIXME: What about intermediate abstract ones? */
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
							if (lclSubclass == argMC) {
								/* FIXME: The following line needs to be augmented to work with SingleTablePolymorphism */
								lclBW.println("\t\t\tlclUF = new " + argMC.getImplementationClassName() + "(argO);");
							} else {
								lclBW.println("\t\t\t" + lclSubclass.getFullyQualifiedOpalFactoryInterfaceName() + " lclOF = OpalFactoryFactory.getInstance().get" + lclSubclass.getOpalFactoryInterfaceName() + "();");
								lclBW.println("\t\t\tassert lclOF != null;");
								lclBW.print("\t\t\t" + lclSubclass.getFullyQualifiedOpalClassName() + " lclO = lclOF.");
								StringBuilder lclArgs = new StringBuilder(128);
								MappedUniqueKey lclSuperclassPK = argMC.getPrimaryKey();
								MappedUniqueKey lclSubclassPK = lclSubclass.getPrimaryKey();
								lclBW.print(lclSubclassPK.generateOpalFactoryMethodName(lclSubclass.needsConcreteFactoryMethod()));
								lclBW.print('(');
								
								assert lclSuperclassPK.getClassMembers().size() == lclSubclassPK.getClassMembers().size();
								boolean lclFirst = true;
								for (int lclJ = 0; lclJ < lclSuperclassPK.getClassMembers().size(); ++lclJ) {
									ClassMember lclCM1 = lclSuperclassPK.getClassMembers().get(lclJ);
									ClassMember lclCM2 = lclSubclassPK.getClassMembers().get(lclJ);
									if (lclCM2.getMemberType().isAssignableFrom(lclCM1.getMemberType()) == false) {
										throw new IllegalStateException("Argument type " + lclCM2.getMemberType().getName() + " is not assignable from value " + lclCM1.getMemberType().getName() + ".");
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
			
			if (argMC.isEphemeral() == false) {
				
				createGeneratedKeysMethods(lclBW, argMC);
				
				createComplicatedDefaultMethods(lclBW, argMC);
				
				/* afterInsert */
								
				/* The afterInsert method is delegated to the specific instance since it will typically involve
				 * database-specific code for updating automatically generated unique identifiers for the classes;
				 * e.g, sequences in Oracle and identities in SQL Server and Sybase. */
				createAfterInsertMethod(lclBW, argMC);
		
				/* The newObject method is also going to be database specific.  Oracle, for example, will need
				 * to check the next value of a sequence. */
				createNewObjectMethod(lclBW, argMC);
			}
			
			/* getFullyQualifiedTableName() */
			createGetFullyQualifiedTableNameMethod(lclBW, argMC);
			
			if (argMC.isEphemeral() == false) {
				/* As in the case of the afterInsert method, this is expected to be delegated to a concrete
				 * subclass that will know how to interpret the concrete Table class to generate a FQTN. */
				/* registerOpal */
				lclBW.println("\t@Override");
				lclBW.println("\tprotected void registerNewOpal(" + lclOCN + " argOpal) {");
				if (argMC.isCreatable() || argMC.isUpdatable()) {
					lclBW.println("\t\tregisterOpal(argOpal, argOpal.getNewValues());");
				} else {
					lclBW.println("\t\tregisterOpal(argOpal, argOpal.getValues());");
				}
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\t@Override");
				lclBW.println("\tprotected void registerOldOpal(" + lclOCN + " argOpal) {");
				if (argMC.isCreatable() || argMC.isUpdatable()) {
					lclBW.println("\t\tregisterOpal(argOpal, argOpal.getOldValues());");
				} else {
					lclBW.println("\t\tregisterOpal(argOpal, argOpal.getValues());");
				}
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\tprotected void registerOpal(" + lclOCN + " argOpal, Object[] argValues) {");
				lclBW.println("\t\tif (argValues == null) { throw new IllegalStateException(); }");
				lclBW.println("\t\tif (argValues.length != " + argMC.getClassMemberCount() + ") { throw new IllegalStateException(); }");
				lclBW.println("\t\tOpalCache<" + lclOCN + "> lclOC = getCache();");
				lclBW.println("\t\tsynchronized (lclOC) {");
				for (MappedUniqueKey lclMUK : argMC.getMappedUniqueKeys()) {
					UniqueKeyType lclT = lclMUK.getType();
					Validate.isTrue(lclT == UniqueKeyType.UNIQUE || lclT == UniqueKeyType.UNIQUE_IF_ENTIRELY_NOT_NULL);
					if (lclT == UniqueKeyType.UNIQUE_IF_ENTIRELY_NOT_NULL && lclMUK.couldHaveNullComponent()) {
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
				if (argMC.isCreatable() || argMC.isUpdatable()) {
					lclBW.println("\t\tObject[] lclOldValues = argOpal.getOldValues();");
				} else {
					lclBW.println("\t\tObject[] lclOldValues = argOpal.getValues();");
				}
				lclBW.println("\t\tif (lclOldValues == null) { throw new IllegalStateException(); }");
				lclBW.println("\t\tif (lclOldValues.length != " + argMC.getClassMemberCount() + ") { throw new IllegalStateException(); }");
				
				lclBW.println("\t\tOpalCache<" + lclOCN + "> lclOC = getCache();");
				lclBW.println("\t\tsynchronized (lclOC) {");
				for (MappedUniqueKey lclMUK : argMC.getMappedUniqueKeys()) {
					UniqueKeyType lclT = lclMUK.getType();
					Validate.isTrue(lclT == UniqueKeyType.UNIQUE || lclT == UniqueKeyType.UNIQUE_IF_ENTIRELY_NOT_NULL);
					if (lclT == UniqueKeyType.UNIQUE_IF_ENTIRELY_NOT_NULL && lclMUK.couldHaveNullComponent()) {
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
				if (argMC.isCreatable() || argMC.isUpdatable()) {
					lclBW.println("\t\torg.apache.commons.lang3.Validate.notNull(argOpal);");
					if (argMC.isCreatable() || argMC.isUpdatable()) {
						lclBW.println("\t\tObject[] lclOldValues = argOpal.getOldValues();");
					} else {
						lclBW.println("\t\tObject[] lclOldValues = argOpal.getValues();");
					}
					lclBW.println("\t\tif (lclOldValues == null) { throw new IllegalStateException(); }");
					lclBW.println("\t\tif (lclOldValues.length != " + argMC.getClassMemberCount() + ") { throw new IllegalStateException(); }");
					if (argMC.isCreatable() || argMC.isUpdatable()) {
						lclBW.println("\t\tObject[] lclNewValues = argOpal.getNewValues();");
					} else {
						lclBW.println("\t\tObject[] lclNewValues = argOpal.getValues();");
					}
					lclBW.println("\t\tif (lclNewValues == null) { throw new IllegalStateException(); }");
					lclBW.println("\t\tif (lclNewValues.length != " + argMC.getClassMemberCount() + ") { throw new IllegalStateException(); }");
					lclBW.println("\t\tOpalCache<" + lclOCN + "> lclOC = getCache();");
					lclBW.println("\t\tsynchronized (lclOC) {");
					
					for (MappedUniqueKey lclMUK : argMC.getMappedUniqueKeys()) {
						UniqueKeyType lclT = lclMUK.getType();
						Validate.isTrue(lclT == UniqueKeyType.UNIQUE || lclT == UniqueKeyType.UNIQUE_IF_ENTIRELY_NOT_NULL);
						// FIXME: Fix indentation of generated code
						// FIXME: All of this generated method should be reviewed; I suspect it is no longer correct -- RRH 
						lclBW.println("\t\t\t{");
						lclBW.println("\t\t\t\tOpalKey<" + lclOCN + "> lclOldKey;");
						lclBW.println("\t\t\t\tOpalKey<" + lclOCN + "> lclNewKey;");
						lclBW.print("\t\t\t\tif (!(");
						lclBW.print(lclMUK.generateKeyEqualityCondition("lclNewValues", "lclOldValues"));
						lclBW.println(")) {");
						
						boolean lclCouldBeNull = lclMUK.couldHaveNullComponent();
						
						if (lclT == UniqueKeyType.UNIQUE_IF_ENTIRELY_NOT_NULL && lclCouldBeNull) {
							lclBW.print("\t\t\t\t\tif (");
							lclBW.print(lclMUK.generateNotNullJavaCondition("lclNewValues"));
							lclBW.println(") {");
							lclBW.println("\t\t\t\t\t\tlclNewKey = " + lclMUK.generateOpalKeyConstructorCall("lclNewValues") + ';');
							lclBW.println("\t\t\t\t\t} else {");
							lclBW.println("\t\t\t\t\t\tlclNewKey = null;");
							lclBW.println("\t\t\t\t\t}");
						} else {
							lclBW.println("\t\t\t\t\tlclNewKey = " + lclMUK.generateOpalKeyConstructorCall("lclNewValues") + ';');
						}
						if (lclT == UniqueKeyType.UNIQUE_IF_ENTIRELY_NOT_NULL && lclCouldBeNull) {
							lclBW.print("\t\t\t\t\tif (");
							lclBW.print(lclMUK.generateNotNullJavaCondition("lclOldValues"));
							lclBW.println(") {");
							lclBW.println("\t\t\t\t\t\tlclOldKey = " + lclMUK.generateOpalKeyConstructorCall("lclOldValues") + ';');
							lclBW.println("\t\t\t\t\t} else {");
							lclBW.println("\t\t\t\t\t\tlclOldKey = null;");
							lclBW.println("\t\t\t\t\t}");
						} else {
							lclBW.println("\t\t\t\t\tlclOldKey = " + lclMUK.generateOpalKeyConstructorCall("lclOldValues") + ';');
						}
						if (lclT == UniqueKeyType.UNIQUE_IF_ENTIRELY_NOT_NULL && lclCouldBeNull) {
							lclBW.println("\t\t\t\t\tif (lclOldKey != null) { lclOC.removeOpal(lclOldKey); }");
							lclBW.println("\t\t\t\t\tif (lclNewKey != null) { lclOC.addOpal(lclNewKey, argOpal, true); } /* true = SoftReference */");
						} else {
							lclBW.println("\t\t\t\t\tlclOC.removeOpal(lclOldKey);");
							lclBW.println("\t\t\t\t\tlclOC.addOpal(lclNewKey, argOpal, true); /* true = SoftReference */");
						}
						lclBW.println("\t\t\t\t}");
						lclBW.println("\t\t\t}");
					}
					lclBW.println("\t\t}");
					lclBW.println("\t\treturn;");
				} else {
					lclBW.println("\t\t/* No keys to update for ImmutableOpals */");
				}
				lclBW.println("\t}");
				lclBW.println();
			}
			
			if (argMC.isEphemeral() == false) {
				Validate.notNull(lclPK);
				
				/* getPrimaryKeyWhereClauseColumns() */
				lclBW.println("\t@Override");
				lclBW.println("\tprotected String[] getPrimaryKeyWhereClauseColumns() {");
				lclBW.println("\t\treturn ourPrimaryKeyWhereClauseColumns;");
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\t@Override");
				lclBW.println("\tprotected OpalKey<" + lclOCN +"> createOpalKeyForReloading(" + lclOCN + " argOpal) {");
				if (argMC.isCreatable() || argMC.isUpdatable()) {
					lclBW.println("\t\tObject[] lclValues = argOpal.getNewValues();");
				} else {
					lclBW.println("\t\tObject[] lclValues = argOpal.getValues();");
				}
				lclBW.println("\t\treturn " + lclPK.generateOpalKeyConstructorCall("lclValues") + ';');
				lclBW.println("\t}");
				lclBW.println();
			}
			
			/* Create a factory method for each foreign key */
			Iterator<MappedForeignKey> lclFKI = argMC.createForeignKeyIterator();
			while (lclFKI.hasNext()) {
				MappedForeignKey lclFK = lclFKI.next();
				
				if (lclFK.hasBackCollection() == false) {
					continue;
				}
				
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + lclFK.getCollectionType().getName() + "<" + lclOCN + "> " + lclFK.getSource().generateOpalFactoryFunctionDefinition() + " /* throws PersistenceException */ {");
				Iterator<ClassMember> lclJ;
				boolean lclFirst;
				
				lclBW.print("\t\tfinal Object[] lclParameters = new Object[] { ");
				lclJ = lclFK.getSource().iterator();
				lclFirst = true;
				while (lclJ.hasNext()) {
					ClassMember lclCM = lclJ.next();
					if (lclFirst) {
						lclFirst = false;
					} else {
						lclBW.print(", ");
					}
					lclBW.print(lclCM.getObjectMutatorArgumentName());
				}
				lclBW.println(" };");
				
				lclBW.print("\t\tfinal String[] lclFieldNames = new String[] { ");
				lclJ = lclFK.getSource().iterator();
				lclFirst = true;
				while (lclJ.hasNext()) {
					ClassMember lclCM = lclJ.next();
					if (lclFirst) {
						lclFirst = false;
					} else {
						lclBW.print(", ");
					}
					lclBW.print("\"" + lclCM.getDatabaseColumn().getName() + "\"");
				}
				lclBW.println(" };");
				lclBW.println("\t\t" + lclFK.getCollectionType().getName() + "<" + lclOCN + "> lclCollection = new " + lclFK.getCollectionType().getName() + "<" /* + lclOCN */ + ">();");
				
				lclBW.println("\t\tload(getFullyQualifiedTableName(), lclFieldNames, lclParameters, null, lclCollection);");
				lclBW.println("\t\treturn lclCollection;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			/* Create a factory method for each unique key */
			for (MappedUniqueKey lclMUK : argMC.getMappedUniqueKeys()) {
				lclBW.println("\t@Override");
				lclBW.print("\tpublic " + lclOCN + " " + lclMUK.generateOpalFactoryMethodName(false) + "(");
				
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
				if (argMC.isEphemeral() == false) {
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
					
					if (argMC.needsConcreteFactoryMethod()) {
						if (lclMUK.isPrimaryKey()) {
							lclBW.println("\t@Override");
							lclBW.print("\tpublic " + lclOCN + " " + lclMUK.generateOpalFactoryMethodName(true) + "(");
							
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
			
			/* Get all.  FIXME: I don't think this actually tracks newly created instances. */
			if (argMC.isGetAll()) {
				String lclAll = "myAll" + lclOCN;
				
				lclBW.println("\tprivate java.util.ArrayList<" + lclOCN + "> " + lclAll + " = null;");
				lclBW.println();
				lclBW.println("\tprotected java.util.ArrayList<" + lclOCN + "> getAll" + lclOCN + "() {");
				lclBW.println("\t\tif (" + lclAll + " == null) {");
				lclBW.println("\t\t\t" + lclAll + " = loadAll(getFullyQualifiedTableName());");
				lclBW.println("\t\t}");
				lclBW.println("\t\treturn " + lclAll + ';');
				lclBW.println("\t}");
				lclBW.println();
				lclBW.println("\tpublic synchronized void acquireAll" + lclOCN + "(java.util.Collection<" + lclOCN + "> argCollection) {");
				lclBW.println("\t\tif (argCollection != null) {");
				lclBW.println("\t\t\targCollection.addAll(getAll" + lclOCN + "());");
				lclBW.println("\t\t}");
				lclBW.println("\t}");
				lclBW.println();
				lclBW.println("\tpublic synchronized java.util.Iterator createAll" + lclOCN + "Iterator() {");
				lclBW.println("\t\treturn getAll" + lclOCN + "().iterator();");
				lclBW.println("\t}");
				lclBW.println();
				lclBW.println("\tpublic synchronized int getAll" + lclOCN + "Count() {");
				lclBW.println("\t\treturn getAll" + lclOCN + "().size();");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			/* Create an OpalKey representing the primary key for the row.  These are constructed to
			determine if the object already exists in the cache so we can return an existing instance rather
			than a new one. */
			if (argMC.isEphemeral() == false) {
				lclBW.println("\t@Override");
				lclBW.println("\tprotected OpalKey<" + lclOCN + "> createOpalKeyForRow(ResultSet argRS) throws SQLException {");
				MappedUniqueKey lclMUK = argMC.getPrimaryKey();
				
				lclBW.println("\t\treturn new " + lclMUK.getOpalKeyClassName() + "(");
				lclI = lclMUK.createClassMemberIterator();
				boolean lclFirst = true;
				while (lclI.hasNext()) {
					ClassMember lclCM = lclI.next();
					if (lclFirst) {
						lclFirst = false;
					} else {
						lclBW.println(','); /* End the previous line with a comma */
					}
					Class<?> lclMemberType = lclCM.getMemberType();
					lclBW.print("\t\t\tOpalUtility.convertTo(" + lclMemberType.getName() + ".class, argRS.getObject(\"" + lclCM.getDatabaseColumn().getName() + "\"))");
				}
				lclBW.println(); /* End the final line (without a comma) */
				
				lclBW.println("\t\t);");
				lclBW.println("\t}");
				lclBW.println();
			}
	
			/* Static nested classes for representing keys for this object */
			Iterator<ClassMember> lclJ;
			
			if (argMC.isEphemeral() == false) {
				for (MappedUniqueKey lclMUK : argMC.getMappedUniqueKeys()) {
					boolean lclSingleValue = lclMUK.getClassMembers().size() == 1;
					Class<?> lclDOKClass = lclSingleValue  ? SingleValueDatabaseOpalKey.class : MultipleValueDatabaseOpalKey.class;
					String lclValueParticle = VALUE_CLASSES ? "/* value */ " : "";
					lclBW.println("\t/* package */ static " + lclValueParticle + "class " + lclMUK.getOpalKeyClassName() + " extends " + lclDOKClass.getName() + "<" + lclOCN + "> {");
					
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
						lclBW.print(lclCM.getMemberParameterizedTypeName(false) + " " + lclCM.getPrimitiveMutatorArgumentName());
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
			
			if (argMC.isEphemeral() == false) {
				MappedUniqueKey lclUSK = argMC.getUniqueStringKey();
				if (lclUSK == null) {
					throw new IllegalStateException("lclUSK is null");
				}
				lclBW.println("\t@Override");
				lclBW.println("\tpublic " + lclOCN + " forUniqueString(String argUniqueString) {");
				lclBW.println("\t\tif (org.apache.commons.lang3.StringUtils.isBlank(argUniqueString)) {");
				lclBW.println("\t\t\treturn null;");
				lclBW.println("\t\t}");
				lclBW.println("\t\tString[] lclArgs = argUniqueString.split(\"\\\\|\");");
				lclBW.println("\t\tassert lclArgs.length == " + lclUSK.sizeClassMember() + ';');
				lclBW.println("\t\treturn forOpalKey(");
				lclBW.println("\t\t\tnew " + lclUSK.getOpalKeyClassName() + "(");
				lclI = lclUSK.createClassMemberIterator();
				int lclIndex = 0;
				while (lclI.hasNext()) {
					ClassMember lclCM = lclI.next();
					lclBW.print("\t\t\t\t" + OpalUtility.getCodeToConvert(lclCM.getMemberType(), String.class, "lclArgs[" + lclIndex + "]", false));
					if (lclIndex < lclUSK.sizeClassMember() - 1) {
						lclBW.print(",");
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
	
	protected abstract void createAfterInsertMethod(PrintWriter argBW, MappedClass argMappedClass);
	
	protected abstract void createNewObjectMethod(PrintWriter argBW, MappedClass argMappedClass);
	
	protected abstract void createGetFullyQualifiedTableNameMethod(PrintWriter argBW, MappedClass argMC);
	
	protected void createForeignKeys(Map<TableName, MappedClass> argMappedClasses, MappedClass argMappedClass /*, boolean argSampleCollections */) /* throws SQLException */ {
		/* Get foreign keys from the database */
		Collection<ForeignKey> lclFKs = getForeignKeysFrom(argMappedClass.getTableName());
				
		/* Modify them with the artificial foreign keys*/
		Iterator<ForeignKey> lclI = getArtificialForeignKeys().iterator();
		
		AFKLoop:
		while (lclI.hasNext()) {
			ForeignKey lclAFK = lclI.next();
			if (lclAFK.getSourceKey().getTableName() == argMappedClass.getTableName() ) {
				lclI.remove();
				Iterator<ForeignKey> lclJ = lclFKs.iterator();
				while (lclJ.hasNext()) {
					ForeignKey lclFK = lclJ.next();
					if (lclFK.getTargetKey().getTableName().equals(lclAFK.getTargetKey().getTableName())) {
						if (lclFK.getSourceKey().getColumnNames().equals(lclAFK.getSourceKey().getColumnNames())) {
							if (lclFK.getTargetKey().getColumnNames().equals(lclAFK.getTargetKey().getColumnNames())) {
								if (lclAFK.getSourceRolePrefix() != null) {
									lclFK.setSourceRolePrefix(lclAFK.getSourceRolePrefix());
								}
								if (lclAFK.getTargetRolePrefix() != null) {
									lclFK.setTargetRolePrefix(lclAFK.getTargetRolePrefix());
								}
								if (lclAFK.getJoinQueryFactoryName() != null) {
									lclFK.setJoinQueryFactoryName(lclAFK.getJoinQueryFactoryName());
								}
								lclFK.setCollectionClass(lclAFK.getCollectionClass());
								if (lclAFK.getSourceAccess() != null) {
									lclFK.setSourceAccess(lclAFK.getSourceAccess());
								}
								if (lclAFK.getTargetAccess() != null) {
									lclFK.setTargetAccess(lclAFK.getTargetAccess());
								}
								if (lclAFK.getSpecifiedBaseName() != null) {
									lclFK.setSpecifiedBaseName(lclAFK.getSpecifiedBaseName());
								}
								if (lclAFK.getSpecifiedCollectionName() != null) {
									lclFK.setSpecifiedCollectionName(lclAFK.getSpecifiedCollectionName());
								}
								if (lclAFK.getSpecifiedCollectionItemName() != null) {
									lclFK.setSpecifiedCollectionItemName(lclAFK.getSpecifiedCollectionItemName());
								}
								if (lclAFK.isOneToOne()) {
									lclFK.setOneToOne(lclAFK.isOneToOne());
								}
								lclFK.setMapped(lclAFK.isMapped());
								continue AFKLoop;
							}
						}
					}
				}
				/* We will only get here if we didn't find the artificial foreign key in the list of
				 * foreign keys from the database. */
				lclFKs.add(lclAFK);
			}
		}
		
		for (ForeignKey lclFK : lclFKs) {
			if (lclFK.isMapped() == false) {
				continue;
			}
			/* This foreign key has the Table of this MappedClass as its source. */
			TableName lclTargetTableName = lclFK.getTargetKey().getTableName();
			
			/* Find the MappedClass whose Table is its target. */
			MappedClass lclTargetMappedClass = argMappedClasses.get(lclTargetTableName);
			if (lclTargetMappedClass == null) {
				boolean lclAllUnmapped = true;
				for (String lclColumnName : lclFK.getSourceKey().getColumnNames()) {
					if (lclColumnName != null) {
						ClassMember lclCM = argMappedClass.getClassMemberByColumnName(lclColumnName);
						if (lclCM.isMapped()) {
							lclAllUnmapped = false;
						}
					}
				}
				if (lclAllUnmapped == false) {
					argMappedClass.complain(MessageLevel.Warning, "Ignoring foreign key " + lclFK + " as its target could not be found (but at least one column in the source key is mapped).");
				} else {
					argMappedClass.complain(MessageLevel.Info, "Ignoring foreign key " + lclFK + " as its target could not be found (but all columns in the source key are unmapped).");
				}
				continue;
			}
			
			/* Drop Foreign Keys that have one or more unmapped source fields. */
			boolean lclAllMapped = true;
			Iterator<String> lclCMI = lclFK.getSourceKey().getColumnNames().iterator();
			while (lclCMI.hasNext()) {
				String lclS = lclCMI.next();
				ClassMember lclCM = argMappedClass.getClassMemberByColumnName(lclS);
				Validate.notNull(lclCM, "Could not find ClassMember for column name \"" + lclS + "\".");
				if (lclCM.isMapped() == false) {
					lclAllMapped = false;
				}
			}
			if (lclAllMapped == false) {
				argMappedClass.complain(MessageLevel.Info, "Ignoring foreign key " + lclFK + " because one or more source fields is not mapped.");
				continue;
			}
			
			MappedForeignKey lclMFK = new MappedForeignKey(lclFK, argMappedClass, lclTargetMappedClass);
			argMappedClass.addForeignKey(lclMFK);
			lclTargetMappedClass.addTargetForeignKey(lclMFK);
		}
	}
	
	protected Class<?> determineCollectionClassFromStatistics(MappedClass argMC, ForeignKey argFK) throws SQLException {
		Validate.notNull(argFK);
		
		String lclSQL = generateForeignKeyStatisticsSQL(argFK);
		
		try (Connection lclC = getDataSource().getConnection()) {
			try (ResultSet lclRS = DatabaseUtility.select(lclC, lclSQL)) {
				/* Check to see if there's actually a row; if there's a foreign key with no rows, we won't
				 * get any data and the database will vomit.
				 */
				if (lclRS.next() == true) {
					final float lclAverage = lclRS.getFloat("avg");
					final float lclDeviation = lclRS.getFloat("stdev");
					
					if (lclAverage + lclDeviation < 3.0f) {
						argMC.complain(MessageLevel.Debug, "For " + argFK + /* " min = " + lclMinimum + */ " avg = " + lclAverage + /* " max = " + lclMaximum + */ " dev = " + lclDeviation +"; choosing Fast3Set.");
						return Fast3Set.class;
					}
				}
				Validate.isTrue(!lclRS.next());
				
				return ForeignKey.USE_DEFAULT_COLLECTION_CLASS;
			} // autoclose lclRS
		} catch (SQLException lclE) {
			argMC.complain(MessageLevel.Error, "Could not execute SQL \"" + lclSQL + "\" for foreign key " + argFK.getName() + " linking table " + argFK.getSourceKey().getTableName() + " and " + argFK.getTargetKey().getTableName() + ".");
			throw lclE;
			// autoclose lclC
		}
	}
	
	protected abstract String generateForeignKeyStatisticsSQL(ForeignKey argFK);
	
	protected void createMappedUniqueKeys(MappedClass argMappedClass) {
		/* Load the unique indexes */
		Collection<Index> lclIndexes = getUniqueIndexes(argMappedClass.getTableName());
		
		Iterator<Index> lclI = lclIndexes.iterator();
		while (lclI.hasNext()) {
			Index lclIndex = lclI.next();
			if (lclIndex.isMapped()) {
				MappedUniqueKey lclMUK = new MappedUniqueKey(argMappedClass, lclIndex);
				argMappedClass.addMappedUniqueKey(lclMUK);
			}
		}
	}
	/* FEATURE:  Check that default schema exists when reading in file */
	
	protected void createFactoryMap(Collection<MappedClass> argMappedClasses, String argPackageRoot, String argSourceDirectory) throws IOException {
		String lclPackage = argPackageRoot + ".application"; // TODO
		
		String lclFM = "FactoryMap"; // FIXME: Make name configurable
		
		String lclOpalClassFileName = StringUtility.makeFilename(argSourceDirectory, lclPackage, lclFM);
		
		File lclOpalClassFile = new File(lclOpalClassFileName);
		
		try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclOpalClassFile)))) {
			lclBW.println("package " + lclPackage + ';');
			lclBW.println();
			lclBW.println("import com.opal.AbstractFactoryMap;");
			lclBW.println();
			lclBW.println("public class " + lclFM + " extends AbstractFactoryMap {");
			lclBW.println();
			lclBW.println("\tprivate static final " + lclFM + " ourInstance = new " + lclFM + "();");
			lclBW.println();
			
			/* A static accessor to obtain a reference to the singleton instance. */
			
			lclBW.println("\tpublic static FactoryMap getInstance() {");
			lclBW.println("\t\treturn ourInstance;");
			lclBW.println("\t}");
			lclBW.println();
			
			String lclCollections = argMappedClasses.stream().map(MappedClass::getCollections).filter(Objects::nonNull).findAny().orElse(null);
			if (lclCollections != null) {
				String lclCollectionClassName;
				if (lclCollections.equalsIgnoreCase("Java")) {
					lclCollectionClassName = java.util.HashSet.class.getName();
				} else if (lclCollections.equalsIgnoreCase("Trove")) {
					lclCollectionClassName = "gnu.trove.set.hash.THashSet";
				} else {
					throw new IllegalStateException("Unknown value for Collections");
				}
				
				lclBW.println("\tpublic static <T extends " + Opal.class.getName() + "<?>> " + Supplier.class.getName() + "<" + Set.class.getName() + "<T>> " + AbstractFactoryMap.NO_ARG_CTOR_SET_CREATOR_METHOD_NAME + "() {");
				lclBW.println("\t\treturn " + lclCollectionClassName + "::new;");
				lclBW.println("\t}");
				lclBW.println();
				
				lclBW.println("\tpublic static <T extends " + Opal.class.getName() + "<?>> " + Function.class.getName() + "<" + Collection.class.getName() + "<T>, " + Set.class.getName() + "<T>> " + AbstractFactoryMap.COLLECTION_ARG_CTOR_SET_CREATOR_METHOD_NAME + "() {");
				lclBW.println("\t\treturn " + lclCollectionClassName +"::new;");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			lclBW.println("\tprivate FactoryMap() {");
			lclBW.println("\t\tsuper();");
			lclBW.println("\t}");
			lclBW.println();
			
			lclBW.println("\t@Override");
			lclBW.println("\tprotected void initialize() {");
			for (MappedClass lclMC : argMappedClasses) {
				if (lclMC.isEphemeral() == false) {
					lclBW.println("\t\tput(" + lclMC.getFullyQualifiedInterfaceClassName() + ".class, " + lclMC.getFullyQualifiedFactoryClassName() + ".getInstance());");
				}
			}
			lclBW.println("\t}");
			lclBW.println();
			
			lclBW.println("}");
		}
	}
	
	protected void createOpalFactoryFactory(Collection<MappedClass> argMappedClasses, String argSpecificOpalFactoryFactoryClassName, String argPackageRoot, String argSourceDirectory) throws IOException {
		String lclPackage = argPackageRoot + ".persistence"; // TODO
		
		String lclOpalClassFileName = StringUtility.makeFilename(argSourceDirectory, lclPackage, "OpalFactoryFactory");
		
		File lclOpalClassFile = new File(lclOpalClassFileName);
		
		try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclOpalClassFile)))) {
			lclBW.println("package " + lclPackage + ';');
			lclBW.println();
			lclBW.println("import com.opal.AbstractOpalFactoryFactory;");
			lclBW.println();
			lclBW.println("public abstract class OpalFactoryFactory extends AbstractOpalFactoryFactory {");
			lclBW.println("\tpublic static OpalFactoryFactory getInstance() { return " + argSpecificOpalFactoryFactoryClassName + ".getSpecificInstance(); }");
			lclBW.println();
			Iterator<MappedClass> lclI = argMappedClasses.iterator();
			while (lclI.hasNext()) {
				MappedClass lclMC = lclI.next();
				Validate.notNull(lclMC);
				if (lclMC.isDeprecated()) {
					lclBW.println("\t@Deprecated");
				}
				lclBW.println("\tpublic abstract " + lclMC.getOpalFactoryInterfaceName() + " get" + lclMC.getOpalFactoryInterfaceName() + "();");
			}
			lclBW.println("}");
		} // autoclose lclBW
	}
	
	protected void createPrimaryKeys(MappedClass argMappedClass) {
		assert argMappedClass != null;
		
		/* Determine which of the MappedUniqueKeys of this MappedClass is the PrimaryKey
		 * and mark it as such. */
		
		PrimaryKey lclPK = getPrimaryKey(argMappedClass.getTableName());
		
		/* Non-ephemeral entities (tables) must have a primary key. */
		
		if (argMappedClass.isEphemeral()) {
			/* Nothing to do. */
			return;
		}
		
		if (lclPK == null) {
			throw new IllegalStateException("No primary key found for non-ephemeral entity " + argMappedClass);
		}
		
		MappedUniqueKey lclPMUK = null;
		
		/* Loop through the MappedUniqueKeys looking for one that has the same columns
		 * in the same order (i.e., Java List equality). */
		
		Iterator<MappedUniqueKey> lclMUKI = argMappedClass.getMappedUniqueKeys().iterator();
		while (lclMUKI.hasNext()) {
			MappedUniqueKey lclMUK = lclMUKI.next();
			if (lclMUK.isPrimaryKey()) {
				if (argMappedClass.isView()) {
					lclPMUK = lclMUK; // FIXME: This ignores the possibility that two are marked as primary keys
					break;
				} else {
					throw new IllegalStateException(argMappedClass + " has a MappedUniqueKey marked as its primary key before it should.");
				}
			}
			if (lclPK.getColumnNames().equals(lclMUK.getIndex().getColumnNames())) {
				if (lclPMUK != null) {
					lclMUKI.remove(); // TODO: This should probably be sorted out earlier
				} else {
					lclPMUK = lclMUK;
				}
			}
		}
		
		/* Did we find a MappedUniqueKey that matched the PrimaryKey? */
		
		if (lclPMUK != null) {
			/* Yes, we did.  Mark it as such. */
			lclPMUK.setPrimaryKey(true);
		} else {
			/* It's possible that we don't find a matching MappedUniqueKey.  In Oracle 8.1 if you have
			 * a table with no primary key and then ALTER TABLE xxx ADD xxx (PRIMARY KEY (xxx)) on a column
			 * that already has a non-unique index, then no unique index will be created (though the table
			 * will have a primary key.)  If this happens we just add the primary key to the list of 
			 * MappedUniqueKeys (this exceptional behavior that was not considered when this method
			 * was first written.  It was only discovered at Genentech in February 2004.) */
			
			argMappedClass.addMappedUniqueKey(
				new MappedUniqueKey(
					argMappedClass,
					lclPK
				)
			);
			System.out.println("Primary key " + lclPK + " does not have a corresponding unique index on " + argMappedClass + ".  Single-valued query methods will be created anyway."); // FIXME
		}
	}
	
	protected abstract String getOpalFactoryFactoryClassName();
	
	protected void createSpecificOpalFactoryFactory(Collection<MappedClass> argMappedClasses, String argPackageRoot, String argSourceDirectory) throws IOException {
		String lclPackage = argPackageRoot + ".persistence." + getDatabasePackageSuffix(); // TODO
		
		String lclOpalClassFileName = StringUtility.makeFilename(argSourceDirectory, lclPackage, getOpalFactoryFactoryClassName());
		
		File lclOpalClassFile = new File(lclOpalClassFileName);
		
		try (PrintWriter lclBW = new PrintWriter(new BufferedWriter(new FileWriter(lclOpalClassFile)))) {
			lclBW.println("package " + lclPackage + ';');
			lclBW.println();
			lclBW.println();
			
			SortedSet<String> lclFactoryImports = new TreeSet<>();
			for (MappedClass lclMC : argMappedClasses) {
				lclFactoryImports.add("import " + lclMC.getOpalFactoryPackageName() + ".OpalFactoryFactory;");
				lclFactoryImports.add("import " + lclMC.getFullyQualifiedOpalFactoryInterfaceName() + ';');
			}
			for (String lclImport : lclFactoryImports) {
				lclBW.println(lclImport);
			}
			// lclBW.println("import " + argPackageRoot + ".persistence.*;"); // TODO
			
			lclBW.println();
			
			lclBW.println("public class " + getOpalFactoryFactoryClassName() + " extends OpalFactoryFactory {");
			lclBW.println("\tprivate static final " + getOpalFactoryFactoryClassName() + " ourInstance = new " + getOpalFactoryFactoryClassName() + "();");
			lclBW.println();
			lclBW.println("\tpublic static final " + getOpalFactoryFactoryClassName() + " getSpecificInstance() { return ourInstance; }");
			lclBW.println();
			
			for (MappedClass lclMC : argMappedClasses) {
				Validate.notNull(lclMC);
				
				String lclSpecificOpalFactoryClassName = getDatabaseOpalPrefix() + lclMC.getOpalFactoryInterfaceName();
	//			String lclSpecificOpalFactoryPackageName = lclMC.getOpalFactoryPackageName() + getDatabasePackageSuffix();
				
				lclBW.println("\t@Override");
				if (lclMC.isDeprecated()) {
					lclBW.println("\t@Deprecated");
				}
				lclBW.println("\tpublic " + lclMC.getOpalFactoryInterfaceName() + " get" + lclMC.getOpalFactoryInterfaceName() + "() {");
				lclBW.println("\t\treturn " + lclSpecificOpalFactoryClassName + ".getInstance();");
				lclBW.println("\t}");
				lclBW.println();
			}
			
			lclBW.println("}");
		} // autoclose lclBW
	}
	
	public abstract TableName createTableName(Element argElement);
	
	public abstract Class<?> determineJavaType(DatabaseColumn argDatabaseColumn);
	
	public abstract boolean isLargeDatabaseType(DatabaseColumn argDatabaseColumn);
	
	protected abstract void determineTablesAndViews(Map<TableName, MappedClass> argMCs) throws SQLException;
	
	protected void checkTablesAndViews(Collection<MappedClass> argMCs) {
		for (MappedClass lclMC : argMCs) {
			if (lclMC.getTrueEntityType() == EntityType.Table && lclMC.getView() == Trinary.TRUE) {
				lclMC.complain(MessageLevel.Warning, "Entity " + lclMC + " is backed by a database Table but is explicitly configured as a View.");
			} else if (lclMC.getTrueEntityType() == EntityType.View && lclMC.getView() == Trinary.FALSE) {
				lclMC.complain(MessageLevel.Warning, "Entity " + lclMC + " is backed by a database View but is explicitly congfigured as a Table.");
			}
		}
	}
	
	protected void inferForeignKeysForViews(Map<TableName, MappedClass> argMCs, MappedClass argMC) {
		if (argMC.isView() == false) {
			return;
		}
		/* FIXME Allow this to be manually disabled */
		for (MappedClass lclTargetMC : argMCs.values()) {
			if (argMC == lclTargetMC) {
				continue;
			}
			if (lclTargetMC.isView()) {
				continue;
			}
			
			// CHECK: Are there MappedUniqueKeys yet?
			MappedUniqueKey lclPK = lclTargetMC.getPrimaryKey();
			if (lclPK == null) {
//				System.out.println("### It has a null primary key ###");
				continue;
			}
			/* FIXME: Make this work with composite foreign keys. */
			if (lclPK.getClassMembers().size() != 1) {
				continue;
			}
			ClassMember lclTargetCM = lclPK.getClassMembers().get(0);
			DatabaseColumn lclTargetDC = lclTargetCM.getDatabaseColumn();
			
			Iterator<ClassMember> lclCMI = argMC.createClassMemberIterator();
			while (lclCMI.hasNext()) {
				ClassMember lclCM = lclCMI.next();
				DatabaseColumn lclDC = lclCM.getDatabaseColumn();
				
				if (lclDC.getDataType().equalsIgnoreCase(lclTargetDC.getDataType())) { // Check raw type
					if (lclDC.getDomainName().equalsIgnoreCase(lclTargetDC.getDomainName())) { // Check user-defined type
						// CHECK: Should we also infer based on the database column name?
						// CHECK: Should this be case-insensitive?
						
						String lclSourceMemberName = lclCM.getBaseMemberName().toLowerCase();
						String lclTargetMemberName = lclTargetCM.getBaseMemberName().toLowerCase();
						String lclTargetTypeName = lclTargetMC.getTypeName().toLowerCase();
						
						if (lclSourceMemberName.endsWith(lclTargetTypeName + lclTargetMemberName)) {
							// CHECK: Table name?  Type name?
							// CHECK: Case-sensitive?
							// CHECK: What if they've already specified this reference using a <Reference> tag in the configuration file?  Does this crash and burn?
							
							Key lclSource = new Key(argMC.getTableName(), "INFERRED_KEY", lclCM.isNullAllowed() == false);  // FIXME: Name this something better
							lclSource.getColumnNames().add(lclCM.getDatabaseColumn().getName());
							Key lclTarget = new Key(lclTargetMC.getTableName(), "INFERRED_KEY", lclTargetCM.isNullAllowed() == false);  // FIXME: Name this something better
							lclTarget.getColumnNames().add(lclTargetCM.getDatabaseColumn().getName());
							ForeignKey lclFK;
							addArtificialForeignKey(
								lclFK = new ForeignKey(
									lclSource,
									lclTarget,
									"UNNAMED_INFERRED_KEY", // FIXME: Name this something better
									ReferentialAction.NO_ACTION, // Delete Action (THINK: What does this even mean for inferred foreign keys?)
									ReferentialAction.NO_ACTION // Update Action (THINK: What does this even mean for inferred foreign keys?)
								)
							);
							
							lclFK.setCollectionClass(null);
							argMC.complain(MessageLevel.Info, "Adding an inferred foreign key from " + argMC + " to " + lclTargetMC + ".");
						}
					}
				}
			}
		}
	}
	
	protected void eliminateUnwantedDefaultBackCollections(Map<TableName, MappedClass> argMCs) {
		
		List<ForeignKey> lclFKs = getForeignKeys();
		
		/* If the foreign key is to an entity (table/view) that is mapped to disallow back collections by default, set the CollectionClass
		 * to null.  This can be overridden later when we process the artificial foreign keys (arising from the <Reference> elements
		 * in the configuration file).
		 */
		for (ForeignKey lclFK : lclFKs) {
			MappedClass lclTarget = argMCs.get(lclFK.getTargetKey().getTableName());
			if (lclTarget != null) {
				if (lclTarget.hasBackCollectionsByDefault(true) == false) {
					lclTarget.complain(MessageLevel.Info, "Back collection for the foreign key from " + lclFK.getSourceKey().getTableName() + " to " + lclFK.getTargetKey().getTableName() + " has been disabled since the target doesn't have them by default.  (This is not necessarily final.)");
					lclFK.setCollectionClass(null);
				}
			}
		}
	}
	
	protected void determineSourceColumnsForArtificialForeignKeys(/* Map<TableName, MappedClass> argMCs */) {
		
		List<ForeignKey> lclFKs = getForeignKeys();
		
		/* Find artificial foreign keys that were created by <Reference> elements that did not specify columns as part of the
		 * <Source> element.  If only one foreign key exists in the database between those two tables, assume that we are
		 * trying to modify that one and copy its source columns into the key list for the artificial foreign key.  This will
		 * then immediately be used to re-match the artificial foreign key with the real one.  This is inefficient. 
		 */
		List<ForeignKey> lclPossibleCandidates = new ArrayList<>();
		Iterator<ForeignKey> lclI = getArtificialForeignKeys().iterator();
		while (lclI.hasNext()) {
			ForeignKey lclAFK = lclI.next();
			if (lclAFK.getSourceKey().getColumnNames().isEmpty()) {
				lclPossibleCandidates.clear();
				for (ForeignKey lclFK : lclFKs) {
					if (lclFK.getSourceKey().getTableName().equals(lclAFK.getSourceKey().getTableName())) {
						if (lclFK.getTargetKey().getTableName().equals(lclAFK.getTargetKey().getTableName())) {
							lclPossibleCandidates.add(lclFK);
						}
					}
				}
				if (lclPossibleCandidates.size() == 1) {
					ForeignKey lclPC = lclPossibleCandidates.get(0);
	
					if (lclPC.getSourceKey().getColumnNames().size() != lclAFK.getTargetKey().getColumnNames().size()) {
						throw new IllegalStateException("*** <Reference> element linking " + lclAFK.getSourceKey().getTableName() + " to " + lclAFK.getTargetKey().getTableName() + " did not specify source columns, but the only database foreign key to which it can be matched has the wrong number of target columns ***");
					}
					
					lclAFK.getSourceKey().getColumnNames().addAll(lclPC.getSourceKey().getColumnNames());
				} else if (lclPossibleCandidates.isEmpty()) {
					System.out.println("*** <Reference> element linking " + lclAFK.getSourceKey().getTableName() + " to " + lclAFK.getTargetKey().getTableName() + " did not specify source columns, but no database foreign key exists with which it can be matched ***"); // FIXME
				} else if (lclPossibleCandidates.size() > 1) {
					System.out.println("*** <Reference> element linking " + lclAFK.getSourceKey().getTableName() + " to " + lclAFK.getTargetKey().getTableName() + " did not specify source columns, but multiple database foreign keys exist with which it can be matched ***"); // FIXME
				}
			}
		}
	}
	
	protected void processMappedClasses(Map<TableName, MappedClass> argMCs /*, boolean argSampleCollections */) throws SQLException {
		determineTablesAndViews(argMCs);
		
		checkTablesAndViews(argMCs.values());
		
		eliminateUnwantedDefaultBackCollections(argMCs);
		
		determineSourceColumnsForArtificialForeignKeys(/* argMCs */);
		
		for (MappedClass lclMC : argMCs.values()) {
			Validate.notNull(lclMC);
			processMappedClassFirstPass(argMCs, lclMC);
		}
		
		for (MappedClass lclMC : argMCs.values()) {
			Validate.notNull(lclMC);
			processMappedClassSecondPass(argMCs, lclMC /*, argSampleCollections */);
		}
		
		// THINK: Can the following two loops be combined?
		for (MappedClass lclMC : argMCs.values()) {
			Validate.notNull(lclMC);
			processMappedClassThirdPass(argMCs, lclMC);
		}
		
		for (MappedClass lclMC : argMCs.values()) {
			Validate.notNull(lclMC);
			lclMC.validateAndResolveInheritance();
		}
	}
	
	public void generateClasses(OpalParseContext argOPC) throws IOException, SQLException {
		Validate.notNull(argOPC);
		
		Map<TableName, MappedClass> lclMappedClasses = argOPC.getMappedClasses();
		
		processMappedClasses(lclMappedClasses /*, false */); // FIXME: Should actually use SampleCollections attribute
		
		/* Create the classes that are independent of the actual database. */
		for (MappedClass lclMC : lclMappedClasses.values()) {
			ClassGenerator lclCG = new ClassGenerator(lclMC);
			lclCG.createClasses();
		}
		
		createFactoryMap(lclMappedClasses.values(), argOPC.getDefaultPackage(), argOPC.getSourceDirectory());
		
		createOpalFactoryFactory(lclMappedClasses.values(), getSpecificOpalFactoryFactoryClassName(argOPC), argOPC.getDefaultPackage(), argOPC.getSourceDirectory());
		
		for (MappedClass lclMC : lclMappedClasses.values()) {
			createSpecificOpalFactory(argOPC, lclMC);
		}
		
		createSpecificOpalFactoryFactory(lclMappedClasses.values(), argOPC.getDefaultPackage(), argOPC.getSourceDirectory());
	}
	
	protected ArrayList<DatabaseColumn> getColumns() {
		if (myColumns == null) {
			try {
				myColumns = loadColumns();
				loadCheckConstraints(); /* Will probably call getColumns(), which will work, but just barely */
			} catch (SQLException lclE) {
				throw new RuntimeException("Could not load columns", lclE);
			}
		}
		return myColumns;
	}
	
	public ArrayList<DatabaseColumn> getColumns(TableName argTableName) {
		ArrayList<DatabaseColumn> lclColumns = new ArrayList<>();
		
		Iterator<DatabaseColumn> lclI = getColumns().iterator();
		while (lclI.hasNext()) {
			DatabaseColumn lclDC = lclI.next();
			if (argTableName.equals(lclDC.getTableName())) {
				lclColumns.add(lclDC);
			}
		}
		
		return lclColumns;
	}
	
	public DataSource getDataSource() {
		return myDataSource;
	}
	
	protected ArrayList<ForeignKey> getArtificialForeignKeys() {
		return myArtificialForeignKeys;
	}
	
	protected ArrayList<ForeignKey> getForeignKeys() {
		if (myForeignKeys == null) {
			try {
				System.out.println("Loading foreign keys from the database.");
				myForeignKeys = loadForeignKeys();
			} catch (SQLException lclE) {
				throw new RuntimeException("Could not load foreign keys", lclE);
			}
		}
		return myForeignKeys;
	}
	
	public ArrayList<ForeignKey> getForeignKeysFrom(TableName argT) {
		Validate.notNull(argT);
		
		ArrayList<ForeignKey> lclAL = new ArrayList<>();
		
		for (ForeignKey lclFK : getForeignKeys()) {
			if (argT.equals(lclFK.getSourceKey().getTableName())) {
				lclAL.add(lclFK);
			}
		}
		
		return lclAL;
	}
	
	public ArrayList<ForeignKey> getForeignKeysTo(TableName argT) {
		Validate.notNull(argT);
		
		ArrayList<ForeignKey> lclAL = new ArrayList<>();
		
		for (ForeignKey lclFK : getForeignKeys()) {
			if (argT.equals(lclFK.getTargetKey().getTableName())) {
				lclAL.add(lclFK);
			}
		}
		
		return lclAL;
	}
	
	protected ArrayList<Index> getIndexes() {
		if (myIndexes == null) {
			try {
				myIndexes = loadIndexes();
			} catch (SQLException lclE) {
				throw new RuntimeException("Could not load indexes from the database.", lclE);
			}
		}
		return myIndexes;
	}
	
	public PrimaryKey getPrimaryKey(TableName argT) {
		Validate.notNull(argT);
		Iterator<PrimaryKey> lclI = getPrimaryKeys().iterator();
		while (lclI.hasNext()) {
			PrimaryKey lclPK = lclI.next();
			TableName lclPKTN = lclPK.getTableName();
			if (argT.equals(lclPKTN)) {
				return lclPK;
			}
		}
		return null;
	}
	
	protected ArrayList<PrimaryKey> getPrimaryKeys() {
		if (myPrimaryKeys == null) {
			try {
				myPrimaryKeys = loadPrimaryKeys();
			} catch (SQLException lclE) {
				throw new RuntimeException("Could not load primary keys", lclE);
			}
		}
		return myPrimaryKeys;
	}
	
	public Collection<Index> getUniqueIndexes(TableName argTableName) {
		Validate.notNull(argTableName);
		
		ArrayList<Index> lclResult = new ArrayList<>();
		
		for(Index lclIndex : getIndexes()) {
			if (lclIndex.isUnique() && argTableName.equals(lclIndex.getTableName())) {
				lclResult.add(lclIndex);
			}
		}
		
		return lclResult;
	}
	
	public abstract void initialize(Element argElement);
	
	protected abstract ArrayList<DatabaseColumn> loadColumns() throws SQLException;
	
	protected abstract void loadCheckConstraints() throws SQLException;
	
	protected abstract ArrayList<ForeignKey> loadForeignKeys() throws SQLException;
	
	protected abstract ArrayList<Index> loadIndexes() throws SQLException;
	
	protected abstract ArrayList<PrimaryKey> loadPrimaryKeys() throws SQLException;
	
	protected void processMappedClassFirstPass(@SuppressWarnings("unused") Map<TableName, MappedClass> argMappedClasses, MappedClass argMappedClass) {
		argMappedClass.complain(MessageLevel.Debug, "Mapping " + argMappedClass.getTableName() + " (first pass)");
		
		createMappedUniqueKeys(argMappedClass);
		
		createPrimaryKeys(argMappedClass);
	}
	
	protected void processMappedClassSecondPass(Map<TableName, MappedClass> argMappedClasses, MappedClass argMappedClass /*, boolean argSampleCollections */) /* throws SQLException */ {
		argMappedClass.complain(MessageLevel.Debug, "Mapping " + argMappedClass.getTableName() + " (second pass)");
		
		inferForeignKeysForViews(argMappedClasses, argMappedClass);
		
		createForeignKeys(argMappedClasses, argMappedClass /*, argSampleCollections */);
		
		argMappedClass.dropUnmappedClassMembers();
		
		argMappedClass.determineAssociationStatus();
		
		argMappedClass.determineUniqueForeignKeys();
		
		argMappedClass.determineTree();
		
		processMappedClassInternal(argMappedClasses, argMappedClass);
	}
	
	protected void processMappedClassThirdPass(@SuppressWarnings("unused") Map<TableName, MappedClass> argMappedClasses, MappedClass argMappedClass) {
		argMappedClass.complain(MessageLevel.Debug, "Mapping " + argMappedClass.getTableName() + " (third pass)");
		
		argMappedClass.determineOneToOneForeignKeys();
		
		argMappedClass.determinePolymorphism();
		
		argMappedClass.handleComputedClassMembers();
	}
	
	@SuppressWarnings("unused")
	protected void processMappedClassInternal(Map<TableName, MappedClass> argMappedClasses, MappedClass argMappedClass) {
		return;
	}
	
	public void addArtificialPrimaryKey(PrimaryKey argPK) {
		getPrimaryKeys().add(argPK);
	}
	
	public void addArtificialForeignKey(ForeignKey argFK) {
		getArtificialForeignKeys().add(argFK);
	}
	
	protected String getSpecificOpalFactoryFactoryClassName(OpalParseContext argOPC) {
		return argOPC.getDefaultPackage() + '.' + argOPC.getPersistenceSubpackage() + '.' + getDatabasePackageSuffix() + '.' + getOpalFactoryFactoryClassName();
	}
	
	public String getDataSourceName() {
		return myDataSourceName;
	}
	
	public void setDataSourceName(String argString) {
		myDataSourceName = argString;
	}
	
	public Map<String, String> getUserTypeMappings() {
		return myUserTypeMappings;
	}
	
	protected void createGeneratedKeysMethods(PrintWriter argPW, MappedClass argMC) {
		if (argMC.isCreatable() && argMC.hasDatabaseGeneratedColumns()) {
			argPW.println("\t@Override");
			argPW.println("\tpublic boolean hasGeneratedKeys() {");
			argPW.println("\t\treturn true;");
			argPW.println("\t}");
			argPW.println();
			
			argPW.println("\t@Override");
			argPW.println("\tprotected void processGeneratedKeys(" + ResultSet.class.getName() + " argRS, " + argMC.getOpalClassName() + " argOpal) {");
			argPW.println("\t\ttry {");
			Iterator<ClassMember> lclCMI = argMC.createClassMemberIterator();
			while (lclCMI.hasNext()) {
				ClassMember lclCM = lclCMI.next();
				if (lclCM.getDatabaseColumn().hasDatabaseGeneratedNumber()) { // Is this right?
					generateGeneratedKeysMethodInternal(argPW, lclCM);
				}
			}
			argPW.println("\t\t} catch (SQLException lclE) {");
			argPW.println("\t\t\tthrow new PersistenceException(\"Could not process generated keys.\");");
			argPW.println("\t\t}");
			argPW.println("\t}");
			argPW.println();
		}
	}
	
	protected void createComplicatedDefaultMethods(PrintWriter argPW, MappedClass argMC) {
		if (argMC.isCreatable() && argMC.hasComplicatedDefaults()) {
			argPW.println("\t@Override");
			argPW.println("\tpublic boolean hasComplicatedDefaults() {");
			argPW.println("\t\treturn true;");
			argPW.println("\t}");
			argPW.println();
			
			{
				argPW.println("\t@Override");
				argPW.println("\tpublic String[] getComplicatedDefaultColumns() {");
				argPW.print("\t\treturn new String[] {");
				Iterator<ClassMember> lclCMI = argMC.createClassMemberIterator();
				while (lclCMI.hasNext()) {
					ClassMember lclCM = lclCMI.next();
					if (lclCM.getDatabaseColumn().hasComplicatedDefault()) { // Is this right?
						argPW.print("\"");
						argPW.print(lclCM.getDatabaseColumn().getName());
						argPW.print("\", ");
					}
				}
				argPW.println("};");
				argPW.println("\t}");
				argPW.println();
			}
			
			{
				argPW.println("\t@Override");
				argPW.println("\tprotected void processGeneratedKeys(" + ResultSet.class.getName() + " argRS, " + argMC.getOpalClassName() + " argOpal) {");
				argPW.println("\t\ttry {");
				Iterator<ClassMember> lclCMI = argMC.createClassMemberIterator();
				while (lclCMI.hasNext()) {
					ClassMember lclCM = lclCMI.next();
					if (lclCM.getDatabaseColumn().hasComplicatedDefault()) { // Is this right?
						generateGeneratedKeysMethodInternal(argPW, lclCM);
					}
				}
				argPW.println("\t\t} catch (SQLException lclE) {");
				argPW.println("\t\t\tthrow new PersistenceException(\"Could not process generated keys.\");");
				argPW.println("\t\t}");
				argPW.println("\t}");
				argPW.println();
			}
		}
	}
	
	protected void generateGeneratedKeysMethodInternal(PrintWriter argPW, ClassMember argCM) {
		argPW.println("\t\t\targOpal." + argCM.getObjectMutatorName() + "(argRS.getInt(\"" + argCM.getDatabaseColumn().getName() + "\"));");
	}
	
}
