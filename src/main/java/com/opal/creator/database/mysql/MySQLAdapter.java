package com.opal.creator.database.mysql;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

import org.w3c.dom.Element;

import com.opal.creator.ClassMember;
import com.opal.creator.MappedClass;
import com.opal.creator.database.ReferentialAction;
import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.DefaultValue;
import com.opal.creator.database.EntityType;
import com.opal.creator.database.ForeignKey;
import com.opal.creator.database.Index;
import com.opal.creator.database.Key;
import com.opal.creator.database.PrimaryKey;
import com.opal.creator.database.RelationalDatabaseAdapter;
import com.opal.creator.database.TableName;
import com.siliconage.database.DatabaseUtility;
import com.siliconage.xml.XMLElement;

public class MySQLAdapter extends RelationalDatabaseAdapter {

	private static final String DATABASE_OPAL_PREFIX = "MySQL";
	private static final String DATABASE_PACKAGE_SUFFIX = "mysql";
	
	private String myDefaultDatabase;
	private String myDefaultOwner;

	public MySQLAdapter(DataSource argDataSource) {
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
//			ClassMember lclAutoIncrementClassMember = null;
//			Iterator<ClassMember> lclI = argMC.createClassMemberIterator();
//			while (lclI.hasNext()) {
//				ClassMember lclCM = lclI.next();
//				if (((MySQLColumn) lclCM.getDatabaseColumn()).isAutoIncrement()) {
//					lclAutoIncrementClassMember = lclCM;
//					System.out.println("Found mandate for afterInsert method for " + argMC);
//					break;
//				}
//			}
//			
//			final String lclOCN = argMC.getOpalClassName();
//			
//			if (lclAutoIncrementClassMember != null) {
//				System.out.println("Generating afterInsert method for " + argMC);
//				argBW.println("\t@Override");
//				argBW.println("\tprotected void afterInsert(TransactionParameter argTP, " + lclOCN + " argOpal) throws PersistenceException {");
//				argBW.println("\t\tassert argTP != null;");
//				argBW.println("\t\tassert argOpal != null;");
//				argBW.println("\t\ttry {");
//				argBW.println("\t\t\targOpal." + lclAutoIncrementClassMember.getPrimitiveMutatorName() + "(");
//				argBW.println("\t\t\t\tcom.siliconage.database.DatabaseUtility.executeIntQuery(");
//				argBW.println("\t\t\t\t\t((DatabaseTransactionParameter) argTP).getConnection(),");
//				argBW.println("\t\t\t\t\t\"SELECT LAST_INSERT_ID() AS id_value\",");
//				argBW.println("\t\t\t\t\tnull");
//				argBW.println("\t\t\t\t)");
//				argBW.println("\t\t\t);");
//				argBW.println("\t\t\treturn;");
//				argBW.println("\t\t} catch (SQLException lclE) {");
//				argBW.println("\t\t\tthrow new PersistenceException(\"Unable to retrieve last value for AUTO_INCREMENT column\", lclE);");
//				argBW.println("\t\t}");
//				argBW.println("\t}");
//				argBW.println();
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
	
	private static final String OPAL_FACTORY_FACTORY_CLASS_NAME = "MySQLOpalFactoryFactory";
	
	@Override
	protected String getOpalFactoryFactoryClassName() {
		return OPAL_FACTORY_FACTORY_CLASS_NAME;
	}
	
	@Override
	public MySQLTableName createTableName(Element argElement) {
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
		return new MySQLTableName(lclDatabaseName, lclTableName);
	}
	
	public MySQLTableName createTableName(String argDatabaseName, String argTableName) {
		// String lclDatabaseName = (argDatabaseName == null) ? getDefaultDatabase() : argDatabaseName;
		String lclDatabaseName = argDatabaseName;
		return new MySQLTableName(lclDatabaseName, argTableName);
	}
	
	public MySQLTableName createTableName(String argTableName) {
		return createTableName(null, argTableName);
	}
	
	@Override
	public Class<?> determineJavaType(DatabaseColumn argDatabaseColumn) {
		if (argDatabaseColumn instanceof MySQLColumn) {
			return determineJavaType((MySQLColumn) argDatabaseColumn);
		} else {
			throw new IllegalStateException("argDatabaseColumn is of type " + argDatabaseColumn.getClass().getName());
		}
	}
	
	public Class<?> determineJavaType(MySQLColumn argDatabaseColumn) {
		/* Other things to think about:  NVARCHAR2, NCHAR, NVARCHAR, UNDEFINED, SDO_DIM_ARRAY, SDO_GEOMETRY, ROWID */
		final String lclS = argDatabaseColumn.getDataType();
		final String lclName = argDatabaseColumn.getName().toUpperCase();
		final boolean lclSigned = argDatabaseColumn.isSigned();
		
		/* TODO:  Some fields should be Booleans */
		
		if (lclS.equals("varchar") || lclS.equals("nvarchar") || lclS.equals("nchar") || lclS.equals("char") || lclS.equals("text") || lclS.equals("longtext")) {
			if (argDatabaseColumn.getLength() == 1) {
				return Character.class;
			} else {
				return String.class;
			}
		} else if (lclS.equals("date")) {
			return java.time.LocalDate.class;
		} else if (lclS.equals("datetime")) {
			return LocalDateTime.class;
		} else if (lclS.equals("timestamp")) {
			return java.time.Instant.class;
		} else if (lclS.equals("time")) {
			return java.time.LocalTime.class;
		} else if (lclS.equals("int") || lclS.equals("integer")) {
			if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
				return Boolean.class;
			}
			return lclSigned ? Integer.class : Long.class;
		} else if (lclS.equals("tinyint")) {
			if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
				return Byte.class;
			}
			return lclSigned ? Byte.class : Short.class;
		} else if (lclS.equals("smallint")) {
			if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
				return Boolean.class;
			}
			return lclSigned ? Short.class : Integer.class;
		} else if (lclS.equals("mediumint")) { /* A 3-byte type */
			if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
				return Boolean.class;
			}
			return Integer.class;
		} else if (lclS.equals("bigint")) {
			if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
				return Boolean.class;
			}
			return lclSigned ? Long.class : BigInteger.class;
		} else if (lclS.equals("decimal") || lclS.equals("numeric")) {
			if (argDatabaseColumn.getScale() > 0) {
				return BigDecimal.class;
			} else {
				if (lclName.startsWith("IS_") || lclName.startsWith("ARE_") || lclName.startsWith("HAS_")) {
					return Boolean.class;
				}
				if (argDatabaseColumn.getPrecision() > MAXIMUM_SAFE_DIGITS_FOR_LONG) {
					return BigInteger.class;
				} else if (argDatabaseColumn.getPrecision() > MAXIMUM_SAFE_DIGITS_FOR_INT) {
					return Long.class;
				} else {
					return Integer.class;
				}
			}
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
		} else if (lclS.equals("float")) {
			return Float.class;
		} else if (lclS.equals("double")) {
			return Double.class;
		} else if (lclS.equals("blob") || lclS.equals("longblob")) {
			return byte[].class;
		} else if (lclS.equals("smalldatetime") || lclS.equals("cursor") || lclS.equals("uniqueidentifier")) {
			throw new IllegalStateException("Unable to map data type " + lclS);
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
				"C.COLUMN_DEFAULT, C.IS_NULLABLE, C.DATA_TYPE, C.COLUMN_TYPE, " +
				"C.CHARACTER_MAXIMUM_LENGTH, C.NUMERIC_PRECISION, C.NUMERIC_SCALE, C.EXTRA " +
			"FROM INFORMATION_SCHEMA.TABLES T, " +
				"INFORMATION_SCHEMA.COLUMNS C " +
			"WHERE T.TABLE_SCHEMA = C.TABLE_SCHEMA " +
				"AND T.TABLE_NAME = C.TABLE_NAME " +
				"AND T.TABLE_TYPE IN ('BASE TABLE', 'VIEW') " +
				"AND T.TABLE_SCHEMA <> 'mysql' " +
			"ORDER BY C.TABLE_CATALOG, C.TABLE_SCHEMA, C.TABLE_NAME, C.ORDINAL_POSITION";
		
		ArrayList<DatabaseColumn> lclColumns = new ArrayList<>();
		
		ResultSet lclRS = null;
		
		try {
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);
			
			while (lclRS.next()) {
				String lclColumnName = lclRS.getString("COLUMN_NAME");
				
				String lclDefaultString = lclRS.getString("COLUMN_DEFAULT");
				
				String lclExtra = lclRS.getString("EXTRA");
				
				boolean lclAutoIncrement = "auto_increment".equalsIgnoreCase(lclExtra);
				
//				if (lclAutoIncrement) { 
//					System.out.println("Found auto_increment column!");
//				}
				
				DefaultValue lclDefaultValue = createDefaultValue(lclDefaultString);
				
				String lclTableDatabase = lclRS.getString("TABLE_SCHEMA");
				String lclTableName = lclRS.getString("TABLE_NAME");
				
				/* FIXME: THis is a hack to make the column names match up until we have a proper way for dealing
				 * with multiple schema, one of which may (or may not) be the default database for one or more of
				 * the JDBC connections with which the opals may be used.
				 */
				if (lclTableDatabase.equals(getDefaultDatabase())) {
					lclTableDatabase = null;
				}
				
				long lclLengthLong = lclRS.getLong("CHARACTER_MAXIMUM_LENGTH");
				int lclLength;
				if (lclLengthLong > Integer.MAX_VALUE) {
					lclLength = Integer.MAX_VALUE;
					System.out.println("Substituting MAX_INT for the maximum length of column " + lclColumnName + " on table " + lclTableName + "; database specification is " + lclLengthLong + ".");
				} else {
					lclLength = (int) lclLengthLong;
				}
				
				DatabaseColumn lclDC = new MySQLColumn(
						new MySQLTableName(
								lclTableDatabase,
								lclTableName
								),
						lclColumnName,
						lclRS.getString("DATA_TYPE"),
						lclLength,
						lclRS.getInt("NUMERIC_PRECISION"),
						lclRS.getInt("NUMERIC_SCALE"),
						false, // FIXME: I'm not sure how to handle wide characters in modern versions of MySQL
						lclRS.getString("DATA_TYPE"),
						lclRS.getString("IS_NULLABLE").equalsIgnoreCase("YES"),
						lclDefaultValue,
						lclAutoIncrement,
						lclRS.getString("COLUMN_TYPE").contains("unsigned") == false
						);
				
				lclColumns.add(lclDC);
//				System.out.println("Loaded " + lclDC);
			}
			
			return lclColumns;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	protected DefaultValue createDefaultValue(@SuppressWarnings("unused") String argDefaultString) {
		return null; // FIXME
	}
	
	@Override
	protected void loadCheckConstraints() {
		return; // FIXME
	}
	
	@Override
	protected ArrayList<ForeignKey> loadForeignKeys() throws java.sql.SQLException {
		System.out.println("Loading foreign keys . . .");
		String lclSQL =
			"SELECT CON.CONSTRAINT_NAME AS constraint_name, " +
				"C1.TABLE_SCHEMA AS source_table_schema, " +
				"C1.TABLE_NAME AS source_table_name, " +
				"C1.COLUMN_NAME AS source_column_name, " +
				"C1.IS_NULLABLE AS source_column_nullable, " +
				"KCU.REFERENCED_TABLE_SCHEMA AS target_table_schema, " +
				"KCU.REFERENCED_TABLE_NAME AS target_table_name, " +
				"KCU.REFERENCED_COLUMN_NAME AS target_column_name " +
			"FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS CON, " +
				"INFORMATION_SCHEMA.COLUMNS C1, " +
				"INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU " +
			"WHERE CON.TABLE_SCHEMA = KCU.CONSTRAINT_SCHEMA " +
				"AND CON.TABLE_NAME = KCU.TABLE_NAME " +
				"AND CON.CONSTRAINT_NAME = KCU.CONSTRAINT_NAME " +
				"AND CON.CONSTRAINT_TYPE = 'FOREIGN KEY' " +
				"AND KCU.TABLE_SCHEMA = C1.TABLE_SCHEMA " +
				"AND KCU.TABLE_NAME = C1.TABLE_NAME " +
				"AND KCU.COLUMN_NAME = C1.COLUMN_NAME " +
			"ORDER BY KCU.TABLE_SCHEMA, KCU.TABLE_NAME, KCU.CONSTRAINT_NAME, KCU.ORDINAL_POSITION";
		
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
				
				if ((lclFK == null) || (!lclFK.getName().equals(lclConstraintName))) {
//					System.out.println("Adding new key " + lclConstraintName);
					lclFKs.add(
						lclFK = new ForeignKey(
							new Key(
								new MySQLTableName(
									lclSourceSchema,
									lclSourceTable
								),
								lclConstraintName + "_SOURCE",
								false /* not required, for now */
							),
							new Key(
								new MySQLTableName(
									lclTargetSchema,
									lclTargetTable
								),
								lclConstraintName + "_TARGET",
								false /* not required, for now */
							),
							lclConstraintName,
							ReferentialAction.NO_ACTION, /* Delete.  TODO: Get this from the database. */
							ReferentialAction.NO_ACTION /* Update.  TODO: Get this from the database. */
						)
					);
				}
//				System.out.println("Adding column");
				lclFK.getSourceKey().getColumnNames().add(lclRS.getString("source_column_name"));
				lclFK.getTargetKey().getColumnNames().add(lclRS.getString("target_column_name"));
				
				if ("NO".equalsIgnoreCase(lclRS.getString("source_column_nullable"))) {
					++lclRequired;
				}
			}
			
			assert lclFK != null; // CHECK: Is it really true that lclFK can't be null here?
			
			// Check requiredness one last time for the last group
			if (lclRequired == lclFK.getSourceKey().getColumnNames().size()) {
				// THINK: Should this really be > 0?
				lclFK.getSourceKey().setRequired(true);
			}
			
//			Iterator<ForeignKey> lclI = lclFKs.iterator();
//			while (lclI.hasNext()) {
//				System.out.println(lclI.next());
//			}
			
//			System.out.println("About to return from loadForeignKeys");
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
				
				/* In MySQL, all primary keys are named "PRIMARY" so we can't tell that we have entered a 
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
						lclIndex = new MySQLIndex(
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
			MySQLIndex lclIndex = (MySQLIndex) lclI.next();
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
		lclSB.append("SELECT min(T.num) AS min, avg(T.num) AS avg, max(T.num) AS max, std(T.num) AS stdev ");
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
	
	@Override
	protected void determineTablesAndViews(Map<TableName, MappedClass> argMCs) throws SQLException {
		for (MappedClass lclMC : argMCs.values()) {
			lclMC.setTrueEntityType(EntityType.Table);
		}
	}

	@Override
	protected void generateGeneratedKeysMethodInternal(PrintWriter argPW, ClassMember argCM) {
		/* MySQL 5.1, currently in use by Nebula Capital (i.e., Tim) returns the generated key with a column
		 * name of "GENERATED_KEY" regardless of the name of the actual column.  This assumes that it
		 * will be the only column returned, which I hope will be slightly less brittle than assuming
		 * that the name of the returned column stays constant.  MySQL only allows one AUTO_INCREMENT
		 * column per table, which suggests that only one column could be returned.
		 */
		argPW.println("\t\t\targOpal." + argCM.getObjectMutatorName() + "(argRS.getInt(1));");		
	}

}
