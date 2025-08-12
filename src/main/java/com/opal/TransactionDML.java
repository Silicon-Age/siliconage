package com.opal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

import com.siliconage.database.DatabaseUtility;

public class TransactionDML extends AbstractTransactionAware {
	private final DataSource myDataSource;
	private final String mySQL;
	private final Object[] myParameters;
	
	public TransactionDML(DataSource argDS, String argSQL, Object[] argParameters) {
		super();
		
		Validate.notNull(argDS);
		myDataSource = argDS;
		
		Validate.notNull(argSQL);
		mySQL = argSQL;
		
		myParameters = argParameters;
		
		TransactionContext lclTC = TransactionContext.getActive();
		Validate.notNull(lclTC, "Cannot create a TransactionDML without being inside a TransactionContext.");
		joinTransactionContext(lclTC);
		
		return;
	}
	
	public TransactionDML(DataSource argDS, String argSQL) {
		this(argDS, argSQL, null);
	}
	
	public DataSource getDataSource() {
		return myDataSource;
	}
	
	public String getSQL() {
		return mySQL;
	}
	
	public Object[] getParameters() {
		return myParameters;
	}
	
	@Override
	protected void commitPhaseOneInternal(TransactionParameter argTP) throws PersistenceException {
		Validate.notNull(argTP);
		Validate.isTrue(argTP instanceof DatabaseTransactionParameter);
		DatabaseTransactionParameter lclDTP = (DatabaseTransactionParameter) argTP;
		try (Connection lclC = lclDTP.getConnection()) {
			DatabaseUtility.executeDML(lclC, getSQL(), getParameters());
		} catch (SQLException lclE) {
			throw new PersistenceException("Exception thrown while executing TransactionDML " + getSQL() + ".", lclE);
		} catch (RuntimeException lclE) {
			throw new PersistenceException("Exception thrown while executing TransactionDML " + getSQL() + ".", lclE);
		} // autoclose lclC
	
		return;
	}
	
	@Override
	public TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap) throws PersistenceException {
		Validate.notNull(argTPMap);
		DataSource lclDS = getDataSource();
		TransactionParameter lclTP = argTPMap.get(lclDS);
		if (lclTP == null) {
			try {
				argTPMap.put(getDataSource(), lclTP = new DatabaseTransactionParameter(lclDS.getConnection()));
			} catch (SQLException lclE) {
				throw new PersistenceException("Could not create new DatabaseTransactionParameter", lclE);
			}
		}
		return lclTP;
	}
	
	@Override
	protected void commitPhaseTwoInternal(TransactionParameter argTP) throws PersistenceException {
		return;
	}
	
	@Override
	protected void joinTransactionContextInternal() {
		return;
	}
	
	@Override
	protected void rollbackInternal() {
		return;
	}
}
