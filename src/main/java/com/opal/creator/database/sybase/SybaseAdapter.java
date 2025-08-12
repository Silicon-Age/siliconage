package com.opal.creator.database.sybase;

import java.io.PrintWriter;
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
import com.siliconage.util.UnimplementedOperationException;
import com.siliconage.xml.XMLElement;

import com.opal.creator.ClassMember;
import com.opal.creator.MappedClass;
import com.opal.creator.database.ReferentialAction;
import static com.opal.creator.database.ReferentialAction.*;
import com.opal.creator.database.DatabaseColumn;
import com.opal.creator.database.EntityType;
import com.opal.creator.database.ForeignKey;
import com.opal.creator.database.Index;
import com.opal.creator.database.Key;
import com.opal.creator.database.PrimaryKey;
import com.opal.creator.database.RelationalDatabaseAdapter;
import com.opal.creator.database.TableName;

public class SybaseAdapter extends RelationalDatabaseAdapter {
	private static final int UNIQUE_FLAG = 2;
//	private static final int CLUSTERED = 16;
	private static final int ENFORCES_PRIMARY_KEY = 2048;
//	private static final int ENFORCES_UNIQUE_CONSTRAINT = 4096;

//	private static final String DATABASE_DRIVER_CLASS_NAME = "com.sybase.jdbc2.jdbc.SybDriver";
//	
	private static final String DATABASE_OPAL_PREFIX = "Sybase";
	private static final String DATABASE_PACKAGE_SUFFIX = "sybase";
	
	private String myDefaultDatabase;
	private String myDefaultOwner;
	
	public SybaseAdapter(DataSource argDataSource) {
		super(argDataSource);
	}
	
//	@Override
//	protected String getDatabaseDriverClassName() {
//		return DATABASE_DRIVER_CLASS_NAME;
//	}
	
	@Override
	protected String getDatabaseOpalPrefix() {
		return DATABASE_OPAL_PREFIX;
	}
	
	@Override
	protected String getDatabasePackageSuffix() {
		return DATABASE_PACKAGE_SUFFIX;
	}
	
	/* The following three methods are called by RelationalDatabaseAdapter when it is creating
	 * the database-specific factory class for a specific Opal (e.g. SQLServerWidgetFactory) to
	 * do database-specific tasks. */
	
	@Override
	protected void createAfterInsertMethod(PrintWriter argBW, MappedClass argMC) {
		ClassMember lclIdentityClassMember = null;
		
		Iterator<ClassMember> lclI = argMC.createClassMemberIterator();
		while (lclI.hasNext()) {
			ClassMember lclCM = lclI.next();
			if (((SybaseColumn) lclCM.getDatabaseColumn()).isIdentity()) {
				lclIdentityClassMember = lclCM;
				break;
			}
		}
		
		if (lclIdentityClassMember != null) {
			argBW.println("\tprotected void afterInsert(TransactionParameter argTP, Opal argOpal) throws PersistenceException {");
			argBW.println("\t\ttry {");
			argBW.println("\t\t\t((" + argMC.getOpalClassName() + ") argOpal)." + lclIdentityClassMember.getPrimitiveMutatorName() + "(");
			argBW.println("\t\t\t\tDatabaseUtility.executeIntQuery(");
			argBW.println("\t\t\t\t\t((DatabaseTransactionParameter) argTP).getConnection(),");
			argBW.println("\t\t\t\t\t\"SELECT @@IDENTITY AS id_value\",");
			argBW.println("\t\t\t\t\tnull");
			argBW.println("\t\t\t\t)");
			argBW.println("\t\t\t);");
			argBW.println("\t\t\treturn;");
			argBW.println("\t\t} catch (SQLException lclE) {");
			argBW.println("\t\t\tthrow new PersistenceException(\"Unable to retrieve identity column\", lclE);");
			argBW.println("\t\t}");
			argBW.println("\t}");
			argBW.println();
		}
	}
	
	@Override
	protected void createNewObjectMethod(PrintWriter argBW, MappedClass argMC) {
		argBW.println("\tprotected void newObject(Opal argOpal) {");
		argBW.println("\t\t" + argMC.getOpalClassName() + " lclOpal = (" + argMC.getOpalClassName() + ") argOpal;");
		argBW.println("\t\tapplyDefaults(lclOpal);");
		argBW.println("\t\treturn;");
		argBW.println("\t}");
		argBW.println();
		
		/* applyDefaults() */
		
		argBW.println("\tprotected void applyDefaults(" + argMC.getOpalClassName() + " argOpal) {");
		argBW.println("\t\treturn;");
		argBW.println("\t}");
		argBW.println();
	}
	
	@Override
	protected void createGetFullyQualifiedTableNameMethod(PrintWriter argBW, MappedClass argMC) {
		/* Note that this doesn't actually return a FQTN */
		
		argBW.println("\tprotected String getFullyQualifiedTableName() {");
		argBW.println("\t\treturn \"" + ((SybaseTableName) argMC.getTableName()).getTableName() + "\";");
		argBW.println("\t}");
		argBW.println();
	}
	
	private static final String OPAL_FACTORY_FACTORY_CLASS_NAME = "SQLServerOpalFactoryFactory";
	
	@Override
	protected String getOpalFactoryFactoryClassName() {
		return OPAL_FACTORY_FACTORY_CLASS_NAME;
	}
	
	@Override
	public SybaseTableName createTableName(Element argElement) {
		Validate.notNull(argElement);
		
		String lclDatabaseName = XMLElement.getAttributeValue(argElement, "Database");
		if (lclDatabaseName == null) {
			lclDatabaseName = getDefaultDatabase();
		}
		
		String lclOwnerName = XMLElement.getAttributeValue(argElement, "Owner");
		if (lclOwnerName == null) {
			lclOwnerName = getDefaultOwner();
		}
		
		String lclTableName = XMLElement.getAttributeValue(argElement, "Table");
		Validate.notNull(lclTableName, "Element does not supply a table name.");
		
		return new SybaseTableName(lclDatabaseName, lclOwnerName, lclTableName);
	}
	
	@Override
	public boolean isLargeDatabaseType(DatabaseColumn argDatabaseColumn) {
		Validate.notNull(argDatabaseColumn);
		return false;
	}
	
	@Override
	public Class<?> determineJavaType(DatabaseColumn argDatabaseColumn) {
		/* Other things to think about:  NVARCHAR2, NCHAR, NVARCHAR, UNDEFINED, SDO_DIM_ARRAY, SDO_GEOMETRY, ROWID */
		final String lclS = argDatabaseColumn.getDataType();
		
		/* FIXME:  1-char fields should be Character */
		/* FIXME:  Some fields should be Booleans */
		
		if (lclS.equals("varchar") || lclS.equals("nvarchar") || lclS.equals("nchar") || lclS.equals("char") || lclS.equals("text")) {
			return String.class;
		} else if (lclS.equals("datetime")) {
			return java.sql.Timestamp.class;
		} else if (lclS.equals("int")) {
			return Integer.class;
		} else if (lclS.equals("shortint")) {
			return Short.class;
		} else if (lclS.equals("tinyint")) {
			return Short.class; /* Byte is signed! */
		} else if (lclS.equals("bit")) {
			return Byte.class; /* ? */
		} else if (lclS.equals("decimal") || lclS.equals("numeric")) {
			if (argDatabaseColumn.getScale() > 0) {
				return Double.class; /* FIXME:  Should some of these be Floats? */
			} else {
				if (argDatabaseColumn.getPrecision() > 9) {
					return Long.class;
				} else {
					return Integer.class;
				}
			}
		} else if (lclS.equals("float")) {
			return Double.class;
		} else if (lclS.equals("real")) {
			return Float.class;
		} else if (lclS.equals("money")) {
			return Float.class;
		} else if (lclS.equals("smallmoney")) {
			return Float.class;
		} else if (lclS.equals("smalldatetime") || lclS.equals("cursor") || lclS.equals("timestamp") || lclS.equals("uniqueidentifier") || lclS.equals("binary") || lclS.equals("varbinary") || lclS.equals("image")) {
			throw new IllegalStateException("Unable to map data type " + lclS + "; big types are not currently supported.");
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
				System.out.println("Changing " + getDatabaseColumn().getName() + " to type " + myMemberType);
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
		setDefaultDatabase(XMLElement.getAttributeValue(argElement, "DefaultDatabase"));
		setDefaultOwner(XMLElement.getAttributeValue(argElement, "DefaultOwner"));
	}
	
	@Override
	protected ArrayList<DatabaseColumn> loadColumns() throws SQLException {
		String lclSQL = 
			"SELECT	C.TABLE_CATALOG, C.TABLE_SCHEMA, C.TABLE_NAME, C.COLUMN_NAME, C.COLUMN_DEFAULT, C.IS_NULLABLE, C.DATA_TYPE, C.CHARACTER_MAXIMUM_LENGTH, C.NUMERIC_PRECISION, C.NUMERIC_SCALE, C2.status " +
			"FROM	INFORMATION_SCHEMA.TABLES T, " +
				"INFORMATION_SCHEMA.COLUMNS C, " +
				"sysobjects T2, " +
				"syscolumns C2 " +
			"WHERE	T.TABLE_CATALOG = C.TABLE_CATALOG " +
				"AND T.TABLE_SCHEMA = C.TABLE_SCHEMA " +
				"AND T.TABLE_NAME = C.TABLE_NAME " +
				"AND T.TABLE_TYPE = 'BASE TABLE' " +
				"AND T.TABLE_NAME = T2.name " +
				"AND C.COLUMN_NAME = C2.name " +
				"AND T2.id = C2.id " +
			"ORDER BY C.TABLE_CATALOG, C.TABLE_SCHEMA, C.TABLE_NAME, C.ORDINAL_POSITION";

		ArrayList<DatabaseColumn> lclColumns = new ArrayList<>();
		
		ResultSet lclRS = null;
		
		try {
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);
			
			while (lclRS.next()) {
				String lclColumnName = lclRS.getString("COLUMN_NAME");
				
				DatabaseColumn lclDC = new SybaseColumn(
						new SybaseTableName (
							lclRS.getString("TABLE_CATALOG"),
							lclRS.getString("TABLE_SCHEMA"),
							lclRS.getString("TABLE_NAME")
						),
						lclColumnName,
						lclRS.getString("DATA_TYPE"),
						lclRS.getInt("CHARACTER_MAXIMUM_LENGTH"),
						lclRS.getInt("NUMERIC_PRECISION"),
						lclRS.getInt("NUMERIC_SCALE"),
						false, // FIXME: I'm not sure how to handle wide characters in modern versions of Sybase
						lclRS.getString("IS_NULLABLE").equalsIgnoreCase("YES"),
						null, /* TODO: lclRS.getString("COLUMN_DEFAULT"), */
						(lclRS.getInt("status") & 0x80) > 0 /* Is it an identity? */
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
		// TODO: Implement for Sybase
		return;
	}
	
	@Override
	protected java.util.ArrayList<ForeignKey> loadForeignKeys() throws java.sql.SQLException {
		String lclSQL =
			"select C.name as constraint_name, T1.name as source_table_name, T2.name as target_table_name, C1.name as source_column_name, C1.status&8 as source_column_nullable C2.name as target_column_name " +
			"from sysobjects C, sysobjects T1, sysobjects T2, syscolumns C1, syscolumns C2, sysforeignkeys FK " +
			"where FK.constid = C.id " +
				"and FK.fkeyid = T1.id " +
				"and FK.rkeyid = T2.id " +
				"and FK.fkeyid = C1.id " +
				"and FK.rkeyid = C2.id " +
				"and FK.fkey = C1.colid " +
				"and FK.rkey = C2.colid " +
			"order by FK.fkeyid, FK.rkeyid, keyno";
		
		ArrayList<ForeignKey> lclFKs = new ArrayList<>();
		
		ResultSet lclRS = null;
		
		try {
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
				
				ReferentialAction lclDeleteAction = NO_ACTION; // TODO: Determine these correctly
				ReferentialAction lclUpdateAction = NO_ACTION; // TODO: Determine these correctly
				
				/* This next command is pretty impressive. */
				if ((lclFK == null) || (!lclFK.getName().equals(lclConstraintName))) {
					lclFKs.add(
						lclFK = new ForeignKey(
							new Key(
								new SybaseTableName(
									getDefaultDatabase(),
									getDefaultOwner(),
									lclRS.getString("source_table_name")
								),
								lclConstraintName + "_SOURCE",
								false /* not required, for now */
							),
							new Key(
								new SybaseTableName(
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
			
			// Check requiredness one last time for the last group
			if (lclFK != null && lclRequired == lclFK.getSourceKey().getColumnNames().size()) {
				// THINK: Should this really be > 0?
				lclFK.getSourceKey().setRequired(true);
			}
			
/*			Iterator lclI = lclFKs.iterator();
			while (lclI.hasNext()) {
				System.out.println(lclI.next());
			}
*/			
			return lclFKs;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	@Override
	protected java.util.ArrayList<Index> loadIndexes() throws java.sql.SQLException {
		String lclSQL =
			"select SI.name as index_name, SI.status, T.name as table_name, C.name as column_name, IC.keyno " +
				"from sysobjects T, syscolumns C, sysindexes SI, sysindexkeys IC " +
				"where SI.id = T.id " +
					"and SI.id = C.id " +
					"and T.id = C.id " +
					"and SI.id = IC.id " +
					"and T.id = IC.id " +
					"and C.id = IC.id " +
					"and SI.indid = IC.indid " +
					"and IC.colid = C.colid " +
					"and SI.indid between 1 and 254 " +
				"order by SI.id, SI.indid, IC.keyno";
		
		ArrayList<Index> lclIndexes = new ArrayList<>();
		
		ResultSet lclRS = null;
		
		try {
			lclRS = DatabaseUtility.select(getDataSource().getConnection(), lclSQL);
			
			Index lclIndex = null;
			
			while (lclRS.next()) {
				String lclIndexName = lclRS.getString("index_name");
				
				/* This next command is pretty impressive. */
				if ((lclIndex == null) || (!lclIndex.getIndexName().equals(lclIndexName))) {
					lclIndexes.add(
						lclIndex = new SybaseIndex(
							new SybaseTableName(
								getDefaultDatabase(), /* FIXME:  Wrong! */
								getDefaultOwner(), /* FIXME:  Wrong! */
								lclRS.getString("table_name")
							),
							lclIndexName,
							(lclRS.getInt("status") & UNIQUE_FLAG) > 0,
							(lclRS.getInt("status") & ENFORCES_PRIMARY_KEY) > 0
						)
					);
				}
				lclIndex.getColumnNames().add(lclRS.getString("column_name"));
			}
			
			return lclIndexes;
		} finally {
			DatabaseUtility.cleanUp(lclRS);
		}
	}
	
	@Override
	protected ArrayList<PrimaryKey> loadPrimaryKeys() /* throws SQLException */ {
		Collection<Index> lclIndexes = getIndexes();
		
		ArrayList<PrimaryKey> lclPKs = new ArrayList<>();
		
		Iterator<Index> lclI = lclIndexes.iterator();
		while (lclI.hasNext()) {
			Index lclIndex = lclI.next();
			Validate.isTrue(lclIndex instanceof SybaseIndex);
			SybaseIndex lclSybaseINdex = (SybaseIndex) lclIndex;
			if (lclSybaseINdex.isPrimaryKey()) {
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
