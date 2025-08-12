package com.opal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

import com.opal.types.StringSerializable;
import com.siliconage.database.DatabaseUtility;

/* This class provides a default implementation for OpalFactories that have the responsibilities of connecting to a specific
 * brand of database and building an Opal.  It extends AbstractEphemeralOpalFactory by specializing that class' methods to work with
 * relational databases (as opposed to other types of persistent stores).
 * 
 * In general, the Opal framework will create one concrete subclass of AbstractDatabaseEphemeralOpalFactory for each mapped
 * (ephemeral) view (or table) and it will have a name like SQLServerWidgetOpalFactory that combines the name of the
 * database (SQLServer) and that of the mapped table (Widget).
 * 
 * Each subclass created will be a Singleton, which limits the type of data that can be stored in this class. 
 */

public abstract class AbstractDatabaseEphemeralOpalFactory<U extends UserFacing, O extends Opal<U>> extends AbstractEphemeralOpalFactory<U, O> {

	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(AbstractDatabaseEphemeralOpalFactory.class.getName());
	
	protected AbstractDatabaseEphemeralOpalFactory() {
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
	
	/* This method converts an array of column names into a comma-separated list.
	 */
	/* CHECK: This method is shared with AbstractDatabaseOpalFactory; consolidate */
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
	 * This method cannot be used to construct WHERE clauses with any greater variety than simple equality for zero
	 * or more columns.
	 * 
	 * Generally speaking, this method is used to load rows referenced by foreign keys.
	 * 
	 * I don't know if this method works correctly if any of the values in argParameters is null because it builds a
	 * PreparedStatement that always uses "column_1 = ?".
	 */
	protected static final ResultSet generateResultSet(Connection argConnection, String argFullyQualifiedTableName, String[] argColumnNames, String[] argWhereClauseColumnNames, Object[] argParameters, String argOrderBy) throws SQLException {
		Validate.notNull(argConnection);
		Validate.notNull(argFullyQualifiedTableName);
		
		StringBuilder lclSB = new StringBuilder(512);
		if (argColumnNames == null) {
			lclSB.append("SELECT * FROM ");
		} else {
			lclSB.append("SELECT ");
			lclSB.append(generateColumnNamesList(argColumnNames));
			lclSB.append(" FROM ");
		}
		lclSB.append(argFullyQualifiedTableName);
		
		if (argWhereClauseColumnNames != null) {
			lclSB.append(" WHERE ");
			for (int lclI = 0; lclI < argWhereClauseColumnNames.length; ++lclI) {
				if (lclI > 0) {
					lclSB.append(" AND ");
				}
				lclSB.append(argWhereClauseColumnNames[lclI]);
				lclSB.append(" = ?");
			}
		}
		
		if (argOrderBy != null) {
			lclSB.append(" ORDER BY ");
			lclSB.append(argOrderBy);
		}
		
		if (argWhereClauseColumnNames != null) {
			for (int lclI = 0; lclI < argWhereClauseColumnNames.length; ++lclI) {
				Object lclO = argParameters[lclI];
				if (lclO instanceof java.time.LocalDate) {
					argParameters[lclI] = java.sql.Date.valueOf((LocalDate) lclO);
				} else if (lclO instanceof java.time.LocalDateTime){
					argParameters[lclI] = java.sql.Timestamp.valueOf((LocalDateTime) lclO);
				} else if (lclO instanceof StringSerializable) {
					argParameters[lclI] = ((StringSerializable) lclO).toSerializedString();
				} else {
					// No conversion required
				}
			}
		}
		
//		System.out.println("About to call select");
		ResultSet lclRS = DatabaseUtility.select(argConnection, lclSB.toString(), argParameters);
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
		
		try (Connection lclC = getDataSource().getConnection()) {
			load(lclC, argFullyQualifiedTableName, argFieldNames, argParameters, argOrderBy, argCollection);
			
			return;
		} catch (SQLException lclE) {
			throw new PersistenceException("Could not load", lclE);
		}
	}
	
	/* For the general purpose of this method, see the documentation of the immediately preceding method.  It creates
	 * a query to load a subset of rows of the specified table, runs that query on the provided connection, and adds
	 * the Opals corresponding to those rows to the provided Collection (without clearing that Collection first).
	 */
	protected final void load(Connection argConnection, String argFullyQualifiedTableName, String[] argFieldNames, Object[] argParameters, String argOrderBy, Collection<O> argCollection) throws SQLException {
//		long lclStart = System.currentTimeMillis();

		try (ResultSet lclRS = generateResultSet(argConnection, argFullyQualifiedTableName, getColumnNames(), argFieldNames, argParameters, argOrderBy)) {
			/* Which should never be null. */
			Validate.notNull(lclRS);
			
			/* Convert that ResultSet into Opals (either by looking them up in the OpalCache or by constructing new Opals
			 * from the data in it, then add them to argCollection.
			 */
			acquireFromResultSet(lclRS, argCollection, true);
		}
	}
	
	/* This method adds every Opal in the Factory's underlying table to the specific Collection (without clearing the
	 * Collection first).
	 */
	protected final Set<O> getAll(Connection argConnection) throws SQLException {
		assert argConnection != null;
		
		// long lclStart = System.currentTimeMillis();
		try (ResultSet lclRS = generateResultSet(argConnection, getFullyQualifiedTableName(), getColumnNames(), null, null, null)) {
			// System.out.println("Done with generate ResultSet; about to acquireFromResultSet");
			/* Convert those rows into their Opals.  This will involve looking to see whether each one is already in
			 * the cache and, if not, creating it from the data in the ResultSet.
			 */
			return getFromResultSet(lclRS, true);
			// System.out.println("Done with acquireFromResultSet");
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
		
		try (ResultSet lclRS = DatabaseUtility.select(argConnection, argSQL, argParameters)) {
			
			/* Convert those rows into their Opals.  This will involve looking to see whether each one is already in
			 * the cache and, if not, creating it from the data in the ResultSet.
			 */
			acquireFromResultSet(lclRS, argCollection, false);
			
			return;
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
		
//		final OpalCache lclOC = getOpalCache();
		
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
			O lclOpal = extractSingleOpalFromResultSet(/* lclOC, */ argRS, argCanonicalColumnOrder);
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
	
	protected final O extractSingleOpalFromResultSet(/* OpalCache argOC, */ ResultSet argRS, boolean argCanonicalColumnOrder) throws SQLException {
//		Validate.notNull(argOC);
		Validate.notNull(argRS);
		
		O lclOpal;
		
		Object[] lclValues = new Object[getFieldCount()];
		populateValueArrayFromResultSet(lclValues, argRS, argCanonicalColumnOrder);
		lclOpal = instantiate(lclValues);
		assert lclOpal.getUserFacing() == null;
		determineUserFacing(lclOpal, false);
		
		return lclOpal;
	}
	
	@Override
	public Set<O> getAll() throws PersistenceException {
		try (Connection lclC = getDataSource().getConnection()) {
			return getAll(lclC);
		} catch (SQLException lclE) {
			throw new PersistenceException("Could not acquire all Opals", lclE);
		}
	}
	
	/* CHECK: This method is shared with AbstractDatabaseOpalFactory; consolidate */
	protected void adjustParameters(Object[] argParameters) {
		if (argParameters == null) {
			return;
		}
		for (int lclI = 0; lclI < argParameters.length; ++lclI) {
			Object lclO = argParameters[lclI];
			if (lclO instanceof LocalDate) {
				argParameters[lclI] = java.sql.Date.valueOf((LocalDate) lclO);
			} else if (lclO instanceof LocalDateTime) {
				argParameters[lclI] = java.sql.Timestamp.valueOf((LocalDateTime) lclO);
			} else {
				/* Do nothing */
			}
		}
	}
	
	/* CHECK: This method is shared with AbstractDatabaseOpalFactory; consolidate */
	protected ResultSet createResultSet(Connection argConnection, Query argQuery) throws SQLException {
		if (argQuery instanceof ImplicitTableDatabaseQuery) {
			ImplicitTableDatabaseQuery lclITDQ = (ImplicitTableDatabaseQuery) argQuery;
			
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
		} else if (argQuery instanceof DatabaseQuery) {
			DatabaseQuery lclDQ = (DatabaseQuery) argQuery;
			
			Object[] lclParameters = lclDQ.getParameters();
			adjustParameters(lclParameters);
			
			return DatabaseUtility.select(
				argConnection,
				lclDQ.getSQL(),
				lclDQ.getParameters()
			);
		} else {
			throw new IllegalArgumentException("This database-driven OpalFactory only understands DatabaseQuery objects; it was passed a " + argQuery.getClass().getName() + " that stringifies as \"" + argQuery + "\"");
		}
	}
	
	/* CHECK: This method is shared with AbstractDatabaseOpalFactory; consolidate */
	@Override
	public void acquireForQuery(Collection<O> argCollection, Query argQuery) throws PersistenceException {
		long lclA = System.currentTimeMillis();
		
		Validate.notNull(argCollection);
		Validate.notNull(argQuery);
		
		try (Connection lclC = getDataSource().getConnection();
			ResultSet lclRS = createResultSet(lclC, argQuery)) {
			
			acquireFromResultSet(lclRS, argCollection, ((DatabaseQuery) argQuery).areColumnsInCanonicalOrder());
			long lclF = System.currentTimeMillis();
			com.siliconage.database.DatabaseUtility.ourTally.tally("Acquiring from " + getFullyQualifiedTableName() + " for " + argQuery, lclF - lclA);
			return;
		} catch (SQLException lclEx) {
			throw new PersistenceException("Could not acquire opals for the query \"" + argQuery.getClass().getName() + "\" that stringifies as \"" + argQuery.toString() + "\"", lclEx);
		}
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
					ourLogger.error("Could not retrieve value for column \"" + getColumnNames()[lclI] + "\".");
					throw lclE;
				}
			}
		} else {
			String[] lclColumnNames = getColumnNames();
			for (int lclI = 0; lclI < lclLength; ++lclI) {
				try {
					argValues[lclI] = OpalUtility.convertTo(lclFieldTypes[lclI], argRS.getObject(lclColumnNames[lclI]) );
				} catch (SQLException lclE) {
					ourLogger.error("Could not retrieve value for column \"" + lclColumnNames[lclI] + "\".");
					throw lclE;
				}
			}
		}
	}
		
}
