package com.opal.creator.database.oracle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;

import org.w3c.dom.Element;

import com.siliconage.database.DatabaseUtility;
import com.siliconage.util.UnimplementedOperationException;
import com.siliconage.xml.XMLElement;

import com.opal.creator.ClassMember;
import com.opal.creator.MappedClass;
import com.opal.creator.MappedForeignKey;
import com.opal.creator.MappedUniqueKey;
import com.opal.creator.database.ReferentialAction;
import com.opal.creator.database.ConstantDefaultValue;
import com.opal.creator.database.CurrentTimestampDefaultValue;
import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.DefaultValue;
import com.opal.creator.database.EntityType;
import com.opal.creator.database.ForeignKey;
import com.opal.creator.database.Index;
import com.opal.creator.database.Key;
import com.opal.creator.database.PrimaryKey;
import com.opal.creator.database.RelationalDatabaseAdapter;
import com.opal.creator.database.TableName;

public class OracleAdapter extends RelationalDatabaseAdapter {
	private ArrayList<Sequence> mySequences;
	
//	private static final String DATABASE_DRIVER_CLASS_NAME = "oracle.jdbc.driver.OracleDriver";
//
	private static final String DATABASE_OPAL_PREFIX = "Oracle";
	
	private static final String DATABASE_PACKAGE_SUFFIX = "oracle";
	
	public OracleAdapter(DataSource argDataSource) {
		super(argDataSource);
	}
	
//	@Override
//	protected String getDatabaseDriverClassName() {
//		return DATABASE_DRIVER_CLASS_NAME;
//	}
	
	protected DefaultValue createDefaultValue(String argDefaultString) {
		if (argDefaultString == null) {
			return null;
		}
		
		String lclDefaultString = argDefaultString.trim();
		
		if ("".equals(lclDefaultString)) {
			return null;
		}
		
//		ourLogger.debug("Check default value [" + argDefaultString + "]");
		
		/* All valid Oracle default strings appear to begin with parentheses. */
		
		if (lclDefaultString.charAt(0) == '(') {
			/* Remove the parentheses.  Note that we don't actually make sure that the right parenthesis is there. */
			lclDefaultString = lclDefaultString.substring(1, lclDefaultString.length() - 1);
		}
		
		if (lclDefaultString.startsWith("CREATE DEFAULT")) {
			int lclAsPosition = lclDefaultString.indexOf(" AS ");
			if (lclAsPosition != -1) {
				return createDefaultValue(lclDefaultString.substring(lclAsPosition+4));
			} else {
				System.out.println("Unable to process default \"" + argDefaultString + "\"");
				return null;
			}
		}
		
		if (lclDefaultString.charAt(0) == '\'') {
			return new ConstantDefaultValue(lclDefaultString.substring(1, lclDefaultString.length() - 1));
		}
		
		if (lclDefaultString.equalsIgnoreCase("null")) {
			return null;
		}
		
		if (lclDefaultString.equalsIgnoreCase("SYSDATE")) {
			return CurrentTimestampDefaultValue.getInstance();
		}
		
		/* TODO: Handle the special USER default (and figure out how to apply this to SQL Server
		 * as well. */
		
		// TODO: What if we really want the String "2"?
		
		try {
			int lclI = Integer.parseInt(lclDefaultString);
			return new ConstantDefaultValue(Integer.valueOf(lclI));
		} catch (NumberFormatException lclE) {
			/* Okay, not an int */
		}
		
		try {
			long lclL = Long.parseLong(lclDefaultString);
			return new ConstantDefaultValue(Long.valueOf(lclL));
		} catch (NumberFormatException lclE) {
			/* Okay, not a long */
		}
		
		try {
			double lclD = Double.parseDouble(lclDefaultString);
			return new ConstantDefaultValue(Double.valueOf(lclD));
		} catch (NumberFormatException lclE) {
			/* Okay, not a double */
		}
		
		/* TODO: Convert this to a string */
		
		System.out.println("Unable to process default \"" + argDefaultString + "\"");
		return null;
	}
	
	protected void createSequencing(MappedClass argMappedClass) {
		/* Do autosequencing */
		/* TODO:  Put in flag for turning this on and off */
		OracleTableName lclOTN = (OracleTableName) argMappedClass.getTableName();
		String lclSchemaName = lclOTN.getSchemaName();
		
		/* Get the primary key */
		MappedUniqueKey lclMUK = argMappedClass.getPrimaryKey();
		
		/* No primary key means no use of sequences */
		if (lclMUK == null) {
			return;
		}
		
		Iterator<ClassMember> lclI = lclMUK.createClassMemberIterator();
		
		PrimaryKeyClassMemberLoop:
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
		
			/* If it's not a numeric column, don't both trying to find a sequence for it. */
			
			if (!Number.class.isAssignableFrom(lclCM.getMemberType())) {
				continue PrimaryKeyClassMemberLoop;
			}
			
			/* If the lclCM class member (which is part of the primary key) is *also* part of one or more
			foreign keys, then we should not use the sequence for that column.  This is because the table
			is a many-to-many table or subclass table which should inherit its value for the sequence
			from the referenced table. */
			
			Iterator<MappedForeignKey> lclFKI = argMappedClass.createForeignKeyIterator();
			while (lclFKI.hasNext()) {
				MappedForeignKey lclFK = lclFKI.next();
				Iterator<ClassMember> lclFKCMI = lclFK.getSource().iterator();
				while (lclFKCMI.hasNext()) {
					if (lclCM.equals(lclFKCMI.next())) {
						System.out.println("Column is part of a foreign key; skipping sequence.");
						continue PrimaryKeyClassMemberLoop;
					}
				}
			}
			
			/* We are eligible for a sequence. */
			
			Collection<Sequence> lclSequences = getSequences();
			
			Iterator<Sequence> lclJ = lclSequences.iterator();
			SequenceLoop:
			while (lclJ.hasNext()) {
				Sequence lclSequence = lclJ.next();
				
				/* Is it from the right schema? */
				
				if (!lclSchemaName.equals(lclSequence.getOwner())) {
					continue;
				}
				
				String lclSequenceName = lclSequence.getName();
				
				/* Does it end in "_SQ"? */
				
				int lclSuffix = lclSequenceName.indexOf("_SQ");
				
				if (lclSuffix < 0) {
					continue SequenceLoop;
				}
				
				/* What column name is it for? */
				
				String lclColumnName = lclSequenceName.substring(0, lclSuffix);
				
				if (lclColumnName.equals(lclCM.getDatabaseColumn().getName())) {						
					System.out.println("Using sequence " + lclSequenceName + " for " + lclCM.getDatabaseColumn().getName() + " of " + argMappedClass);
					lclCM.getDatabaseColumn().setSequenced(true);
				}
			}
		}
	}
	
	@Override
	protected String getDatabaseOpalPrefix() {
		return DATABASE_OPAL_PREFIX;
	}
	
	@Override
	protected String getDatabasePackageSuffix() {
		return DATABASE_PACKAGE_SUFFIX;
	}
	
	/* The following methods are used to create database-specific functionality for the
	 * database-specific factories for specific opals (e.g. OracleWidgetFactory).  They are
	 * invoked by the createSpecificOpalFactory method in RelationalDatabaseAdapter. */
	
	@Override
	protected void createAfterInsertMethod(PrintWriter argBW, MappedClass argMC) {
		
		/* Since Oracle uses sequences that Opal applies on object creation, there is nothing
		 * to do immediately after an insert.  Other databases (e.g., SQL Server) would need to
		 * run a SELECT here to get the identity of the newly inserted row so that the
		 * object could be updated. */
		
		return;
	}
	
	/* Returns true if any ClassMember of MappedClass argMC has a default value.  Essentially
	 * this is used to determine whether or not an applyDefaults method is generated
	 * (and called) when newObject(...) is called.
	 */
	protected boolean hasMemberWithDefault(MappedClass argMC) {
		Iterator<ClassMember> lclI = argMC.createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (lclCM.getDatabaseColumn().getDefault() != null) {
				return true;
			}
		}
		return false;
	}
	
	/* Returns true if any ClassMember of MappedClass argMC has a sequence.  Essentially
	 * this is used to determine whether or not an applySequences method is generated
	 * (and called) when newObject(...) is called.
	 */
	protected boolean hasMemberWithSequence(MappedClass argMC) {
		Iterator<ClassMember> lclI = argMC.createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (lclCM.getDatabaseColumn().isSequenced()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected void createNewObjectMethod(PrintWriter argBW, MappedClass argMC) {
		argBW.println("\tprotected void newObject(Opal argOpal) {");
//		argBW.println("\t\t" + argMC.getOpalClassName() + " lclOpal = (" + argMC.getOpalClassName() + ") argOpal;");
		
		if (hasMemberWithDefault(argMC)) {
			argBW.println("\t\tapplyDefaults((" + argMC.getOpalClassName() + ") argOpal);");
		}
		if (hasMemberWithSequence(argMC)) {
		argBW.println("\t\ttry {");
			argBW.println("\t\t\tapplySequences((" + argMC.getOpalClassName() + ") argOpal);");
		argBW.println("\t\t} catch (SQLException lclE) {");
		argBW.println("\t\t\tthrow new com.siliconage.util.RuntimeEncapsulatingException(\"Could not get sequence values\", lclE);");
		argBW.println("\t\t}");
		}
		argBW.println("\t\treturn;");
		argBW.println("\t}");
		argBW.println();
		
		/* applyDefaults() */
		
		/* TODO: We should actually assign defaults */
		
		if (hasMemberWithDefault(argMC) ) {
		argBW.println("\tprotected void applyDefaults(" + argMC.getOpalClassName() + " argOpal) {");
		argBW.println("\t\treturn;");
		argBW.println("\t}");
		argBW.println();
		}
		
		/* applySequences() */
		
		/* TODO:  Maybe delay this until we know that the ID is needed? */
		/* TODO:  Work with Objects, not primitives */
		
			if (hasMemberWithSequence(argMC)) {
			argBW.println("\tprotected void applySequences(" + argMC.getOpalClassName() + " argOpal) throws SQLException {");
			Iterator<ClassMember> lclI = argMC.createClassMemberIterator();
			while (lclI.hasNext()) {
				ClassMember lclCM = lclI.next();
				if (lclCM.getDatabaseColumn().isSequenced()) {
					argBW.println("\t\targOpal.getNewValues()[" + lclCM.getFieldIndex() + "] = new " + lclCM.getMemberType().getName() + "(com.siliconage.database.DatabaseUtility.getNextvalForSequence(getDataSource(), \"" + lclCM.getDatabaseColumn().getSequenceName() + "\"));");
				}
			}
			argBW.println("\t\treturn;");
			argBW.println("\t}");
			argBW.println();
		}
	}
	
	@Override
	protected void createGetFullyQualifiedTableNameMethod(PrintWriter argBW, MappedClass argMC) {
		argBW.println("\tprotected String getFullyQualifiedTableName() {");
		argBW.println("\t\treturn \"" + ((OracleTableName) argMC.getTableName()).getTableName() + "\";");
		argBW.println("\t}");
		argBW.println();
	}
	
	private static final String OPAL_FACTORY_FACTORY_CLASS_NAME = "OracleOpalFactoryFactory";
	
	@Override
	protected String getOpalFactoryFactoryClassName() {
		return OPAL_FACTORY_FACTORY_CLASS_NAME;
	}
	
	@Override
	public OracleTableName createTableName(Element argElement) {
		String lclTableName = XMLElement.getAttributeValue(argElement, "Table");
		String lclSchemaName = ObjectUtils.firstNonNull(XMLElement.getAttributeValue(argElement, "Schema"), getDefaultSchema());
		
		return new OracleTableName(
			lclSchemaName,
			lclTableName
		);
	}
	
	@Override
	public boolean isLargeDatabaseType(DatabaseColumn argDatabaseColumn) {
		Validate.notNull(argDatabaseColumn);
		return false;
	}
	
	@Override
	public Class<?> determineJavaType(DatabaseColumn argDatabaseColumn) {
		/* Other things to think about:  NVARCHAR2, NCHAR, NVARCHAR, UNDEFINED, SDO_DIM_ARRAY, SDO_GEOMETRY, ROWID */
		String lclS = argDatabaseColumn.getDataType();
		String lclName = argDatabaseColumn.getName().toUpperCase();
		
		/* TODO:  Some fields should be Booleans */
		
		if (lclS.equals("VARCHAR") || lclS.equals("VARCHAR2") || lclS.equals("CHAR")) {
			if (argDatabaseColumn.getLength() == 1) {
				return Character.class;
			} else {
				return String.class;
			}
		} else if (lclS.equals("DATE")) {
			return Timestamp.class;
		} else if (lclS.equals("NUMBER")) {
			if (argDatabaseColumn.getScale() > 0) {
				return Double.class; /* TODO:  Should some of these be Floats? */
			} else {
				if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
					return Boolean.class;
				}
				return argDatabaseColumn.getPrecision() > 9 ? Long.class : Integer.class;
			}
		} else if (lclS.equals("FLOAT")) {
			return Double.class;
		} else if (lclS.equals("CLOB") || lclS.equals("RAW") || lclS.equals("LONG") || lclS.equals("LONG RAW") || lclS.equals("NCLOB") || lclS.equals("BLOB")) {
			System.out.println("Unable to map data type " + lclS + "; big types are not currently supported.");
			return null;
		} else {
			System.out.println("Unable to map unknown type \"" + lclS  + "\"");
			return null;
		}
		
		/* Special types */
		
		/*
		if (myMemberType == String.class) {
			lclS = '_' + getDatabaseColumn().getName() + '_';
			if (lclS.indexOf("_EMAIL_") > -1) {
				myMemberType = com.opal.types.EmailAddress.class;
			} else if (lclS.indexOf("_IP_") > -1) {
				myMemberType = java.net.InetAddress.class;
			}

			if (myMemberType != String.class) {
				ourLogger.debug("Changing " + getDatabaseColumn().getName() + " to type " + myMemberType);
			}
		};
		*/
	}
	
	protected ArrayList<Sequence> getSequences() {
		if (mySequences == null) {
			try {
				mySequences = loadSequences();
			} catch (SQLException lclE) {
				throw new RuntimeException("Could not load sequences", lclE);
			}
		}
		return mySequences;
	}
	
	@Override
	public void initialize(Element argElement) {
		// setDefaultSchema(argElement.getAttributeValue("DefaultSchema"));
	}
	
	@Override
	protected ArrayList<DatabaseColumn> loadColumns() throws SQLException {
		String lclSQL = "SELECT owner, table_name, column_name, data_type, data_length, data_precision, data_scale, nullable, data_default FROM all_tab_columns WHERE owner = '" + getDefaultSchema() + "'";
		
		ArrayList<DatabaseColumn> lclColumns = new ArrayList<>();
		
		ResultSet lclRS = null;
		
		try {
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);
			
			while (lclRS.next()) {
				String lclColumnName = lclRS.getString("column_name");
				
				// ourLogger.debug("Column = " + lclColumnName);
				
				DefaultValue lclDefaultValue = createDefaultValue(lclRS.getString("data_default"));
				
				DatabaseColumn lclDC = new DatabaseColumn(
						new OracleTableName(
								lclRS.getString("owner"),
								lclRS.getString("table_name")
								),
						lclColumnName,
						lclRS.getString("data_type"),
						lclRS.getInt("data_length"),
						lclRS.getInt("data_precision"),
						lclRS.getInt("data_scale"),
						false, // FIXME: I'm not sure how to handle wide characters in modern versions of Oracle.
						lclRS.getString("data_type"), // Second use of data_type; I'm not sure whether this returns the raw data_type or the domain. 
						"Y".equals(lclRS.getString("nullable")),
						lclDefaultValue
						);
				
				lclColumns.add(lclDC);
			}
			
			return lclColumns;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	@Override
	protected void loadCheckConstraints() {
		// TODO:  Implement for Oracle
		return;
	}
	
	@Override
	protected ArrayList<ForeignKey> loadForeignKeys() throws SQLException {
		ResultSet lclRS = null;
		
		final ArrayList<Key> lclKeys = new ArrayList<>();
		
		try {
			String lclSQL = "SELECT ACC.owner, ACC.table_name, ACC.constraint_name, ACC.column_name, ATC.nullable " +
				"FROM all_cons_columns ACC " +
					"JOIN all_tab_columns ATC ON ACC.owner = ATC.owner AND ACC.table_name = ATC.table_name AND ACC.column_name = ATC.column_name " +
				"WHERE ACC.owner = ? " +
				"ORDER BY ACC.owner, ACC.constraint_name, ACC.position";
			
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL, new Object[] { getDefaultSchema() });
			
			Key lclKey = null;
			int lclRequired = 0;
			while (lclRS.next()) {
				String lclConstraintName = lclRS.getString("constraint_name");
				
				if (lclKey != null && lclKey.getName().equals(lclConstraintName) == false) {
					if (lclRequired == lclKey.getColumnNames().size()) {
						// THINK: Should this really be > 0?
						lclKey.setRequired(true);
					}
					
					lclRequired = 0;
				}
				
//				boolean lclNotNull = "N".equalsIgnoreCase(lclRS.getString("nullable"));
				if ((lclKey == null) || (!lclKey.getName().equals(lclConstraintName))) {
					lclKeys.add(
						lclKey = new Key(
							new OracleTableName(lclRS.getString("owner"), lclRS.getString("table_name")),
							lclConstraintName,
							false /* not required, for now */
						)
					);
				}
				lclKey.getColumnNames().add(lclRS.getString("column_name"));
			}
			
			// Check requiredness one last time for the last group
			if (lclKey != null && lclRequired == lclKey.getColumnNames().size()) {
				// THINK: Should this really be > 0?
				lclKey.setRequired(true);
			}
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
		
		final ArrayList<ForeignKey> lclFKs = new ArrayList<>();
		
		try {
			String lclSQL = "SELECT owner, table_name, constraint_name, r_owner, r_constraint_name, delete_rule FROM all_constraints WHERE constraint_type = 'R' AND owner = ?";
			
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL, new Object[] { getDefaultSchema() });
			
			while (lclRS.next()) {
				String lclSourceSchemaName = lclRS.getString("owner");
				String lclSourceConstraintName = lclRS.getString("constraint_name");
				String lclTargetSchemaName = lclRS.getString("r_owner");
				String lclTargetConstraintName = lclRS.getString("r_constraint_name");
				
				Key lclSourceKey = null;
				Key lclTargetKey = null;
				
				Iterator<Key> lclI = lclKeys.iterator();
				while (lclI.hasNext()) {
					Key lclKey = lclI.next();
					if (((OracleTableName) lclKey.getTableName()).getSchemaName().equals(lclSourceSchemaName) && lclKey.getName().equals(lclSourceConstraintName)) {
						lclSourceKey = lclKey;
					}
					if (((OracleTableName) lclKey.getTableName()).getSchemaName().equals(lclTargetSchemaName) && lclKey.getName().equals(lclTargetConstraintName)) {
						lclTargetKey = lclKey;
					}
				}
				
				Validate.notNull(lclSourceKey);
				Validate.notNull(lclTargetKey);
				
				ReferentialAction lclDeleteAction = determineDeleteAction(lclRS.getString("delete_rule"));
				ReferentialAction lclUpdateAction = determineUpdateAction(lclRS.getString("update_rule"));
				
				ForeignKey lclFK = new ForeignKey(
						lclSourceKey,
						lclTargetKey,
						lclRS.getString("constraint_name"),
						lclDeleteAction,
						lclUpdateAction
						);
				
				lclFKs.add(lclFK);
			}
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
		
		return lclFKs;
	}
	
	protected ReferentialAction determineDeleteAction(@SuppressWarnings("unused") String argS) {
		return ReferentialAction.NO_ACTION; // TODO: Fix!
	}

	protected ReferentialAction determineUpdateAction(@SuppressWarnings("unused") String argS) {
		return ReferentialAction.NO_ACTION; // TODO: Fix!
	}

	@Override
	protected ArrayList<Index> loadIndexes() throws SQLException {
		String lclSQL = "SELECT I.owner, I.table_name, I.index_name, I.uniqueness, C.column_name "
				+ "FROM all_indexes I, all_ind_columns C "
				+ "WHERE I.index_name = C.index_name AND I.table_name = C.table_name "
				+ "AND I.owner = '" + getDefaultSchema() + "' AND C.table_owner = I.owner "
//				+ "AND I.owner = 'INTA' "
//				+ "AND I.owner = ? AND I.table_name = ? "
//				+ "AND I.uniqueness = 'UNIQUE' "
				+ "ORDER BY I.owner, I.index_name, C.column_position ";
		
		ResultSet lclRS = null;
		
		ArrayList<Index> lclIndexes = new ArrayList<>();
		
		try {
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);

//			String lclLastIndexName = null;
			Index lclIndex = null;
			
			/* TODO:  The following code will break if there are indexes with the same name in different schemata */
			while (lclRS.next()) {
				String lclThisIndexName = lclRS.getString("index_name");
				if ((lclIndex == null) || !(lclIndex.getIndexName().equals(lclThisIndexName))) {
					lclIndexes.add(
						lclIndex = new Index(
							new OracleTableName(
								lclRS.getString("owner"),
								lclRS.getString("table_name")
							),
							lclThisIndexName,
							lclRS.getString("uniqueness").equals("UNIQUE"),
							null // FIXME: Oracle Opals don't yet support filtered indexes
						)
					);
//					ourLogger.debug("Created " + lclIndex);
				}
				String lclColumnName = lclRS.getString("column_name");
//				ourLogger.debug("...with column " + lclColumnName);
				lclIndex.getColumnNames().add(lclColumnName);
			}
			
			return lclIndexes;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	@Override
	protected ArrayList<PrimaryKey> loadPrimaryKeys() throws SQLException {
		ResultSet lclRS = null;
		try {
			String lclSQL =
				"SELECT AC.owner, AC.table_name, AC.constraint_name, C.column_name "
					+ "FROM all_constraints AC, all_cons_columns C "
					+ "WHERE C.owner = AC.owner AND C.table_name = AC.table_name AND C.constraint_name = AC.constraint_name "
					+ "AND AC.constraint_type='P' AND AC.owner = '" + getDefaultSchema() + "' "
					+ "ORDER BY AC.owner, AC.constraint_name, C.position ";
			
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);
			
			ArrayList<PrimaryKey> lclPKs = new ArrayList<>();
			
			PrimaryKey lclPK = null;
			while (lclRS.next()) {
				String lclPKName = lclRS.getString("constraint_name");
				if (lclPK == null || !(lclPK.getName().equals(lclPKName))) {
					OracleTableName lclOTN = new OracleTableName(lclRS.getString("owner"), lclRS.getString("table_name"));
					lclPKs.add(lclPK = new PrimaryKey(lclOTN, lclPKName));
				}
				lclPK.getColumnNames().add(lclRS.getString("column_name"));
//				ourLogger.debug("Found PK " + lclPK);
			}
			
			return lclPKs;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	protected ArrayList<Sequence> loadSequences() throws SQLException {
		if (mySequences != null) {
			throw new IllegalStateException("Cannot call loadSequences when mySequences is not null");
		}
		
		String lclSQL = "SELECT sequence_owner, sequence_name FROM all_sequences";
		
		ResultSet lclRS = null;
		
		ArrayList<Sequence> lclSequences = new ArrayList<>();
		
		try {
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);
			
			while (lclRS.next()) {
				String lclSequenceOwner = lclRS.getString("sequence_owner");
				String lclSequenceName = lclRS.getString("sequence_name");

//				ourLogger.debug("Trying sequence " + lclSequenceName);
				/* If the sequence doesn't end in "_sq", we won't use it */
				
				int lclPos = lclSequenceName.indexOf("_SQ");
				if (lclPos < 0) {
					continue;
				}
				
				lclSequences.add(new Sequence(lclSequenceOwner, lclSequenceName));
			}
			
			return lclSequences;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	@Override
	protected void processMappedClassInternal(Map<TableName, MappedClass> argMappedClasses, MappedClass argMappedClass) {
		createSequencing(argMappedClass);
		return;
	}
	
//	private static final String[] splitFullyQualifiedTableName(String argFQTN) {
//		String[] lclResult = new String[2];
//		if (argFQTN != null) {
//			int lclIndex = argFQTN.indexOf('.');
//
//			if (lclIndex > -1) {
//				lclResult[0] = argFQTN.substring(0, lclIndex);
//			}
//
//			lclResult[1] = argFQTN.substring(lclIndex+1);
//		}
//
//		return lclResult;
//	}
	
	@Override
	protected String generateForeignKeyStatisticsSQL(ForeignKey argFK) {
		throw new UnimplementedOperationException();
	}
	
	/* FIXME: Implement this correctly */
	@Override
	protected void determineTablesAndViews(Map<TableName, MappedClass> argMCs) throws SQLException {
		for (MappedClass lclMC : argMCs.values()) {
			lclMC.setTrueEntityType(EntityType.Table);
		}
	}

}

