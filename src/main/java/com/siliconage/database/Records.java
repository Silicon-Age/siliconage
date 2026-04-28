package com.siliconage.database;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.opal.DatabaseQuery;

/* This class exists to provide a convenient way to populate Java records from a database query.  The quick guide to using it is that
 * you make a public record class for the data you want.  For instance;
 * 
 * public record Stuff(int id, String firstName, String lastName) {}
 * 
 * then write SQL that returns a ResultSet with columns named "id", "firstName", and "lastName," that match those of the record's
 * components without regard to case or the presence of underscores.  For instance, a column named "First_Name" would also match
 * that of our example record.
 * 
 * Make a DatabaseQuery dq object out of your SQL and the relevant parameters, and then call:
 * 
 * Records.select(ds, dq);
 * 
 * You'll get back a List<Stuff>.
 * 
 * You'll need to write:
 * 
 * List<Stuff> stuff = Records.select(...)
 * 
 * so that the select method can deduce the record type that should be constructed.  It'll also need to be able to invoke the
 * record's canonical constructor, so the record will almost certainly need to be public.
 */
public abstract class Records {
	/* A functional interface for methods that accept a ResultSet and a columnName and a return an Object created from
	 * the ResultSet's current row's value for the column name.  The key is that these methods will alter (subtly or not)
	 * the value in some way.
	 */
	private static interface ResultSetAccessor {		
		Object extract(ResultSet rs, String columnName) throws SQLException;
	}

	/* A ResultSetAccessor that makes sure an integer-valued database column is manifested as a (non-null) Integer.
	 * This needs to return an Object because it is ultimately packed into an Object[] passed to the record
	 * constructor.
	 */
	private static Object extractPrimitiveInt(ResultSet rs, String column) throws SQLException {
		return Integer.valueOf(rs.getInt(column));
	}
	
	/* A ResultSetAccessor that makes sure a real-valued database column is manifested as a (non-null) Double.
	 * This needs to return an Object because it is ultimately packed into an Object[] passed to the record
	 * constructor.
	 */
	private static Object extractPrimitiveDouble(ResultSet rs, String column) throws SQLException {
		return Double.valueOf(rs.getDouble(column));
	}

	/* A ResultSetAccessor that simply returns a String from the appropriate column. */
	private static Object extractString(ResultSet rs, String column) throws SQLException {
		return rs.getString(column);
	}
	
	/* A column handler stores the accessor and the column name that it can apply to any (single row of a)
	 * ResultSet.  Basically, it converts whatever data is the named column into an Object that can be legitimately
	 * passed in the component-appropriate slot of the constructor call.
	 */
	private record ColumnHandler(ResultSetAccessor accessor, String columnName) {
		public Object access(ResultSet rs) throws SQLException {
			return accessor().extract(rs, columnName());
		}
	}
	
	/* This Handler has all of the reflection-obtained data necessary to turn a single row of a ResultSet into a
	 * record.
	 * 
	 * Its array of ColumnHandlers has one entry per component, all of which are methods that take
	 * a ResultSet and a column name, extract the value of that column, possibly polish it a little, and then
	 * returns an Object that could be passed to the all-field record constructor in the appropriate slot.
	 * 
	 * Its ctor component is the all-field constructor that should be called to instantiate the record.
	 */
	private record RowHandler<T extends Record>(ColumnHandler[] handlers, Constructor<T> ctor) {
		
		@SuppressWarnings("resource") // Not our responsibility to close rs
		public T construct(ResultSet rs) {
			ColumnHandler[] handlerArray = handlers();
			
			Object[] ctorArguments = new Object[handlerArray.length];
			for (int i = 0; i < handlerArray.length; ++i) {
				try {
					Object v = handlerArray[i].access(rs);
					ctorArguments[i] = v;
				} catch (SQLException e) {
					throw new RuntimeException("Could not parse component #" + i + " out of column " + handlerArray[i].columnName(), e);
				}
			}
			Constructor<T> tctor = ctor();
			try {
				return tctor.newInstance(ctorArguments);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException("Could not create record from ResultSet.", e);
			}
		}
	}
	
	/* The Map caching Handlers for each record class.  This is used by the select(...) and from(...) methods that
	 * take a record class as their first argument.
	 */
	private static final Map<Class<?>, RowHandler<?>> ourRecordHandlerMap = new HashMap<>();
	
	/* This method creates a Handler for translating a ResultSet into the specified record class <T extends Record>.  
	 * Essentially, it uses reflection to look at each record component and matches its name to a column in the ResultSet
	 * (by dropping underscores in the column name and ignoring case).  Then it checks the type of the component and
	 * figures out the appropriate method to call to populate it.  These are stored along with a reference to the
	 * standard constructor for the record (also obtained using reflection).
	 * 
	 * It is fine if the record class has additional constructors, but it must have the conventional all-component
	 * constructor.
	 * 
	 * It is fine if the ResultSet has additional columns that are not required for constructing the record.
	 * 
	 * The current code only works for (primitive) int, (primitive) double, and String record components (as each
	 * supported component type requires an if clause to determine the proper method to extract the value from the
	 * ResultSet.  If this way of constructing records proves useful, it should be easy to add many other types.
	 * 
	 * There is a problem here if different spots in the code want to populate instances of the same record class
	 * using ResultSets with different column names:  The correct column names from the first call are "baked into"
	 * the Handler that is associated with that record class.  If it is used for a different ResultSet (even one that
	 * could be handled by the name-matching algorithm used by createHandler), SQLExceptions for invalid column names
	 * will result.
	 * 
	 * The easiest workaround is probably just renaming the columns (SELECT x.y1 AS y2) in the SQL statement, but it
	 * could also be possible to add some sort of (optional) "Context" sentinel that joined with the record class
	 * to define the key in ourRecordHandlerMap. 
	 */
	private static <T extends Record> RowHandler<T> createHandler(Class<T> recordClass, ResultSet rs) throws SQLException, NoSuchMethodException, SecurityException {
		var metaData = rs.getMetaData();
		final int columnCount = metaData.getColumnCount();
		String[] columnNames = new String[columnCount];
		String[] adjustedNames = new String[columnCount];
		for (int i = 0; i < columnCount; ++i) {
			columnNames[i] = metaData.getColumnName(i + 1);
			adjustedNames[i] = columnNames[i].toLowerCase().replace("_", "");
		}
		
		RecordComponent[] components = recordClass.getRecordComponents();
		ColumnHandler[] handlers = new ColumnHandler[components.length];
		for (int i = 0; i < components.length; ++i) {
			RecordComponent component = components[i];
			String name = component.getName();
			String lowerName = name.toLowerCase();
			int matchedColumn = -1;
			for (int j = 0; j < columnCount; ++j) {
				if (lowerName.equals(adjustedNames[j])) {
					matchedColumn = j;
				}
			}
			if (matchedColumn >= 0) {
				ResultSetAccessor accessor;
				var type = component.getType();
				if (type == int.class) {
					accessor = Records::extractPrimitiveInt;
				} else if (type == double.class) {
					accessor = Records::extractPrimitiveDouble;
				} else if (type == String.class) {
					accessor = Records::extractString;
				} else {
					throw new IllegalStateException("Could not determine an extractor method for component " + component.getName() + " of type " + type.getName() + ".");
				}
				handlers[i] = new ColumnHandler(accessor, columnNames[matchedColumn]);				
			} else {
				throw new IllegalStateException("Could not match record component " + component.getName() + " to a column in the ResultSet.");
			}
		}
		
		Class<?>[] ctorArgumentTypes = new Class<?>[components.length];
		for (int i = 0; i < components.length; ++i) {
			ctorArgumentTypes[i] = components[i].getType();
		}
		Constructor<T> ctor = recordClass.getDeclaredConstructor(ctorArgumentTypes);
		if (ctor == null) {
			throw new IllegalStateException();
		}
		
		return new RowHandler<>(handlers, ctor);
	}
	
	/* This method looks up the appropriate Handler for translating a ResultSet into an instance of a record class.  If ho
	 * Handler is found in ourRecordHandlerMap (indicating that this is the first time instances of this record class
	 * have been created using DatabaseUtility), then one is created using the column names in the ResultSet argument's
	 * metadata (this is the only reason the ResultSet needs to be passed in).
	 * 
	 * The Handler essentially consists of an appropriate accessor for each field of the record plus the proper constructor
	 * to call.
	 */
	private static <T extends Record> RowHandler<T> obtainHandler(Class<T> recordClass, ResultSet rs) throws SQLException {
		@SuppressWarnings("unchecked")
		RowHandler<T> handler = (RowHandler<T>) ourRecordHandlerMap.get(recordClass); // synchronization?
		if (handler == null) {
			try {
				handler = createHandler(recordClass, rs);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("Could not find appropriate constructor on " + recordClass.getName() + ".", e);
			}
			ourRecordHandlerMap.put(recordClass, handler);
		}
		return handler;
	}
	
	/* This method takes a record class <T extends Record> and turns the current row of its ResultSet argument into an instance
	 * of that class.  ResultSet::next is not called before or after this construction.
	 * 
	 * The difficult work of object construction is delegated to the Handler obtained from obtainHandler.
	 */
	public static <T extends Record> T from(Class<T> recordClass, ResultSet rs) throws SQLException {
		if (recordClass == null) {
			throw new IllegalArgumentException("recordClass is null");
		}
		if (rs == null) {
			return null;
		}
		RowHandler<T> handler = obtainHandler(recordClass, rs);
		
		return handler.construct(rs);
	}

	/* This method takes a record class <T extends Record> and turns the contents of a ResultSet into instances of that
	 * record type.  It assumes that next() needs to be called on the ResultSet argument to advance to the first meaningful
	 * row.
	 * 
	 * The hard work of object construction is delegated to the Handler obtained from obtainHandler.
	 */
	public static <T extends Record> List<T> select(Class<T> recordClass, ResultSet rs) throws SQLException {
		if (recordClass == null) {
			throw new IllegalArgumentException("recordClass is null");
		}
		if (rs == null) {
			return null;
		}
		RowHandler<T> handler = obtainHandler(recordClass, rs);
		
		List<T> constructedRecords = new ArrayList<>();
		
		while (rs.next()) {
			constructedRecords.add(handler.construct(rs));
		}
		
		return constructedRecords;
	}
	
	/* This method takes a record class <T extends Record> and turns the output of a database query defined by its sql and parameters
	 * arguments into instances of those records.  It delegates the difficult work to the overload that takes a record
	 * class and a ResultSet.
	 */
	public static <T extends Record> List<T> select(Class<T> recordClass, Connection conn, String sql, Object... parameters) throws SQLException {
		if (conn == null) {
			throw new IllegalArgumentException("conn is null.");
		}
		if (recordClass == null) {
			throw new IllegalArgumentException("recordClass is null");
		}
		if (sql == null) {
			throw new IllegalArgumentException("sql is null");
		}
		// It's okay for parameters to be null (assuming that's compatible with the SQL statement)
		try (ResultSet rs = DatabaseUtility.select(conn, sql, parameters)) {
			return select(recordClass, rs);
		}
	}

	@SafeVarargs
	public static <T extends Record> List<T> select(Connection conn, DatabaseQuery dq, T... typeDeducingVararg) throws SQLException {
		if (conn == null) {
			throw new IllegalArgumentException("conn is null.");
		}
		if (dq == null) {
			throw new IllegalArgumentException("dq is null.");
		}
		if (typeDeducingVararg == null) {
			throw new IllegalArgumentException("typeDeducingArray is null.");
		}
		Class<?> arrayType = typeDeducingVararg.getClass();
		if (arrayType.isArray() == false) {
			throw new IllegalArgumentException("arrayType is not an array.");
		}
		@SuppressWarnings("unchecked")
		Class<T> witness = (Class<T>) arrayType.getComponentType();
		if (witness == null) {
			throw new IllegalArgumentException("witness is null.");
		}
		if (Record.class.isAssignableFrom(witness) == false) { // Redundant
			throw new IllegalArgumentException("Record is not assignable from witness.");
		}
		return select(
				witness,
				conn,
				dq.getSQL(),
				dq.getParameters()
				);
	}
	
	@SafeVarargs
	public static <T extends Record> List<T> select(DataSource ds, DatabaseQuery dq, T... typeDeducingVararg) throws SQLException {
		if (ds == null) {
			throw new IllegalArgumentException("ds is null.");
		}
		if (dq == null) {
			throw new IllegalArgumentException("dq is null.");
		}
		if (typeDeducingVararg == null) {
			throw new IllegalArgumentException("typeDeducingArray is null.");
		}
		Class<?> arrayType = typeDeducingVararg.getClass();
		if (arrayType.isArray() == false) {
			throw new IllegalArgumentException("arrayType is not an array.");
		}
		@SuppressWarnings("unchecked")
		Class<T> witness = (Class<T>) arrayType.getComponentType();
		if (witness == null) {
			throw new IllegalArgumentException("witness is null.");
		}
		if (Record.class.isAssignableFrom(witness) == false) {
			throw new IllegalArgumentException("Record is not assignable from witness.");
		}
		try (Connection conn = ds.getConnection()) {
			return select(
					witness,
					conn,
					dq.getSQL(),
					dq.getParameters()
					);
		} // close conn
	}
	
}
