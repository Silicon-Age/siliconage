package com.opal.creator.database.postgres;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import com.opal.creator.database.DefaultValue;
import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.EntityType;
import com.opal.creator.database.ForeignKey;
import com.opal.creator.database.Index;
import com.opal.creator.database.Key;
import com.opal.creator.database.PrimaryKey;
import com.opal.creator.database.RelationalDatabaseAdapter;
import com.opal.creator.database.TableName;

public class PostgresAdapter extends RelationalDatabaseAdapter {
	private static final String DATABASE_OPAL_PREFIX = "Postgres";
	private static final String DATABASE_PACKAGE_SUFFIX = "postgres";
	
	private String myDefaultDatabase;
	private String myDefaultOwner;

	public PostgresAdapter(DataSource argDataSource) {
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
	
	@Override
	protected void createAfterInsertMethod(PrintWriter argBW, MappedClass argMC) {
//		assert argBW != null;
//		assert argMC != null;
//		
//		if (argMC.isCreatable()) {
//			Iterator<ClassMember> lclI = argMC.createClassMemberIterator();
//			while (lclI.hasNext()) {
//				ClassMember lclCM = lclI.next();
//				PostgresColumn lclColumn = (PostgresColumn) lclCM.getDatabaseColumn();
//				final String lclSequenceName = lclColumn.getSequenceName();
//				if (lclSequenceName != null) {
//					System.out.println("Found mandate for afterInsert method for " + argMC);
//					
//					final String lclOCN = argMC.getOpalClassName();
//					
//					System.out.println("Generating afterInsert method for " + argMC);
//					argBW.println("\t@Override");
//					argBW.println("\tprotected void afterInsert(TransactionParameter argTP, " + lclOCN + " argOpal) throws PersistenceException {");
//					argBW.println("\t\tassert argTP != null;");
//					argBW.println("\t\tassert argOpal != null;");
//					argBW.println("\t\ttry {");
//					argBW.println("\t\t\targOpal." + lclCM.getPrimitiveMutatorName() + "(");
//					argBW.println("\t\t\t\tcom.siliconage.database.DatabaseUtility.executeIntQuery(");
//					argBW.println("\t\t\t\t\t((DatabaseTransactionParameter) argTP).getConnection(),");
//					argBW.println("\t\t\t\t\t\"SELECT last_value FROM " + lclSequenceName + " AS id_value\",");
//					argBW.println("\t\t\t\t\tnull");
//					argBW.println("\t\t\t\t)");
//					argBW.println("\t\t\t);");
//					argBW.println("\t\t\treturn;");
//					argBW.println("\t\t} catch (SQLException lclE) {");
//					argBW.println("\t\t\tthrow new PersistenceException(\"Unable to retrieve last value for sequence column " + lclSequenceName + "\", lclE);");
//					argBW.println("\t\t}");
//					argBW.println("\t}");
//					argBW.println();
//				}
//			}
//		}
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
		argBW.println("\t@Override");
		argBW.println("\tprotected String getFullyQualifiedTableName() {");
		argBW.println("\t\treturn \"" + argMC.getTableName().getFullyQualifiedTableName() + "\";");
		argBW.println("\t}");
		argBW.println();
	}
	
	private static final String OPAL_FACTORY_FACTORY_CLASS_NAME = "PostgresOpalFactoryFactory";
	
	@Override
	protected String getOpalFactoryFactoryClassName() {
		return OPAL_FACTORY_FACTORY_CLASS_NAME;
	}
	
	@Override
	public PostgresTableName createTableName(Element argElement) {
		assert argElement != null;
		
		String lclDatabaseName;
		if (argElement.hasAttribute("Database")) {
			lclDatabaseName = argElement.getAttribute("Database");
		} else {
			// lclDatabaseName = getDefaultDatabase(); // Leads to problems if the database changes
			lclDatabaseName = null; // Null means to use whatever the default database for the connection is 
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
		
//		System.out.println("Creating table name: " + lclDatabaseName + "/" + lclTableName);
		return new PostgresTableName(lclDatabaseName, lclTableName);
	}
	
	public PostgresTableName createTableName(String argDatabaseName, String argTableName) {
		// String lclDatabaseName = (argDatabaseName == null) ? getDefaultDatabase() : argDatabaseName;
		return new PostgresTableName(argDatabaseName, argTableName);
	}
	
	public PostgresTableName createTableName(String argTableName) {
		return createTableName(null, argTableName);
	}
	
	@Override
	public Class<?> determineJavaType(DatabaseColumn argDatabaseColumn) {
		if (argDatabaseColumn instanceof PostgresColumn) {
			return determineJavaType((PostgresColumn) argDatabaseColumn);
		} else {
			throw new IllegalStateException("argDatabaseColumn is of type " + argDatabaseColumn.getClass().getName());
		}
	}
	
	public Class<?> determineJavaType(PostgresColumn argDatabaseColumn) {
		/* Other things to think about:  NVARCHAR2, NCHAR, NVARCHAR, UNDEFINED, SDO_DIM_ARRAY, SDO_GEOMETRY, ROWID */
		final String lclS = argDatabaseColumn.getDataType();
		
		final String lclDomainName = argDatabaseColumn.getDomainName();
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
		
		if (lclS.equals("text") || lclS.equals("character varying") || lclS.equals("character")) {
			if (argDatabaseColumn.getLength() == 1) {
				return Character.class;
			} else {
				return String.class;
			}
		} else if (lclS.equals("json")) {
			// TODO: Should probably be a com.google.gson.JsonObject
			return String.class;
		} else if (lclS.equals("date")) { /* FIXME:  Date should probably be something else */
			return java.time.LocalDate.class;
		} else if (lclS.equals("time") || lclS.equals("time with time zone") || lclS.equals("time without time zone") || lclS.equals("timestamp") || lclS.equals("timestamp with time zone") || lclS.equals("timestamp without time zone")) {
			return java.time.LocalDateTime.class;
		} else if (lclS.equals("int") || lclS.equals("integer")) {
			return Integer.class;
		} else if (lclS.equals("smallint")) {
			return Short.class;
		} else if (lclS.equals("bigint")) {
			return Long.class;
		} else if (lclS.equals("decimal") || lclS.equals("numeric")) {
			if (argDatabaseColumn.getScale() > 0) {
				return BigDecimal.class;
			} else {
				if (argDatabaseColumn.getPrecision() > MAXIMUM_SAFE_DIGITS_FOR_LONG) {
					return BigInteger.class;
				} else if (argDatabaseColumn.getPrecision() > MAXIMUM_SAFE_DIGITS_FOR_INT) {
					return Long.class;
				} else {
					return Integer.class;
				}
			}
		} else if (lclS.equals("money")) {
			return BigDecimal.class;
		} else if (lclS.equals("boolean")) {
			return Boolean.class;
		} else if (lclS.equals("bit")){
			if (argDatabaseColumn.getLength() > 64) {
				throw new IllegalStateException("Cannot map a bit column with length longer than 64.");
			} else if (argDatabaseColumn.getLength() > 32) {
				return Long.class;
			} else if (argDatabaseColumn.getLength() > 1) {
				return Integer.class;
			} else {
				return Boolean.class;
			}
		} else if (lclS.equals("real")) {
			return Double.class;
		} else if (lclS.equals("double precision")) {
			return Double.class;
		// } else if (lclS.equals("blob") || lclS.equals("longblob")) {
			// return byte[].class;
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
	
	@Override
	public boolean isLargeDatabaseType(DatabaseColumn argDatabaseColumn) {
		assert argDatabaseColumn != null;
		
//		final String lclS = argDatabaseColumn.getDataType();
		
		return false;
	}
	
	@Override
	public void initialize(Element argElement) {
		assert argElement != null;
		setDefaultDatabase(XMLElement.getAttributeValue(argElement, "DefaultDatabase"));
		setDefaultOwner(XMLElement.getAttributeValue(argElement, "DefaultOwner"));
		
		System.out.println("Default database is: " + getDefaultDatabase());
	}
	
	@Override
	protected ArrayList<DatabaseColumn> loadColumns() throws SQLException {
		String lclSQL = 
				"SELECT C.TABLE_CATALOG, C.TABLE_SCHEMA, C.TABLE_NAME, C.COLUMN_NAME, " +
					"C.COLUMN_DEFAULT, C.IS_NULLABLE, C.DATA_TYPE, C.DOMAIN_NAME, " +
					"C.CHARACTER_MAXIMUM_LENGTH, C.NUMERIC_PRECISION, C.NUMERIC_SCALE " +
				"FROM INFORMATION_SCHEMA.TABLES T, " +
					"INFORMATION_SCHEMA.COLUMNS C " +
				"WHERE T.TABLE_SCHEMA = C.TABLE_SCHEMA " +
					"AND T.TABLE_NAME = C.TABLE_NAME " +
					"AND T.TABLE_TYPE IN ('BASE TABLE', 'VIEW') " +
					"AND T.TABLE_SCHEMA NOT IN ('information_schema', 'pg_catalog') " +
				"ORDER BY C.TABLE_CATALOG, C.TABLE_SCHEMA, C.TABLE_NAME, C.ORDINAL_POSITION;";
		
		ArrayList<DatabaseColumn> lclColumns = new ArrayList<>();
		
		ResultSet lclRS = null;
		
		try {
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);
			
			while (lclRS.next()) {
				String lclColumnName = lclRS.getString("COLUMN_NAME");
				
				String lclDefaultString = lclRS.getString("COLUMN_DEFAULT");
				
				String lclSequenceName = null;
				if (lclDefaultString != null && lclDefaultString.contains("nextval")) {
					lclSequenceName = lclDefaultString.replaceAll("nextval\\('", "").replaceAll("'::regclass\\)", "");
				}
				
				if (lclDefaultString != null && lclDefaultString.endsWith("'::character varying")) {
					lclDefaultString = lclDefaultString.replaceAll("::character varying", "");
				}
				
				DefaultValue lclDefaultValue = createDefaultValue(lclDefaultString);
				
				String lclTableDatabase = lclRS.getString("TABLE_SCHEMA");
				String lclTableName = lclRS.getString("TABLE_NAME");
				
				/* FIXME: This is a hack to make the column names match up until we have a proper way for dealing
				 * with multiple schemata, one of which may (or may not) be the default database for one or more of
				 * the JDBC connections with which the opals may be used.
				 */
				if (lclTableDatabase.equals(getDefaultDatabase())) {
					lclTableDatabase = null;
				}
				
				int lclMaximumLength = lclRS.getInt("CHARACTER_MAXIMUM_LENGTH");
				if (lclRS.wasNull()) {
					lclMaximumLength = Integer.MAX_VALUE;
				}
				
				DatabaseColumn lclDC = new PostgresColumn(
						new PostgresTableName(
								lclTableDatabase,
								lclTableName
								),
						lclColumnName,
						lclRS.getString("DATA_TYPE"),
						lclMaximumLength,
						lclRS.getInt("NUMERIC_PRECISION"),
						lclRS.getInt("NUMERIC_SCALE"),
						false, // FIXME: I'm not sure how to handle wide characters in modern versions of PostgreSQL
						lclRS.getString("DOMAIN_NAME"),
						lclRS.getString("IS_NULLABLE").equalsIgnoreCase("YES"),
						lclDefaultValue,
						lclSequenceName
						);
				
				lclColumns.add(lclDC);
//				System.out.println("Loaded " + lclDC);
			}
			
			return lclColumns;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	protected DefaultValue createDefaultValue(String argDefaultString) {
		if (argDefaultString == null) {
			return null;
		}
		
		String lclDefaultString = argDefaultString.trim();
		
		if ("".equals(lclDefaultString)) {
			return null;
		}
		
		if (lclDefaultString.startsWith("'") == false && lclDefaultString.contains("(") && lclDefaultString.equalsIgnoreCase("now()") == false) {
			// FIXME Hacky check for the default being a function
			return null;
		}
		
		if (lclDefaultString.charAt(0) == '\'') {
			String lclAdjusted = lclDefaultString.replaceAll("::bpchar","");
			return new ConstantDefaultValue(lclAdjusted.substring(1, lclAdjusted.length() - 1));
		}
		
		if (lclDefaultString.equalsIgnoreCase("null")) {
			return null;
		}
		
		if (lclDefaultString.equalsIgnoreCase("now()")) {
			return CurrentTimestampDefaultValue.getInstance();
		}
		
		try {
			int lclI = Integer.parseInt(lclDefaultString);
			return new ConstantDefaultValue(Integer.valueOf(lclI));
		} catch (NumberFormatException lclE) {
			/* Okay, not an int */
		}
		
		
		if (lclDefaultString.equalsIgnoreCase("true")) {
			return new ConstantDefaultValue(Boolean.TRUE);
		} else if (lclDefaultString.equalsIgnoreCase("false")) {
			return new ConstantDefaultValue(Boolean.FALSE);
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
		
		return new ConstantDefaultValue(lclDefaultString);
	}
	
	@Override
	protected void loadCheckConstraints() throws SQLException {
		String lclSQL = 
			"SELECT T.TABLE_CATALOG, T.TABLE_SCHEMA, T.TABLE_NAME, T.COLUMN_NAME, C.CONSTRAINT_NAME, C.CHECK_CLAUSE "+
				"FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS C, " +
					"INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE T " +
				"WHERE T.TABLE_CATALOG = C.CONSTRAINT_CATALOG " +
					"AND T.TABLE_SCHEMA = C.CONSTRAINT_SCHEMA " +
					"AND T.CONSTRAINT_NAME = C.CONSTRAINT_NAME";
		
		ResultSet lclRS = null;
		
		ArrayList<DatabaseColumn> lclColumns = getColumns();
		
		try {
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);
			
			while (lclRS.next()) {
				PostgresTableName lclTableName = new PostgresTableName(
					lclRS.getString("TABLE_SCHEMA"),
					lclRS.getString("TABLE_NAME")
				);
				
				String lclColumnName = lclRS.getString("COLUMN_NAME");
				
				for (DatabaseColumn lclDC : lclColumns) {
					if (lclDC.getTableName().equals(lclTableName) && lclDC.getName().equals(lclColumnName)) {
						lclDC.addCheckConstraint(
							new PostgresCheckConstraint(
								lclRS.getString("CONSTRAINT_NAME"),
								lclRS.getString("CHECK_CLAUSE")
							)
						);
					}
				}
			}
			
			return;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	@Override
	protected ArrayList<ForeignKey> loadForeignKeys() throws java.sql.SQLException {
		System.out.println("Loading foreign keys . . .");
		String lclSQL =
			"SELECT " +
				"CON.constraint_name AS constraint_name, " +
				"CON.table_schema AS source_table_schema, " +
				"CON.table_name AS source_table_name, " +
				"COLS.is_nullable AS source_column_nullable, " +
				"SOURCE.column_name AS source_column_name, " +
				"TARGET.table_schema AS target_table_schema, " +
				"TARGET.table_name AS target_table_name, " +
				"TARGET.column_name AS target_column_name " +
			"FROM information_schema.table_constraints CON " +
				"JOIN information_schema.key_column_usage SOURCE " +
					"ON CON.constraint_catalog = SOURCE.constraint_catalog " +
					"AND CON.constraint_schema = SOURCE.constraint_schema " +
					"AND CON.constraint_name = SOURCE.constraint_name " +
				"JOIN information_schema.columns COLS " +
					"ON SOURCE.table_catalog = COLS.table_catalog " +
					"AND SOURCE.table_schema = COLS.table_schema " +
					"AND SOURCE.table_name = COLS.table_name " +
					"AND SOURCE.column_name = COLS.column_name " +
				"JOIN information_schema.referential_constraints RC " +
					"ON CON.constraint_catalog = RC.constraint_catalog " +
					"AND CON.constraint_schema = RC.constraint_schema " +
					"AND CON.constraint_name = RC.constraint_name " +
				"LEFT JOIN information_schema.key_column_usage TARGET " +
					"ON TARGET.constraint_catalog = RC.constraint_catalog " +
					"AND TARGET.constraint_schema = RC.constraint_schema " +
					"AND TARGET.constraint_name = RC.unique_constraint_name " +
			"WHERE LOWER(CON.constraint_type) in ('foreign key');";
		
		ArrayList<ForeignKey> lclFKs = new ArrayList<>();
		
		ResultSet lclRS = null;
		
		try {
//			System.out.println("Executing foreign key query . . .");
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);
			
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
				
				
				/* This next command is pretty impressive. */
				String lclSourceSchema = lclRS.getString("source_table_schema");
				if (lclSourceSchema.equalsIgnoreCase(getDefaultDatabase())) {
					lclSourceSchema = null;
				}
				String lclSourceTable = lclRS.getString("source_table_name");
				
				String lclTargetSchema = lclRS.getString("target_table_schema");
				if (lclTargetSchema.equalsIgnoreCase(getDefaultDatabase())) {
					lclTargetSchema = null;
				}
				
				String lclTargetTable = lclRS.getString("target_table_name");
				
				System.out.println("FOREIGN KEY: source table " + lclSourceTable + ", target schema " + lclTargetSchema + "; target table " + lclTargetTable + "; constraint name " + lclConstraintName + "; source column name " + lclRS.getString("source_column_name") + "; target column name " + lclRS.getString("target_column_name"));
				
				ReferentialAction lclDeleteAction = NO_ACTION; // TODO: Determine correctly
				ReferentialAction lclUpdateAction = NO_ACTION; // TODO: Determine correctly
				
				if ((lclFK == null) || (!lclFK.getName().equals(lclConstraintName))) {
					System.out.println("Adding new key " + lclConstraintName);
					lclFKs.add(
						lclFK = new ForeignKey(
							new Key(
								new PostgresTableName(
									lclSourceSchema,
									lclSourceTable
								),
								lclConstraintName + "_SOURCE",
								false /* not required, for now */
							),
							new Key(
								new PostgresTableName(
									lclTargetSchema,
									lclTargetTable
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
				System.out.println("Adding column");
				lclFK.getSourceKey().getColumnNames().add(lclRS.getString("source_column_name"));
				lclFK.getTargetKey().getColumnNames().add(lclRS.getString("target_column_name"));
				
				if ("NO".equalsIgnoreCase(lclRS.getString("source_column_nullable"))) {
					++lclRequired;
				}
			}
			
			// Check requiredness one last time for the last group
			if (lclFK != null && lclRequired == lclFK.getSourceKey().getColumnNames().size()) {
				// THINK: Should this really be > 0?
				lclFK.getSourceKey().setRequired(true);
			}
			
			Iterator<ForeignKey> lclI = lclFKs.iterator();
			while (lclI.hasNext()) {
				System.out.println(lclI.next());
			}
			
			System.out.println("About to return from loadForeignKeys");
			return lclFKs;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	@Override
	protected ArrayList<Index> loadIndexes() throws java.sql.SQLException {
		String lclSQL =
			"SELECT CON.CONSTRAINT_NAME AS index_name, " +
				"CON.CONSTRAINT_TYPE AS constraint_type, " +
				"C.TABLE_SCHEMA AS source_table_schema, " +
				"C.TABLE_NAME AS table_name, " +
				"C.COLUMN_NAME AS column_name " +
			"FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS CON, " +
				"INFORMATION_SCHEMA.COLUMNS C, " +
				"INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU " +
			"WHERE CON.TABLE_SCHEMA = KCU.CONSTRAINT_SCHEMA " +
				"AND CON.TABLE_NAME = KCU.TABLE_NAME " +
				"AND CON.CONSTRAINT_NAME = KCU.CONSTRAINT_NAME " +
				"AND CON.CONSTRAINT_TYPE IN ('PRIMARY KEY', 'UNIQUE') " +
				"AND KCU.TABLE_SCHEMA = C.TABLE_SCHEMA " +
				"AND KCU.TABLE_NAME = C.TABLE_NAME " +
				"AND KCU.COLUMN_NAME = C.COLUMN_NAME " +
			"ORDER BY KCU.TABLE_SCHEMA, KCU.TABLE_NAME, KCU.CONSTRAINT_NAME, KCU.ORDINAL_POSITION";
		
		ArrayList<Index> lclIndexes = new ArrayList<>();
		
		ResultSet lclRS = null;
		
		try {
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);
			
			Index lclIndex = null;
			
			String lclLastIndexName = null;
			String lclLastTableName = null;
			
			while (lclRS.next()) {
				String lclIndexName = lclRS.getString("index_name");
				String lclTableDatabase = lclRS.getString("source_table_schema");
				String lclTableName = lclRS.getString("table_name");
				
				/* FIXME: This is a hack so that columns read from the indexes will end up with .equal()
				 * TableNames to those created from the XML file.  Basically, if they are in the default
				 * database for the XML file, then null out the database name.
				 */
//				System.out.println("lclTableDatabase = " + lclTableDatabase + " DefaultDatabase = " + getDefaultDatabase());
				if (lclTableDatabase.equals(getDefaultDatabase())) {
//					System.out.println("Nulling Default Database");
					lclTableDatabase = null;
				}
				
				/* All primary keys are named "PRIMARY" so we can't tell that we have entered a 
				 * new index by the changing of the constraint name. */
				
				boolean lclNewIndex = false;
				if (lclIndex == null) {
					lclNewIndex = true;
				} else {
					Validate.notNull(lclLastIndexName);
					Validate.notNull(lclLastTableName);
					if (!lclLastIndexName.equals(lclIndexName) || (!lclLastTableName.equals(lclTableName))) {
						lclNewIndex = true;
					}
				}
				if (lclNewIndex) {
					String lclType = lclRS.getString("constraint_type");
					
					boolean lclPrimaryKey = "PRIMARY KEY".equals(lclType);
					boolean lclUnique = lclPrimaryKey || "UNIQUE".equals(lclType);
					
					lclIndexes.add(
						lclIndex = new PostgresIndex(
							createTableName(lclTableDatabase, lclTableName),
							lclIndexName,
							lclUnique,
							lclPrimaryKey
						)
					);
					
//					System.out.println("While loading PKs, made table name " + lclIndex.getTableName());
					lclLastIndexName = lclIndexName;
					lclLastTableName = lclTableName;
				}
				Validate.notNull(lclIndex);
				lclIndex.getColumnNames().add(lclRS.getString("column_name"));
			}
			
//			System.out.println("Loaded " + lclIndexes.size() + " indexes/foreign keys.");
			return lclIndexes;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	@Override
	protected ArrayList<PrimaryKey> loadPrimaryKeys() {
		Collection<Index> lclIndexes = getIndexes();
		
		ArrayList<PrimaryKey> lclPKs = new ArrayList<>();
		
		Iterator<Index> lclI = lclIndexes.iterator();
		while (lclI.hasNext()) {
			PostgresIndex lclIndex = (PostgresIndex) lclI.next();
			if (lclIndex.isPrimaryKey()) {
				PrimaryKey lclPK = new PrimaryKey(lclIndex.getTableName(), lclIndex.getIndexName());
				lclPK.getColumnNames().addAll(lclIndex.getColumnNames());
				lclPKs.add(lclPK);
			}
		}
		
		return lclPKs;
	}
	
	public String getDefaultDatabase() {
		return myDefaultDatabase;
	}
	
	public String getDefaultOwner() {
		return myDefaultOwner;
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
		lclSB.append("SELECT min(T.num) AS min, avg(T.num) AS avg, max(T.num) AS max, stddev_pop(T.num) AS stdev ");
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
		lclSB.append(" A LEFT OUTER JOIN ");
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

	/* FIXME: Implement this correctly */
	@Override
	protected void determineTablesAndViews(Map<TableName, MappedClass> argMCs) throws SQLException {
		for (MappedClass lclMC : argMCs.values()) {
			lclMC.setTrueEntityType(EntityType.Table);
		}
	}

}
