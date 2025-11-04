package com.opal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

import com.opal.types.StringSerializable;
import com.opal.types.UTCDateTime;
import com.siliconage.database.DatabaseUtility;
import com.siliconage.util.UnimplementedOperationException;

/* This class provides a default implementation for OpalFactories that have the responsibilities of connecting to a specific
 * brand of database and building an Opal.  It extends AbstractOpalFactory by specializing that class' methods to work with
 * relational databases (as opposed to other types of persistent stores).
 * 
 * In general, the Opal framework will create one concrete subclass of AbstractDatabaseOpalFactory for each mapped table
 * and it will have a name like SQLServerWidgetOpalFactory that combines the name of the database (SQLServer) and that of
 * the mapped table (Widget).
 * 
 * Each subclass created will be a Singleton, which limits the amount of data that can be stored in this class. 
 */

public abstract class AbstractDatabaseIdentityOpalFactory<U extends IdentityUserFacing, O extends IdentityOpal<U>> extends AbstractIdentityOpalFactory<U, O> {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(AbstractDatabaseIdentityOpalFactory.class.getName());
	
	protected AbstractDatabaseIdentityOpalFactory() {
		super();
	}
	
	protected abstract DataSource getDataSource();
	
	/* The instantiate methods are responsible for creating an Opal object and populating it with an array of values loaded
	 * from the persistent store.  These arrays have generally been created by populateValueArray().  An instantiate method
	 * will typically call invoke the constructor for the WidgetOpal and pass in the array of values.
	 * 
	 * When instantiate is used to create a new Opal (i.e., one that doesn't exist in the persistent store), a null value is
	 * passed to argValues (rather than, say, an array of nulls.).
	 */

	protected abstract O instantiate(Object[] argValues);
	
	/* This method takes a ResultSet representing a complete column set (i.e., "SELECT * FROM") for this Opal's underlying
	 * database table and plucks out the values needed to create an OpalKey for that table's primary key.  This is called
	 * when loading multiple Opals from a query; for each row, we need to see if the Opal is already from the cache, so we
	 * construct the key from the ResultSet, check the cache, and, if it's not there, then go ahead and instantiate the
	 * Opal using the same ResultSet.  (Most of that work, of course, takes place in other methods; this one only creates
	 * the appropriate OpalKey.
	 */
	protected abstract OpalKey<O> createOpalKeyForRow(ResultSet argRS) throws SQLException;
	
	protected abstract String[] getPrimaryKeyWhereClauseColumns();
	
	/* deleteInternal is called by AbstractOpalFactory.delete() to actually delete the data from the persistent store.  That is,
	 * delete() takes care of Opal-specific aspects of deletion (like removing keys from the OpalCache), but only OpalFactories
	 * specific to a type of store (in this case, a relational database since we are in Abstract_Database_OpalFactory) will
	 * know what has to be done to remove the information from the store.
	 * 
	 * delete will actually call the version that takes a TransactionParameter, but that method will cast that to a
	 * DatabaseTransactionParameter (so the Connection is available) and pass that to the overridden method that takes a
	 * DatabaseTransactionParameter.
	 */
	
	@Override
	protected final void deleteInternal(TransactionParameter argTP, O argOpal) {
		assert argTP != null;
		assert argOpal != null;
		deleteInternal((DatabaseTransactionParameter) argTP, argOpal);
	}
	
	/* This override for deleteInternal does the actual work of issuing a SQL DELETE statement for this Opal's row. 
	 */
	@SuppressWarnings("resource") // We are not responsible for closing argDTP's Connection object
	protected void deleteInternal(DatabaseTransactionParameter argDTP, O argOpal) {
		if (argOpal.isNew()) {
			return;
		}
		
		try {
			int lclResult = DatabaseUtility.delete(
					argDTP.getConnection(),
					getFullyQualifiedTableName(),
					getPrimaryKeyWhereClauseColumns(),
					argOpal.getPrimaryKeyWhereClauseValues()
					);

			if (lclResult != 1) {
				ourLogger.warn("DELETE SQL for " + argOpal + " resulted in " + lclResult + " rows being changed.");
			}
		} catch (SQLException lclE) {
			throw new PersistenceException("Could not delete " + this, lclE);
		}
	}
	
	/* This method converts an array of column names into a comma-separated list.
	 */
	protected static String generateColumnNamesList(String[] argColumnNames) {
		int lclLength = argColumnNames.length;
		StringBuilder lclSB = new StringBuilder(16 * lclLength);
		for (int lclI = 0; lclI < lclLength; ++lclI) {
			if (lclI > 0) {
				lclSB.append(", ");
			}
			lclSB.append(argColumnNames[lclI]);
		}
		return lclSB.toString();
	}
	
	/* This static method constructs a SQL statement that returns certain columns of the rows of a table that have
	 * specific values for specific columns.  That is, it builds a statement of this form:
	 * 
	 * SELECT <column_list>
	 * FROM <table>
	 * WHERE column_1 = value_1 
	 *   AND column_2 = value_2
	 *   ...
	 *   AND column_3 = value_3
	 *   
	 * It may also add an ORDER BY clause to the end.  It then executes that statement on a provided
	 * Connection and returns the ResultSet to the user.
	 * 
	 * If the list of columns to select is null, it does a SELECT * to get all columns.
	 * 
	 * This method cannot be used to construct WHERE clauses with any greater variety that simple equality for zero
	 * or more columns.
	 * 
	 * Generally speaking, this method is used to load rows referenced by foreign keys.
	 * 
	 * The handling of null parameters is poorly tested.
	 * 
	 * This method does not attempt to pass along information about whether its (string-y) parameters are ASCII or not.
	 */
	@SuppressWarnings("resource") // We are not responsible for closing argConnection
	protected static final ResultSet generateResultSet(Connection argConnection, String argFullyQualifiedTableName, String[] argColumnNames, String[] argWhereClauseColumnNames, Object[] argParameters, String argOrderBy) throws SQLException {
		Validate.notNull(argConnection);
		Validate.notNull(argFullyQualifiedTableName);
		Validate.isTrue((argWhereClauseColumnNames != null) ? (argParameters != null && argParameters.length == argWhereClauseColumnNames.length) : (argParameters == null));
		
		StringBuilder lclSB = new StringBuilder(512);
		if (argColumnNames == null) {
			lclSB.append("SELECT * FROM ");
		} else {
			lclSB.append("SELECT ");
			lclSB.append(generateColumnNamesList(argColumnNames));
			lclSB.append(" FROM ");
		}
		lclSB.append(argFullyQualifiedTableName);
		
		int lclNonNullParameterCount = 0;
		
		Object[] lclParameters;
		
		if (argWhereClauseColumnNames != null) {
			assert argParameters != null;
			lclSB.append(" WHERE ");
			for (int lclI = 0; lclI < argWhereClauseColumnNames.length; ++lclI) {
				if (lclI > 0) {
					lclSB.append(" AND ");
				}
				lclSB.append(argWhereClauseColumnNames[lclI]);
				if (argParameters[lclI] != null) {
					lclSB.append(" = ?");
					++lclNonNullParameterCount;
				} else {
					lclSB.append(" IS NULL");
				}
			}
			if (lclNonNullParameterCount == 0) {
				lclParameters = null;
			} else {
				if (lclNonNullParameterCount == argParameters.length) {
					lclParameters = argParameters;
				} else {
					lclParameters = new Object[lclNonNullParameterCount];
					int lclIndex = 0;
					for (int lclI = 0; lclI < argParameters.length; ++lclI) {
						if (argParameters[lclI] != null) {
							lclParameters[lclIndex++] = argParameters[lclI];
						}
					}
					Validate.isTrue(lclNonNullParameterCount == lclIndex);
				}
			}
		} else {
			lclParameters = null;
		}
		
		if (argOrderBy != null) {
			lclSB.append(" ORDER BY ");
			lclSB.append(argOrderBy);
		}
		
		adjustParameters(lclParameters);
//		/* FIXME: Why doesn't this need to deal with OffsetDateTimes? */
//		if (lclParameters != null) {
//			Validate.isTrue(argWhereClauseColumnNames != null);
//			for (int lclI = 0; lclI < lclParameters.length; ++lclI) {
//				Object lclO = lclParameters[lclI];
//				if (lclO instanceof java.time.LocalDate lclLD) {
//					lclParameters[lclI] = java.sql.Date.valueOf(lclLD);
//				} else if (lclO instanceof java.time.LocalDateTime lclLDT){
//					lclParameters[lclI] = java.sql.Timestamp.valueOf(lclLDT);
//				} else if (lclO instanceof com.opal.types.UTCDateTime lclUDT){
//					lclParameters[lclI] = java.sql.Timestamp.valueOf((lclUDT).toLocalDateTime());
//				} else if (lclO instanceof StringSerializable lclSS) {
//					lclParameters[lclI] = lclSS.toSerializedString();
//				} else {
//					// No conversion required
//				}
//			}
//		}
		
//		System.out.println("About to call select");
		ResultSet lclRS = DatabaseUtility.select(argConnection, lclSB.toString(), lclParameters);
//		System.out.println("Done with select");
		
		return lclRS;
	}
	
	/* This method returns the Opal-specific array of column names in the Opal's underlying table.
	 */
	protected abstract String[] getColumnNames();
	
	/* This method returns the Opal-specific array of member types used by the fields taken from the
	 * database.
	 */
	@Override
	protected abstract Class<?>[] getFieldTypes();
	
	/* This method returns the fully qualified, Opal-specific name of the underlying table.  This is
	 * a name that can be used to access the table regardless of whatever the "current schema" is for
	 * a Connection.
	 */
	protected abstract String getFullyQualifiedTableName();
	
	/* insertInternal is responsible for handling the tasks specific to this kind of persistence 
	 * engine (in this case a generic relational database) that occur when an Opal is inserted.  For
	 * convenience, all insertInternal does is cast its TransactionParameter to a DatabaseTransactionParameter
	 * and pass it to an overloaded method.
	 */
	@Override
	protected void insertInternal(TransactionParameter argTP, O argOpal) throws PersistenceException {
		insertInternal((DatabaseTransactionParameter) argTP, argOpal);
		return;
	}
	
	protected boolean hasGeneratedKeys() {
		return false;
	}
	
	protected boolean hasComplicatedDefaults() {
		return false;
	}
	
	protected String[] getComplicatedDefaultColumns() {
		return null;
	}
	
	protected void processGeneratedKeys(@SuppressWarnings("unused") ResultSet argRS, @SuppressWarnings("unused") O argOpal) {
		throw new UnimplementedOperationException();
	}
	
	/* insertInternal does the real work of inserting an Opal into a (generic) relational database.
	 * In particular, it makes a Map of the values that need to be inserted and passes those to a
	 * utility method in DatabaseUtility to build and execute the actual SQL string.  It uses the
	 * Connection passed around in the DatabaseTransactionParameter.
	 */
	@SuppressWarnings("resource") // We are not responsible for closing argDTP's Connection (which we use)
	protected void insertInternal(DatabaseTransactionParameter argDTP, O argOpal) throws PersistenceException {
		assert argDTP != null;
		assert argOpal != null;
		
		/* This map holds the (column name, value) pairs that are to be inserted. */
		Map<String, Object> lclMap = new HashMap<>(); // Maybe some sort of faster map?
		
		/* Populate that map. */
		populateInsertMap(lclMap, argOpal);
		
		/* Are we setting zero fields? */
		if (lclMap.size() == 0) {
			/* Yes.  That's anomalous enough to warrant a warning.  However, this could happen legitimately
			 * for a table whose only columns were automatically populated by sequences or other defaults. 
			 */
			ourLogger.warn("SKM populated with 0 changed fields for " + this + " on insert.");
		}
		
		try {
			/* Call DatabaseUtility.insert which will construct and execute a SQL INSERT statement that uses
			 * the values in lclMap to insert a row into the specified table.  
			 */
			if (hasComplicatedDefaults()) {
				DatabaseUtility.insertWithOutput(
						argDTP.getConnection(),
						getFullyQualifiedTableName(),
						lclMap,
						getComplicatedDefaultColumns(),
						x -> processGeneratedKeys(x, argOpal)
						);
			} else if (hasGeneratedKeys()) {
				DatabaseUtility.insert(
						argDTP.getConnection(),
						getFullyQualifiedTableName(),
						lclMap,
						x -> processGeneratedKeys(x, argOpal)
						);
			} else {
				DatabaseUtility.insert(
						argDTP.getConnection(),
						getFullyQualifiedTableName(),
						lclMap
						);
			}
			return;
		} catch (SQLException lclE) {
			throw new PersistenceException("Unable to insert " + argOpal + " with data " + lclMap, lclE);
		}
	}
	
	/* The load method is specific to factories for Opals backed by a (generic) relational database.  It is called to
	 * acquire a Collection of Opals from a table based on a query like:
	 * 
	 *  SELECT * FROM <table> WHERE column_1 = value_1 AND column_2 = value_2
	 *  
	 *  This method is only currently used to generate the Collection of objects that match a foreign key; that is
	 *  if A.b_id is a reference to B.id, then this method is invoked by a BOpalFactory to find the Collection of
	 *  AOpals that are linked to a given B.
	 *  
	 *  This method grabs a Connection from the OpalFactory's DataSource and passes it to an overload of the load(...)
	 *  method to do the actual work.
	 *  
	 *  The resulting Opals are added to argCollection (without clearing its prior contents).
	 */
	protected final void load(String argFullyQualifiedTableName, String[] argFieldNames, Object[] argParameters, String argOrderBy, Collection<O> argCollection) {
		assert argFullyQualifiedTableName != null;
		assert argFieldNames != null;
		assert argCollection != null;
		assert argFieldNames.length == argParameters.length;
		
		Connection lclConnection = null;
		
		try {
			lclConnection = getDataSource().getConnection();
			
			load(lclConnection, argFullyQualifiedTableName, argFieldNames, argParameters, argOrderBy, argCollection);
			
			return;
		} catch (SQLException lclE) {
			throw new PersistenceException("Could not load", lclE);
		} finally {
			DatabaseUtility.closeConnection(lclConnection);
		}
	}
	
	/* For the general purpose of this method, see the documentation of the immediately preceding method.  It creates
	 * a query to load a subset of rows of the specified table, runs that query on the provided connection, and adds
	 * the Opals corresponding to those rows to the provided Collection (without clearing that Collection first).
	 */
	@SuppressWarnings("resource") // We are not responsible for closing argConnection and lclRS will be properly closed by the finally clause
	protected final void load(Connection argConnection, String argFullyQualifiedTableName, String[] argFieldNames, Object[] argParameters, String argOrderBy, Collection<O> argCollection) throws SQLException {
//		long lclStart = System.currentTimeMillis();
		ResultSet lclRS = null;
		try {
			/* Construct the SQL statement identify the rows, turn it into a PreparedStatement, execute it, and grab
			 * the resulting ResultSet.
			 */
			lclRS = generateResultSet(argConnection, argFullyQualifiedTableName, getColumnNames(), argFieldNames, argParameters, argOrderBy);
			
			/* Which should never be null. */
			Validate.notNull(lclRS);
			
			/* Convert that ResultSet into Opals (either by looking them up in the OpalCache or by constructing new Opals
			 * from the data in it, then add them to argCollection.
			 */
			acquireFromResultSet(lclRS, argCollection, true);
		} finally {
			DatabaseUtility.cleanUp(lclRS, DatabaseUtility.CLEAN_STATEMENT);
//			long lclEnd = System.currentTimeMillis();
//			StringBuilder lclSB = new StringBuilder();
//			for (int lclI = 0; lclI < argFieldNames.length; ++lclI) {
//				if (lclI > 0) {
//					lclSB.append('/');
//				}
//				lclSB.append(argFieldNames[lclI]);
//			}
//			DatabaseUtility.ourTally.tally("Object construction for " + getFullyQualifiedTableName() + " for " + lclSB.toString(), lclEnd - lclStart);
		}
	}
	
	/* This method adds every Opal in the Factory's underlying table to the specific Collection (without clearing the
	 * Collection first).
	 */
	protected final Set<O> getAll(Connection argConnection) throws SQLException {
		assert argConnection != null;
		
		ResultSet lclRS = null;
		// long lclStart = System.currentTimeMillis();
		try {
			// System.out.println("About to generate ResultSet");
			
			/* Generate a ResultSet that loads every column of every row in the table without using a WHERE clause. 
			 */
			lclRS = generateResultSet(argConnection, getFullyQualifiedTableName(), getColumnNames(), null, null, null);
			// System.out.println("Done with generate ResultSet; about to acquireFromResultSet");
			/* Convert those rows into their Opals.  This will involve looking to see whether each one is already in
			 * the cache and, if not, creating it from the data in the ResultSet.
			 */
			return getFromResultSet(lclRS, true);
		} finally {
			DatabaseUtility.cleanUp(lclRS, DatabaseUtility.CLEAN_STATEMENT);
			// long lclEnd = System.currentTimeMillis();
			// DatabaseUtility.ourTally.tally("Acquire all for " + getFullyQualifiedTableName(), lclEnd - lclStart);
		}
	}
	
	/* This method executes a specific SQL SELECT statement.  Then it turns each row of the ResultSet into an
	 * Opal and adds them to the specified Collection (without clearing the Collection first).  The provided SQL
	 * statement must return *all* of the (mapped) columns for this Opal using their original names.  It is
	 * okay to return additional columns (even from different tables), so long as there are no name collisions.
	 */
	protected final void acquireForSQL(Connection argConnection, Collection<O> argCollection, String argSQL, Object[] argParameters) throws SQLException {
//		long lclStart = System.currentTimeMillis();
		assert argConnection != null;
		assert argSQL != null;
		assert argCollection != null;
		
		ResultSet lclRS = null;
		try {
			/* Generate a ResultSet using the specified SQL statement and parameters. */
			lclRS = DatabaseUtility.select(argConnection, argSQL, argParameters);
			
			/* Convert those rows into their Opals.  This will involve looking to see whether each one is already in
			 * the cache and, if not, creating it from the data in the ResultSet.
			 */
			acquireFromResultSet(lclRS, argCollection, false);
			
			return;
		} finally {
			DatabaseUtility.cleanUp(lclRS, DatabaseUtility.CLEAN_STATEMENT);
//			long lclEnd = System.currentTimeMillis();
//			DatabaseUtility.ourTally.tally("Acquiring for " + argSQL, lclEnd - lclStart);
		}
	}
	
	protected final Set<O> getFromResultSet(ResultSet argRS, boolean argCanonicalColumnOrder) throws SQLException {
		Set<O> lclResults = new HashSet<>();
		
		acquireFromResultSet(argRS, lclResults, argCanonicalColumnOrder);
		
		return lclResults;
	}
	
	protected final void acquireFromResultSet(ResultSet argRS, Collection<O> argCollection, boolean argCanonicalColumnOrder) throws SQLException {
		assert argRS != null;
		assert argCollection != null;
		
//		long lclA = System.currentTimeMillis();
		
		final OpalCache<O> lclOC = getCache();
		
//		long lclB = System.currentTimeMillis();
//		System.out.println("A3 to B3 = " + (lclB - lclA) + " ms.");
//		long lclE = lclB;
		
		while (argRS.next()) {
//			long lclC = System.currentTimeMillis();
//			System.out.println("B3/E3 to C3 = " + (lclC - lclB) + " ms.");
			
			/* We are about to instantiate Opals for the rows returned by the query, we do this by
			 * calling extractSingleOpalFromResultSet which will create the Opals if they aren't
			 * already in the OpalCache. */
			
//			System.out.println("About to extractSingleOpal");
			O lclOpal = extractSingleOpalFromResultSet(lclOC, argRS, argCanonicalColumnOrder);
//			System.out.println("Extracted " + lclOpal);
//			long lclD = System.currentTimeMillis();
//			System.out.println("C3 to D3 = " + (lclD - lclC) + " ms.");
			
			/* Now add the object to the collection. */
			
			argCollection.add(lclOpal);
			
//			lclE = System.currentTimeMillis();
//			System.out.println("D3 to E3 = " + (lclE - lclD) + " ms.");
			
			/* FEATURE:  Must deal with the issue of whether the current thread sees (is searching
			on) the new values or the old values.  That will suck. */
			
//			lclB = lclE;
		}
		
//		long lclF = System.currentTimeMillis();
//		System.out.println("E3 to F3 = " + (lclF - lclE) + " ms.");
	
//		System.out.println("acquireFromResultSet took " + (lclF - lclA) + " ms.");
		return;
	}
	
	@SuppressWarnings("resource") // We are not responsible for closing argRS
	protected final O extractSingleOpalFromResultSet(OpalCache<O> argOC, ResultSet argRS, boolean argCanonicalColumnOrder) throws SQLException {
		Validate.notNull(argOC);
		Validate.notNull(argRS);
		/* We are about to instantiate an Opal for the row returned by the query, but before
		we create a new object, we need to make sure that an object for that rows hasn't already
		been created.  We do this by creating a key from the database row and checking with the
		OpalCache to see if it is already in there. */
		
		/* Create an OpalKey representing the primary key for the row (this is done by an
		abstract method that creates a different class for each Opal. */
		
//		long lclA = System.currentTimeMillis();
//		System.out.println("About to createOpalKeyForRow in " + this);
		OpalKey<O> lclOpalKey = createOpalKeyForRow(argRS);
//		System.out.println("Done with createOpalKeyForRow; lclOpalKey = " + lclOpalKey);
//		long lclB = System.currentTimeMillis();
//		System.out.println("A2 to B2 = " + (lclB - lclA) + " ms.");
		/* See if an object with that key is already in the cache. */
		
		O lclOpal;
		
//		long lclH;
		
		synchronized (argOC) {
//			long lclC = System.currentTimeMillis();
//			System.out.println("B2 to C2 = " + (lclC - lclB) + " ms.");
			lclOpal = argOC.forOpalKey(lclOpalKey);
//			long lclD = System.currentTimeMillis();
//			System.out.println("C2 to D2 = " + (lclD - lclC) + " ms.");
			
			if (lclOpal == null) { /* No, there isn't */
				Object[] lclValues = new Object[getFieldCount()];
//				long lclE = System.currentTimeMillis();
//				System.out.println("D2 to E2 = " + (lclE - lclD) + " ms.");
				populateValueArrayFromResultSet(lclValues, argRS, argCanonicalColumnOrder);
//				long lclF = System.currentTimeMillis();
//				System.out.println("E2 to F2 = " + (lclF - lclE) + " ms.");
//				System.out.println("About to create(lclValues) in " + this);
				lclOpal = instantiate(lclValues);
//				System.out.println("Done with create(lclValues) in " + this);
//				long lclG = System.currentTimeMillis();
//				System.out.println("F2 to G2 = " + (lclG - lclF) + " ms.");
//				ourLogger.debug("Loaded " + lclOpal + " for " + lclOpalKey + "; adding to " + argOC);
				registerOldOpal(lclOpal);
//				lclH = System.currentTimeMillis();
//				System.out.println("G2 to H2 = " + (lclH - lclG) + " ms.");
				
				assert lclOpal.getUserFacing() == null;
				
				determineUserFacing(lclOpal, false);
				
			} else {
//				ourLogger.debug("Found " + lclOpal + " for " + lclOpalKey + " in cache " + argOC);
				/* Yes, there already is one.  We don't have to do anything since we've
				 * already gotten a reference to it which we will return rather than
				 * building a new object from the database. */
//				lclH = System.currentTimeMillis();
			}
		}
//		long lclI = System.currentTimeMillis();
//		System.out.println("H2 to I2 = " + (lclI - lclH) + " ms.");
		
		return lclOpal;
	}
	
	@Override
	public Set<O> getAll() throws PersistenceException {
		try (Connection lclConnection = getDataSource().getConnection()) {
			return getAll(lclConnection);
		} catch (SQLException lclE) {
			throw new PersistenceException("Could not acquire all Opals", lclE);
		}
	}
	
	/* In general, the Java Objects that store that field/column values of Opals are passed directly into
	 * the parameters of SQL (prepared) Statements.  However, some classes that we present to the user (and
	 * the user uses to construct queries) can't be used in that way.  This method runs through an array
	 * of parameters containing programmer-facing values (e.g., LocalDates) and converts them into the objects
	 * that need to be used as part of a SQL query (in this case, java.sql.Date).
	 * 
	 * It might be faster to check for the more common datatypes (e.g., Strings/Numbers) to avoid the majority
	 * of these instanceof tests.  On the other hand, I haven't profiled the code to see that this is really
	 * a bottleneck.
	 */
	protected static void adjustParameters(Object[] argParameters) {
		if (argParameters == null) {
			return;
		}
		for (int lclI = 0; lclI < argParameters.length; ++lclI) {
			Object lclO = argParameters[lclI];
			argParameters[lclI] = switch (lclO) {
			case LocalDate lclLD -> java.sql.Date.valueOf(lclLD);
			case LocalTime lclLT -> java.sql.Time.valueOf(lclLT);
			case LocalDateTime lclLDT -> java.sql.Timestamp.valueOf(lclLDT);
			case UTCDateTime lclUDT -> java.sql.Timestamp.valueOf((lclUDT).toLocalDateTime()); // The local time in UTC
			/* The following conversion is problematic, as the actual time zone of the OffsetDateTime is lost.  This
			 * doesn't matter for the motivating use case of NAQT, but it will almost certainly cause problems
			 * in other contexts.  However, there's not really a good SQL Server type to store a local date/time
			 * and a time zone.  We'd need Opal to understand how to break this apart and store the components
			 * in two separate columns (possibly without a good standard for how to represent the time zone).
			 */
			case OffsetDateTime lclODT -> java.sql.Timestamp.valueOf((lclODT).toLocalDateTime()); // FIXME: Loses zone data
			case StringSerializable lclSS -> lclSS.toSerializedString();
			default -> lclO;
			};
		}
	}
	
	protected ResultSet createResultSet(Connection argConnection, Query argQuery) throws SQLException {
		if (argQuery instanceof ImplicitTableDatabaseQuery lclITDQ) {
			Object[] lclParameters = lclITDQ.getParameters();
			adjustParameters(lclParameters);
			
			return DatabaseUtility.select(
					argConnection,
					"SELECT " +
					generateColumnNamesList(getColumnNames()) +
					" FROM " + getFullyQualifiedTableName() + ' ' +
					"WHERE " + lclITDQ.getSQL(),
					lclParameters
					);
		} else if (argQuery instanceof DatabaseQuery lclDQ) {
			Object[] lclParameters = lclDQ.getParameters();
			adjustParameters(lclParameters);
			
			return DatabaseUtility.select(
					argConnection,
					lclDQ.getSQL(),
					lclParameters
					);
		} else {
			throw new IllegalArgumentException("This database-driven OpalFactory only understands DatabaseQuery objects; it was passed a " + argQuery.getClass().getName() + " that stringifies as \"" + argQuery + "\"");
		}
	}
	
	@Override
	public void acquireForQuery(Collection<O> argCollection, Query argQuery) throws PersistenceException {
//		long lclA = System.currentTimeMillis();
		
		Validate.notNull(argCollection);
		Validate.notNull(argQuery);
		
		Connection lclConnection = null;
		ResultSet lclRS = null;
//		long lclE = 0;
		try {
//			long lclB = System.currentTimeMillis();
//			System.out.println("A to B = " + (lclB - lclA) + " ms.");
			lclConnection = getDataSource().getConnection();
//			long lclC = System.currentTimeMillis();
//			System.out.println("B to C = " + (lclC - lclB) + " ms.");
			lclRS = createResultSet(lclConnection, argQuery);
//			long lclD = System.currentTimeMillis();
//			System.out.println("C to D = " + (lclD - lclC) + " ms.");
			acquireFromResultSet(lclRS, argCollection, ((DatabaseQuery) argQuery).areColumnsInCanonicalOrder());
//			lclE = System.currentTimeMillis();
//			System.out.println("D to E = " + (lclE - lclD) + " ms.");
		} catch (SQLException lclEx) {
			throw new PersistenceException("Could not acquire opals for the query \"" + argQuery.getClass().getName() + "\" that stringifies as \"" + argQuery.toString() + "\"", lclEx);
		} finally {
			DatabaseUtility.cleanUp(lclRS, lclConnection);
//			long lclF = System.currentTimeMillis();
//			System.out.println("E to F = " + (lclF - lclE) + " ms.");
//			com.siliconage.database.DatabaseUtility.ourTally.tally("Acquiring from " + getFullyQualifiedTableName() + " for " + argQuery, lclF - lclA);
//			long lclG = System.currentTimeMillis();
//			System.out.println("F to G = " + (lclG - lclF) + " ms.");
		}
	}
	
	@Override
	protected Object[] loadValuesFromPersistentStore(OpalKey<O> argOpalKey) {
		@SuppressWarnings("unchecked")
		DatabaseOpalKey<O> lclKey = (DatabaseOpalKey<O>) argOpalKey;
		return loadValuesFromPersistentStore(lclKey);
	}
	
	protected Object[] loadValuesFromPersistentStore(DatabaseOpalKey<O> argDOK) {
//		long lclStart = System.currentTimeMillis();
		Connection lclConnection = null;
		ResultSet lclRS = null;
		try {
			lclConnection = getDataSource().getConnection();
			
//			System.out.println("About to generateResultSet for " + argDOK);
			lclRS = generateResultSet(
					lclConnection,
					getFullyQualifiedTableName(),
					getColumnNames(),
					argDOK.getColumnNames(),
					argDOK.getParameters(),
					null /* ORDER BY */
					);
//			System.out.println("Done with generateResultSet");
			
			if (!lclRS.next()) {
//				System.out.println("lclRS.next() immediately returned false");
				return null;
			}
			
			Object[] lclValues = new Object[getFieldCount()];
			
//			System.out.println("About to populateValueArrayFromResultSet");
			populateValueArrayFromResultSet(lclValues, lclRS, true);
//			System.out.println("Done with populateValueArrayFromResultSet");
			
			if (lclRS.next()) {
				throw new IllegalStateException("Ostensibly unique key returned multiple rows.");
			}
			
			return lclValues;
		} catch (SQLException lclE) {
			throw new PersistenceException("Could not load values from persistent store for key " + argDOK, lclE);
		} finally {
			DatabaseUtility.cleanUp(lclRS, lclConnection);
//			long lclEnd = System.currentTimeMillis();
//			StringBuilder lclSB = new StringBuilder();
//			for (int lclI = 0; lclI < argDOK.getColumnNames().length; ++lclI) {
//				if (lclI > 0) {
//					lclSB.append('/');
//				}
//				lclSB.append(argDOK.getColumnNames()[lclI]);
//			}
//			com.siliconage.database.DatabaseUtility.ourTally.tally("Load from " + getFullyQualifiedTableName() + " for " + lclSB.toString(), lclEnd - lclStart); 
		}
	}
		
	protected O loadFromPersistentStore(DatabaseOpalKey<O> argDOK /*, boolean argConcrete */) throws PersistenceException {
		assert argDOK != null;
		
//		System.out.println("About to loadValuesFromPersistentStore in " + this);
		Object[] lclValues = loadValuesFromPersistentStore(argDOK);
//		System.out.println("Done with loadValuesFromPersistentStore in " + this);
		if (lclValues == null) {
			return null;
		}
		
//		System.out.println("About to create(lclValues) in " + this);
		O lclOpal = /* argConcrete ? instantiateConcrete(lclValues) : */ instantiate(lclValues);
//		System.out.println("Done with create(lclValues) in " + this);
		
		return lclOpal;
	}
	
	@Override
	protected O loadFromPersistentStore(OpalKey<O> argOK) throws PersistenceException {
		@SuppressWarnings("unchecked")
		DatabaseOpalKey<O> lclKey = (DatabaseOpalKey<O>) argOK;
		return loadFromPersistentStore(lclKey);
	}
	
	protected void populateInsertMap(Map<String, Object> argMap, O argOpal) {
		assert argMap != null;
		assert argOpal != null;
		
		argOpal.translateReferencesToFields(); /* THINK: Is this the right place to do this?  Probably not. -- 82*/
		
		if (!(argOpal instanceof UpdatableOpal<?>)) {
			if (ourLogger.isInfoEnabled()) {
				ourLogger.info("Passed Opal of class " + argOpal.getClass().getName() + " which is not updatable; ignoring.");
			}
			return;
		}
		
		UpdatableOpal<?> lclOpal = (UpdatableOpal<?>) argOpal;
		String[] lclFieldNames = getColumnNames();
		int lclI = lclFieldNames.length;
		
		Object[] lclOldValues = lclOpal.getOldValues();
		Object[] lclNewValues = lclOpal.getNewValues();
		
		if (lclOldValues == null) {
//			System.out.println("Doing an insert on " + System.identityHashCode(argOpal));
			/* This is an insert */
			
			/* FIXME: Can we merge this parameter adjustment with the above ones? */
			while ((--lclI) >= 0) {
				Object lclNewValue = lclNewValues[lclI];
				if (lclNewValue != null) {
//					System.out.println(lclFieldNames[lclI] + " -> " + lclNewValue);
					Object lclDatabaseValue = translateJavaObjectToSQLValue(lclNewValue);
					argMap.put(lclFieldNames[lclI], lclDatabaseValue);
				}
			}
//			System.out.println();
		} else {
//			System.out.println("Doing an update on " + System.identityHashCode(argOpal));
			/* This is an update */
			while ((--lclI) >= 0) {
				Object lclOldValue = lclOldValues[lclI];
				Object lclNewValue = lclNewValues[lclI];
				if ((lclOldValue == null && lclNewValue != null) || ((lclOldValue != null) && !(lclOldValue.equals(lclNewValue)))) {
//					System.out.println(lclFieldNames[lclI] + " -> " + lclNewValue);
					Object lclDatabaseValue = translateJavaObjectToSQLValue(lclNewValue);
					argMap.put(lclFieldNames[lclI], lclDatabaseValue);
				}
			}
//			System.out.println();
		}
		return;
	}
	
	/* This method has essentially the same content as adjustParameters, except it processes a single value at a
	 * time.  I don't remember why it was broken out; I suspect that adjustParameters could be re-implemented in terms
	 * of this method.
	 */
	protected Object translateJavaObjectToSQLValue(Object argO) { // THINK: Is this actually really slow?  Should we check Strings and Numbers first?
		assert argO != null;
		Object lclDatabaseValue;
		if (argO instanceof LocalDate lclLD) {
			lclDatabaseValue = java.sql.Date.valueOf(lclLD);
		} else if (argO instanceof LocalTime lclLT) {
			lclDatabaseValue = java.sql.Time.valueOf(lclLT);
		} else if (argO instanceof LocalDateTime lclLDT) {
			lclDatabaseValue = java.sql.Timestamp.valueOf(lclLDT);
		} else if (argO instanceof UTCDateTime lclUDT) {
			lclDatabaseValue = java.sql.Timestamp.valueOf((lclUDT).toLocalDateTime());
		} else if (argO instanceof OffsetDateTime lclODT) {
//			lclDatabaseValue = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format((OffsetDateTime) argO);
			/* Note that this conversion loses the Zone data.  In effect, it assumes that the user only wants to store the local
			 * date and time values in the database.  This is probably wrong in most cases (but correct for the specific NAQT
			 * application that led to this datatype being supported), but most databases don't have "zoned datetime" columns,
			 * so properly supporting this would require the ability to map multiple database columns into a single composite
			 * Java object when loading.  We probably want that eventually, but it's going to be a pain.
			 */
			lclDatabaseValue = java.sql.Timestamp.valueOf((lclODT).toLocalDateTime());
		} else if (argO instanceof StringSerializable lclSS) {
			lclDatabaseValue = lclSS.toSerializedString();
		} else {
			lclDatabaseValue = argO;
		}
		return lclDatabaseValue;
	}
	
	protected void populateUpdateMap(Map<String, Object> argMap, O argOpal) {
		populateInsertMap(argMap, argOpal);
		return;
	}
	
	protected void populateValueArrayFromResultSet(Object[] argValues, ResultSet argRS, boolean argCanonicalColumnOrder) throws SQLException {
		assert argValues != null;
		assert argRS != null;
		
		Class<?>[] lclFieldTypes = getFieldTypes();
		int lclLength = getColumnNames().length; //
		
		if (argCanonicalColumnOrder) {
			for (int lclI = 0; lclI < lclLength; ++lclI) {
				try {
					argValues[lclI] = OpalUtility.convertTo(lclFieldTypes[lclI], argRS.getObject(lclI+1)); /* JDBC columns are 1-based */
				} catch (SQLException lclE) {
					ourLogger.error("Could not retrieve value for column \"" + getColumnNames()[lclI] + "\".", lclE);
					throw lclE;
				}
			}
		} else {
			String[] lclColumnNames = getColumnNames();
			for (int lclI = 0; lclI < lclLength; ++lclI) {
				try {
					argValues[lclI] = OpalUtility.convertTo(lclFieldTypes[lclI], argRS.getObject(lclColumnNames[lclI]) );
				} catch (SQLException lclE) {
					ourLogger.error("Could not retrieve value for column \"" + lclColumnNames[lclI] + "\".", lclE);
					throw lclE;
				}
			}
		}
	}
	
	@SuppressWarnings("resource") // We are not responsible for closing argDTP's Connection object.
	protected void updateInternal(DatabaseTransactionParameter argDTP, O argOpal) throws PersistenceException {
		assert argDTP != null;
		assert argOpal != null;
		
		// THINK: Do we ever actually use the Map-nature of this?  Or is it just a List of name-value pairs?
		Map<String, Object> lclMap = new HashMap<>(); // FIXME:  Use a faster map for small things?
		
		populateUpdateMap(lclMap, argOpal);
		
		if (lclMap.size() == 0) {
//			ourLogger.debug("Skipping " + this + " (no fields modified)");
			return;
		}
		
		try {
			int lclResult = DatabaseUtility.update(
					argDTP.getConnection(),
					getFullyQualifiedTableName(),
					lclMap,
					getPrimaryKeyWhereClauseColumns(),
					argOpal.getPrimaryKeyWhereClauseValues()
					);
			
			if (lclResult != 1) {
				ourLogger.warn("UPDATE SQL for " + argOpal + " resulted in " + lclResult + " rows being changed.");
			}
			return;
		} catch (SQLException lclE) {
			throw new PersistenceException("Unable to update " + argOpal + " with data " + lclMap, lclE);
		}
	}
	
	@Override
	protected void updateInternal(TransactionParameter argTP, O argOpal) throws PersistenceException {
		updateInternal((DatabaseTransactionParameter) argTP, argOpal);
		return;
	}
	
	/* I'd like this to be protected */
	@Override
	public TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap) throws PersistenceException {
		Validate.notNull(argTPMap);
		TransactionParameter lclTP = argTPMap.get(getDataSource());
		if (lclTP == null) {
			try {
				argTPMap.put(getDataSource(), lclTP = new DatabaseTransactionParameter(getDataSource().getConnection()));
			} catch (SQLException lclE) {
				throw new PersistenceException("Could not create new DatabaseTransactionParameter", lclE);
			}
		}
		return lclTP;
	}
	
	@Override
	public void reloadForQuery(Collection<? extends O> argC, Query argQ) throws PersistenceException {
		Validate.notNull(argC);
		Validate.notNull(argQ);
		
		throw new UnimplementedOperationException();
		
//		OpalCache lclOC = getOpalCache();
//		
//		Connection lclConnection = null;
//		ResultSet lclRS = null;
//		
//		try {
//			lclConnection = getDataSource().getConnection();
//			lclRS = createResultSet(lclConnection, argQ);
//			
//			while (lclRS.next()) {
//				final OpalKey<O> lclOK = createOpalKeyForRow(lclRS);
//				synchronized (this) { /* The so-called "Matt's fix" */
//					O lclOpal = lclOC.forOpalKey(lclOK);
//					if (lclOpal != null) {
//						Object[] lclNewValues = new Object[getFieldCount()];
//						
//						populateValueArrayFromResultSet(lclNewValues, lclRS);
//				
//						lclOpal.myNewValues = lclNewValues;
//						System.arraycopy(lclNewValues, 0, lclOpal.myOldValues, 0, lclNewValues.length);
//						lclOpal.updateReferencesAfterReload();
//						lclOpal.updateCollectionsAfterReload();
//						
//						argC.add(lclOpal);
//					}
//				}
//			}
//		} catch (SQLException lclE) {
//			throw new PersistenceException("Unable to reload Opals for query " + argQ, lclE);
//		} finally {
//			if (lclRS != null) {
//				DatabaseUtility.cleanUp(lclRS);
//			} else {
//				DatabaseUtility.closeConnection(lclConnection);
//			}
//		}
	}
	
}
