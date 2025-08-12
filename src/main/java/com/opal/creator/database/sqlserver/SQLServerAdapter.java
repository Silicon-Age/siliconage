package com.opal.creator.database.sqlserver;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

import org.w3c.dom.Element;

import com.siliconage.database.DatabaseUtility;
import com.siliconage.xml.XMLElement;

import com.opal.creator.MappedClass;
import com.opal.creator.database.ReferentialAction;
import static com.opal.creator.database.ReferentialAction.*;
import com.opal.creator.database.ConstantDefaultValue;
import com.opal.creator.database.CurrentTimestampDefaultValue;
import com.opal.creator.database.CurrentUTCTimestampDefaultValue;
import com.opal.creator.database.DefaultValue;
import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.EntityType;
import com.opal.creator.database.ForeignKey;
import com.opal.creator.database.Index;
import com.opal.creator.database.Key;
import com.opal.creator.database.PrimaryKey;
import com.opal.creator.database.RelationalDatabaseAdapter;
import com.opal.creator.database.TableName;

public class SQLServerAdapter extends RelationalDatabaseAdapter {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(SQLServerAdapter.class);
	
	private static final int UNIQUE_FLAG = 2;
//	private static final int CLUSTERED = 16;
	private static final int ENFORCES_PRIMARY_KEY = 2048;
//	private static final int ENFORCES_UNIQUE_CONSTRAINT = 4096;
	
	private static final String DATABASE_OPAL_PREFIX = "SQLServer";
	private static final String DATABASE_PACKAGE_SUFFIX = "sqlserver";
	
	/* The column metadata contains this value in the CHARACTER_SET_NAME column to indicate columns whose type
	 * (e.g., nvarchar) can store wide characters.
	 */
	private static final String WIDE_CHARACTER_SET = "UNICODE";
	
	private String myDefaultDatabase;
	private String myDefaultOwner;

	private static final int MAXIMUM_DIGITS_GUARANTEED_TO_FIT_IN_AN_INT = 9; // A ten-digit number might be 3,000,000,000
	
	public SQLServerAdapter(DataSource argDataSource) {
		super(argDataSource);
	}
	
	@Override
	protected String getDatabaseOpalPrefix() {
		return DATABASE_OPAL_PREFIX;
	}
	
	@Override
	protected String getDatabasePackageSuffix() {
		return DATABASE_PACKAGE_SUFFIX;
	}
	
	protected DefaultValue createDefaultValue(String argDefaultString) {
		if (argDefaultString == null) {
			return null;
		}
		
		String lclDefaultString = argDefaultString.trim();
		
		if ("".equals(lclDefaultString)) {
			return null;
		}
		
		/* All valid SQL Server default strings appear to begin with parentheses. */
		
		while (lclDefaultString.charAt(0) == '(') {
			/* Remove the parentheses.  Note that we don't actually make sure that the right parenthesis is there. */
			if (lclDefaultString.charAt(lclDefaultString.length() - 1) != ')') {
				ourLogger.error("The value \"" + argDefaultString + "\" obtained from INFORMATION_SCHEMA.COLUMNS.COLUMN_DEFAULT began with a parenthesis, but did not end with one.");
				return null;
			}
			lclDefaultString = lclDefaultString.substring(1, lclDefaultString.length() - 1);
		}
		
		if (lclDefaultString.startsWith("CREATE DEFAULT")) {
			int lclAsPosition = lclDefaultString.indexOf(" AS ");
			if (lclAsPosition != -1) {
				return createDefaultValue(lclDefaultString.substring(lclAsPosition+4));
			} else {
				ourLogger.debug("Unable to process default \"" + argDefaultString + "\"");
			}
		}
		
		if (lclDefaultString.charAt(0) == '\'') {
			return new ConstantDefaultValue(lclDefaultString.substring(1, lclDefaultString.length() - 1));
		}
		
		if (lclDefaultString.equalsIgnoreCase("null")) {
			return null;
		}
		
		if (lclDefaultString.equalsIgnoreCase("getdate()")) {
			return CurrentTimestampDefaultValue.getInstance();
		}

		if (lclDefaultString.equalsIgnoreCase("getutcdate()")) {
			return CurrentUTCTimestampDefaultValue.getInstance();
		}

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
		
		ourLogger.debug("Unable to process default \"" + argDefaultString + "\"");
		return null;
	}
	
	/* The following three methods are called by RelationalDatabaseAdapter when it is creating
	 * the database-specific factory class for a specific Opal (e.g. SQLServerWidgetFactory) to
	 * do database-specific tasks. */
	
	@Override
	protected void createAfterInsertMethod(PrintWriter argBW, MappedClass argMC) {
		/* Nothing */
	}
	
	@Override
	protected void createNewObjectMethod(PrintWriter argBW, MappedClass argMC) {
		argBW.println("\tprotected void newObject(@SuppressWarnings(\"unused\") " + argMC.getOpalClassName() + " argOpal) {");
		argBW.println("\t\treturn;");
		argBW.println("\t}");
		argBW.println();		
	}
	
	@Override
	protected void createGetFullyQualifiedTableNameMethod(PrintWriter argBW, MappedClass argMC) {
		/* FIXME: Note that this doesn't actually return a FQTN */
		
		argBW.println("\t@Override");
		argBW.println("\tprotected String getFullyQualifiedTableName() {");
		argBW.println("\t\treturn \"" + ((SQLServerTableName) argMC.getTableName()).getTableName() + "\";");
		argBW.println("\t}");
		argBW.println();
	}
	
	private static final String OPAL_FACTORY_FACTORY_CLASS_NAME = "SQLServerOpalFactoryFactory";
	
	@Override
	protected String getOpalFactoryFactoryClassName() {
		return OPAL_FACTORY_FACTORY_CLASS_NAME;
	}
	
	@Override
	public SQLServerTableName createTableName(Element argElement) {
		Validate.notNull(argElement);
		
		String lclDatabaseName = XMLElement.getAttributeValue(argElement, "Database");
		if (lclDatabaseName == null) {
			lclDatabaseName = getDefaultDatabase();
		}
		
		String lclOwnerName = XMLElement.getAttributeValue(argElement, "Owner");
		if (lclOwnerName == null) {
			lclOwnerName = getDefaultOwner();
		}
		
		/* This seems like redundant logic vis-a-vis determining the entity name in Mapping */
		String lclTableName = XMLElement.getAttributeValue(argElement, "Entity");
		if (lclTableName == null) {
			lclTableName = XMLElement.getAttributeValue(argElement, "Table");
		}
		if (lclTableName == null) {
			lclTableName = XMLElement.getAttributeValue(argElement, "View");
		}
		Validate.notNull(lclTableName);
		
		return new SQLServerTableName(lclDatabaseName, lclOwnerName, lclTableName);
	}
	
	@Override
	public boolean isLargeDatabaseType(DatabaseColumn argDatabaseColumn) {
		assert argDatabaseColumn != null;
		
		final String lclS = argDatabaseColumn.getDataType();
		
		if (lclS.equals("varbinary") || lclS.equals("image") || lclS.equals("binary")) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public Class<?> determineJavaType(DatabaseColumn argDatabaseColumn) {
		Validate.notNull(argDatabaseColumn);
		/* Other things to think about:  NVARCHAR2, NCHAR, NVARCHAR, UNDEFINED, SDO_DIM_ARRAY, SDO_GEOMETRY, ROWID */
		final String lclName = argDatabaseColumn.getName().toUpperCase();
		
		if (argDatabaseColumn instanceof SQLServerColumn lclSScolumn) {
			final String lclDomainName = lclSScolumn.getDomainName();
			if (lclDomainName != null) {
				String lclJavaType = getUserTypeMappings().get(lclDomainName);
				if (lclJavaType != null) {
					try {
						Class<?> lclC = Class.forName(lclJavaType);
						return lclC;
					} catch (ClassNotFoundException lclE) {
						System.out.println("*** Could not convert user-specific type mapping of \"" + lclJavaType + "\" into an actual type using Class.forName() ***");
					}
				}
			}
		} else {
			// THINK: Should this be an exception?
		}
		
		final String lclS = argDatabaseColumn.getDataType();
//		final boolean lclN = argDatabaseColumn.isNullable();
		
		if (lclS.equals("varchar") || lclS.equals("nvarchar") || lclS.equals("nchar") || lclS.equals("char") || lclS.equals("text")) {
			if (argDatabaseColumn.getLength() == 1) {
				return Character.class;
			} else {
				return String.class;
			}
		} else if (lclS.equals("date")) { // The date type became available with SQL Server 2012.
			return java.time.LocalDate.class;
		} else if (lclS.equals("datetime") || lclS.equals("datetime2")) { // TODO: What about specified precision?
			return java.time.LocalDateTime.class;
		} else if (lclS.equals("smalldatetime")) {
			return java.time.LocalDateTime.class; // But, of course, many valid LocalDateTimes are illegal or will have precision lost when being converted back
		} else if (lclS.equals("datetimeoffset")) {
			return java.time.OffsetDateTime.class;
		} else if (lclS.equals("time")) { // TODO: How do we handle a specified precision?
			return java.time.LocalTime.class;
		} else if (lclS.equals("int")) {
			if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
				return /* lclX && lclN ? Trinary.class : */ Boolean.class;
			}
			return Integer.class;
		} else if (lclS.equals("bigint")) {
			if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
				return /* lclX && lclN ? Trinary.class : */ Boolean.class;
			}
			return Long.class;
		} else if (lclS.equals("smallint")) {
			if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
				return /* lclX && lclN ? Trinary.class : */ Boolean.class;
			}
			return Short.class;
		} else if (lclS.equals("tinyint")) {
			if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
				return /* lclX && lclN ? Trinary.class : */ Boolean.class;
			}
			return Short.class; /* Byte is signed! */
		} else if (lclS.equals("bit")) {
			return /* lclX && lclN ? Trinary.class : */ Boolean.class;
		} else if (lclS.equals("decimal") || lclS.equals("numeric")) {
			if (argDatabaseColumn.getScale() > 0) {
				return Double.class; /* TODO:  Should some of these be Floats?  Or BigDecimals? */
			} else {
				if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
					return /* lclX && lclN ? Trinary.class : */ Boolean.class;
				}
				if (argDatabaseColumn.getPrecision() > 18) { // FIXME: Magic number
					return BigInteger.class;
				} else if (argDatabaseColumn.getPrecision() > MAXIMUM_DIGITS_GUARANTEED_TO_FIT_IN_AN_INT) {
					return Long.class;
				} else if (argDatabaseColumn.getPrecision() > 4) { // FIXME: Magic number
					return Integer.class;
				} else if (argDatabaseColumn.getPrecision() > 2) { // FIXME: Magic number
					return Short.class;
				} else {
					return Byte.class;
				}
			}
		} else if (lclS.equals("float")) {
			return Double.class;
		} else if (lclS.equals("real")) {
			return Float.class;
		} else if (lclS.equals("money")) { // THINK: Is this really correct, since money and smallmoney are exact types?
			return Double.class;
		} else if (lclS.equals("smallmoney")) {
			return Double.class;
		} else if (lclS.equals("varbinary") || lclS.equals("image") || lclS.equals("binary")) {
			return byte[].class;
		} else if (lclS.equals("smalldatetime") || lclS.equals("cursor") || lclS.equals("timestamp") || lclS.equals("uniqueidentifier")) {
			throw new IllegalStateException("Unable to map data type " + lclS + ".");
		} else {
			throw new IllegalStateException("Unable to map unknown type " + lclS);
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
	
	public String getDefaultDatabase() {
		return myDefaultDatabase;
	}
	
	public String getDefaultOwner() {
		return myDefaultOwner;
	}
	
	@Override
	public void initialize(Element argElement) {
		assert argElement != null;
		setDefaultDatabase(XMLElement.getAttributeValue(argElement, "DefaultDatabase"));
		setDefaultOwner(XMLElement.getAttributeValue(argElement, "DefaultOwner"));
	}
	
	@Override
	protected ArrayList<DatabaseColumn> loadColumns() throws SQLException {
		String lclSQL = 
			"""
			SELECT C.TABLE_CATALOG, C.TABLE_SCHEMA, C.TABLE_NAME, C.COLUMN_NAME, C.COLUMN_DEFAULT, C.IS_NULLABLE, C.DATA_TYPE, C.CHARACTER_MAXIMUM_LENGTH, C.NUMERIC_PRECISION, C.NUMERIC_SCALE, C.CHARACTER_SET_NAME, C.DOMAIN_NAME, C2.status
			FROM INFORMATION_SCHEMA.TABLES T,
				INFORMATION_SCHEMA.COLUMNS C,
				sysobjects T2,
				syscolumns C2
			WHERE T.TABLE_CATALOG = C.TABLE_CATALOG
				AND T.TABLE_SCHEMA = C.TABLE_SCHEMA
				AND T.TABLE_NAME = C.TABLE_NAME
				AND T.TABLE_TYPE IN ('BASE TABLE', 'VIEW')
				AND T.TABLE_NAME = T2.name
				AND C.COLUMN_NAME = C2.name
				AND T2.id = C2.id
			ORDER BY C.TABLE_CATALOG, C.TABLE_SCHEMA, C.TABLE_NAME, C.ORDINAL_POSITION
			""";
		
		ArrayList<DatabaseColumn> lclColumns = new ArrayList<>();
		
		try (Connection lclC = getDataSource().getConnection()) {
			try (ResultSet lclRS = DatabaseUtility.select(lclC, lclSQL)) {
				while (lclRS.next()) {
					String lclColumnName = lclRS.getString("COLUMN_NAME");
					
					String lclDefaultString = lclRS.getString("COLUMN_DEFAULT");
					
					boolean lclIdentity = (lclRS.getInt("status") & 0x80) > 0;
					
					boolean lclValueComesFromSequence;
					if (lclDefaultString != null && lclDefaultString.toUpperCase().contains("NEXT VALUE FOR")) {
						lclValueComesFromSequence = true;
					} else {
						lclValueComesFromSequence = false;
					}

					DefaultValue lclDefaultValue = createDefaultValue(lclDefaultString);
					
					boolean lclWideCharacters = WIDE_CHARACTER_SET.equals(lclRS.getString("CHARACTER_SET_NAME")); 
					
					DatabaseColumn lclDC = new SQLServerColumn(
							new SQLServerTableName(
									lclRS.getString("TABLE_CATALOG"),
									lclRS.getString("TABLE_SCHEMA"),
									lclRS.getString("TABLE_NAME")
									),
							lclColumnName,
							lclRS.getString("DATA_TYPE"),
							lclRS.getInt("CHARACTER_MAXIMUM_LENGTH"),
							lclRS.getInt("NUMERIC_PRECISION"),
							lclRS.getInt("NUMERIC_SCALE"),
							lclWideCharacters,
							lclRS.getString("IS_NULLABLE").equalsIgnoreCase("YES"),
							lclRS.getString("DOMAIN_NAME"),
							lclDefaultValue,
							lclIdentity						
							);
					
					lclDC.setComplicatedDefault(lclValueComesFromSequence);
					
					lclColumns.add(lclDC);
	//				System.out.println("Loaded " + lclDC);
				}
				
				return lclColumns;
			} // autoclose lcLRS
		} // autoclose lclC
	}
	
	@Override
	protected void loadCheckConstraints() throws SQLException {
		String lclSQL = 
			"""
			SELECT T.TABLE_CATALOG, T.TABLE_SCHEMA, T.TABLE_NAME, T.COLUMN_NAME, C.CONSTRAINT_NAME, C.CHECK_CLAUSE
			FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS C
				JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE T
					ON T.TABLE_CATALOG = C.CONSTRAINT_CATALOG
						AND T.TABLE_SCHEMA = C.CONSTRAINT_SCHEMA
						AND T.CONSTRAINT_NAME = C.CONSTRAINT_NAME
			""";
		
		ArrayList<DatabaseColumn> lclColumns = getColumns();
		
		try (Connection lclC = getDataSource().getConnection()) {
			try (ResultSet lclRS = DatabaseUtility.select(lclC, lclSQL)) {
				while (lclRS.next()) {
					SQLServerTableName lclTableName = new SQLServerTableName(
							lclRS.getString("TABLE_CATALOG"),
							lclRS.getString("TABLE_SCHEMA"),
							lclRS.getString("TABLE_NAME")
							);
					
					String lclColumnName = lclRS.getString("COLUMN_NAME");
					
//					/* SQL Server 2003 allowed access to the CHECK_CLAUSE column for non-sa users.
//					 * SQL Server 2012 does not (it's on a short list of metadata-restricted columns).
//					 * So, if the definition comes back NULL, we skip the column.  This is a hack
//					 * (since we aren't currently doing anything with this information anyway), but
//					 * at some point we need to figure out how to allow a non-sa user to get this
//					 * information (probably some sort of GRANT X ON Y TO Z).
//					 */
//					
//					if (lclRS.getString("CHECK_CLAUSE") == null) {
//						continue;
//					}
					
					for(DatabaseColumn lclDC : lclColumns) {
						if (lclDC.getTableName().equals(lclTableName) && lclDC.getName().equals(lclColumnName)) {
							lclDC.addCheckConstraint(
									new SQLServerCheckConstraint(
											lclRS.getString("CONSTRAINT_NAME"),
											lclRS.getString("CHECK_CLAUSE")
											)
							);
						}
					}
				}
				return;
			}
		}
	}
	
	@Override
	protected java.util.ArrayList<ForeignKey> loadForeignKeys() throws java.sql.SQLException { // FIXME: Rewrite query as proper join
		String lclSQL =
			"""
			SELECT C.name AS constraint_name, T1.name AS source_table_name, T2.name AS target_table_name, C1.name AS source_column_name, C1.isnullable AS source_column_nullable, C2.name as target_column_name, SFK.delete_referential_action AS delete_referential_action, SFK.update_referential_action AS update_referential_action 
			FROM sysobjects C, sysobjects T1, sysobjects T2, syscolumns C1, syscolumns C2, sysforeignkeys FK, sys.foreign_keys SFK
			WHERE FK.constid = C.id
				AND FK.fkeyid = T1.id
				AND FK.rkeyid = T2.id
				AND FK.fkeyid = C1.id
				AND FK.rkeyid = C2.id
				AND FK.fkey = C1.colid
				AND FK.rkey = C2.colid
				AND SFK.object_id = FK.constid AND SFK.object_id = C.id
			ORDER BY C.id, FK.fkeyid, FK.rkeyid, keyno
			""";
		
		ArrayList<ForeignKey> lclFKs = new ArrayList<>();
		
		try (Connection lclC = getDataSource().getConnection()) {
			try (ResultSet lclRS = DatabaseUtility.select(lclC, lclSQL)) {
				ForeignKey lclFK = null;
				int lclRequired = 0;
				while (lclRS.next()) {
					String lclConstraintName = lclRS.getString("constraint_name");
					
					if (lclFK != null && lclFK.getName().equals(lclConstraintName) == false) {
						if (lclRequired == lclFK.getSourceKey().getColumnNames().size()) {
							// THINK: Should this really be > 0?
							lclFK.getSourceKey().setRequired(true);
						}
						
						lclRequired = 0;
					}
										
					if ((lclFK == null) || (!lclFK.getName().equals(lclConstraintName))) {
						
						ReferentialAction lclDeleteAction = Validate.notNull(determineDeleteAction(lclRS.getInt("delete_referential_action")));
						ReferentialAction lclUpdateAction = Validate.notNull(determineUpdateAction(lclRS.getInt("update_referential_action")));
						
						lclFKs.add(
								lclFK = new ForeignKey(
										new Key(
												new SQLServerTableName(
													getDefaultDatabase(),
													getDefaultOwner(),
													lclRS.getString("source_table_name")
												),
												lclConstraintName + "_SOURCE",
												false /* not required, for now */
												),
										new Key(
												new SQLServerTableName(
													getDefaultDatabase(),
													getDefaultOwner(),
													lclRS.getString("target_table_name")
												),
												lclConstraintName + "_TARGET",
												false /* not required, for now */
												),
										lclConstraintName,
										lclDeleteAction,
										lclUpdateAction
										)
									);
					}
					
					lclFK.getSourceKey().getColumnNames().add(lclRS.getString("source_column_name"));
					lclFK.getTargetKey().getColumnNames().add(lclRS.getString("target_column_name"));
					
					if (lclRS.getInt("source_column_nullable") == 0) {
						++lclRequired;
					}
					
				}
				
				// Check requiredness one last time for the last group.
				if (lclFK != null && lclRequired == lclFK.getSourceKey().getColumnNames().size()) {
					// THINK: Should this really be > 0?
					lclFK.getSourceKey().setRequired(true);
				}
			} // autoclose lclRS
		} // autoclose lclC
		
		return lclFKs;
	}
	
	protected ReferentialAction determineDeleteAction(int argDeleteAction) {
		return determineReferentialAction(argDeleteAction);
	}
	
	protected ReferentialAction determineUpdateAction(int argUpdateAction) {
		return determineReferentialAction(argUpdateAction);
	}
	
	protected ReferentialAction determineReferentialAction(int argAction) {
		switch (argAction) {
		case 0: return NO_ACTION;
		case 1: return CASCADE;
		case 2: return SET_NULL;
		case 3: return SET_DEFAULT;
		default: throw new IllegalStateException("Unknown referential action numeric code " + argAction + ".");
		}
	}
	
	@Override
	protected ArrayList<Index> loadIndexes() throws java.sql.SQLException {
		
		/* FIXME: I think this query is mixing two different styles of metadata tables to get the information it needs.
		 * Probably it can be rewritten in terms of sys.* tables (only) or in terms of the (more standard?)
		 * INFORMATION_SCHEMA views.
		 */
		String lclSQL =
				"""
				SELECT SI.name as index_name, SI.status, T.name as table_name, C.name as column_name, IC.keyno, SI2.filter_definition
				FROM sysobjects T
					JOIN sysindexes SI ON T.id = SI.id
					JOIN sys.indexes SI2 ON SI.name = SI2.name AND SI2.object_id = T.id AND SI2.object_id = SI.id
					JOIN sysindexkeys IC ON T.id = IC.id AND SI.id = IC.id AND SI.indid = IC.indid
					JOIN syscolumns C ON T.id = C.id AND SI.id = C.id AND IC.colid = C.colid
				WHERE SI.indid between 1 and 254
					AND (SI2.has_filter = 0 OR (SI2.has_filter = 1 AND SI2.filter_definition LIKE '% IS NOT NULL)')) -- OpalCaches don't know how to handle filtered indexes that don't simply filter out NULL keys; this is a hack, no two ways about it.		
					AND IC.keyno > 0 -- Exclude columns that are merely INCLUDEd as part of the index, but not part of the key
				ORDER BY SI.id, SI.indid, IC.keyno
				""";

		ArrayList<Index> lclIndexes = new ArrayList<>();
		
		try (Connection lclC = getDataSource().getConnection(); ResultSet lclRS = DatabaseUtility.select(lclC, lclSQL)) {
			Index lclIndex = null;
			
			while (lclRS.next()) {
				String lclIndexName = lclRS.getString("index_name");
				
				/* This next command is pretty impressive. */
				if ((lclIndex == null) || (!lclIndex.getIndexName().equals(lclIndexName))) {
					lclIndexes.add(
							lclIndex = new SQLServerIndex(
									new SQLServerTableName(
											getDefaultDatabase(), /* FIXME:  Wrong! */
											getDefaultOwner(), /* FIXME:  Wrong! */
											lclRS.getString("table_name")
											),
									lclIndexName,
									(lclRS.getInt("status") & UNIQUE_FLAG) > 0,
									(lclRS.getInt("status") & ENFORCES_PRIMARY_KEY) > 0,
									lclRS.getString("filter_definition")
									)
							);
				}
				lclIndex.getColumnNames().add(lclRS.getString("column_name"));
			}
			
			return lclIndexes;
		} // autoclose
	}
	
	@Override
	protected ArrayList<PrimaryKey> loadPrimaryKeys() {
		Collection<Index> lclIndexes = getIndexes();
		
		ArrayList<PrimaryKey> lclPKs = new ArrayList<>();
		
		Iterator<Index> lclI = lclIndexes.iterator();
		while (lclI.hasNext()) {
			SQLServerIndex lclIndex = (SQLServerIndex) lclI.next();
			if (lclIndex.isPrimaryKey()) {
				PrimaryKey lclPK = new PrimaryKey(lclIndex.getTableName(), lclIndex.getIndexName());
				lclPK.getColumnNames().addAll(lclIndex.getColumnNames());
				lclPKs.add(lclPK);
			}
		}
		
		return lclPKs;
	}
	
	public void setDefaultDatabase(String argDefaultDatabase) {
		myDefaultDatabase = argDefaultDatabase;
	}
	
	public void setDefaultOwner(String argDefaultOwner) {
		myDefaultOwner = argDefaultOwner;
	}
	
	@Override
	protected String generateForeignKeyStatisticsSQL(ForeignKey argFK) {
		Validate.notNull(argFK);
		
		StringBuilder lclSB = new StringBuilder(1024);
		lclSB.append("SELECT min(T.num) AS min, avg(T.num) AS avg, max(T.num) AS max, stdev(T.num) AS stdev ");
		lclSB.append("FROM (SELECT ");
		
		final int lclColumnCount = argFK.getSourceKey().getColumnNames().size();
		
		for (int lclI = 0; lclI < lclColumnCount; ++lclI) {
			final String lclTargetColumnName = argFK.getTargetKey().getColumnNames().get(lclI);
			if (lclI > 0) {
				lclSB.append(", ");
			}
			lclSB.append("B.");
			lclSB.append(lclTargetColumnName);
		}
		
		lclSB.append(", 1.0*count(A.");
		lclSB.append(argFK.getSourceKey().getColumnNames().get(0));
		lclSB.append(") AS num ");
		lclSB.append("FROM ");
		lclSB.append(argFK.getSourceKey().getTableName());
		lclSB.append(" A RIGHT OUTER JOIN ");
		lclSB.append(argFK.getTargetKey().getTableName());
		lclSB.append(" B ");
		lclSB.append("ON ");
		
		for (int lclI = 0; lclI < lclColumnCount; ++lclI) {
			final String lclSourceColumnName = argFK.getSourceKey().getColumnNames().get(lclI);
			final String lclTargetColumnName = argFK.getTargetKey().getColumnNames().get(lclI);
			if (lclI > 0) {
				lclSB.append("AND ");
			}
			lclSB.append("B.");
			lclSB.append(lclTargetColumnName);
			lclSB.append(" = A.");
			lclSB.append(lclSourceColumnName);
			lclSB.append(' ');
		}
		lclSB.append("GROUP BY ");
		
		for (int lclI = 0; lclI < lclColumnCount; ++lclI) {
			final String lclTargetColumnName = argFK.getTargetKey().getColumnNames().get(lclI);
			if (lclI > 0) {
				lclSB.append(", ");
			}
			lclSB.append("B.");
			lclSB.append(lclTargetColumnName);
		}
		lclSB.append(") T");
		
		return lclSB.toString();
	}

	@Override
	protected void determineTablesAndViews(Map<TableName, MappedClass> argMCs) throws SQLException {
		try (Connection lclC = getDataSource().getConnection()) {
			try (ResultSet lclRS = DatabaseUtility.select(lclC,  "SELECT T.TABLE_CATALOG, T.TABLE_SCHEMA, T.TABLE_NAME, T.TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES T")) {
				while (lclRS.next()) {
					TableName lclTN = new SQLServerTableName(
							lclRS.getString("TABLE_CATALOG"),
							lclRS.getString("TABLE_SCHEMA"),
							lclRS.getString("TABLE_NAME")
							);
					
					MappedClass lclMC = argMCs.get(lclTN);
					
					if (lclMC != null) {
						String lclTableType = lclRS.getString("TABLE_TYPE");
						if ("BASE TABLE".equals(lclTableType)) {
							lclMC.setTrueEntityType(EntityType.Table);							
						} else if ("VIEW".equals(lclTableType)) {
							lclMC.setTrueEntityType(EntityType.View);
						} else {
							System.out.println("*** Entity " + lclMC.getTableName() + " has a type of \"" + lclTableType + "\", which is expected to be either BASE_TABLE or VIEW.");
							lclMC.setTrueEntityType(EntityType.Unspecified);
						}
					}
				}
			}
		}
	}
	

}
