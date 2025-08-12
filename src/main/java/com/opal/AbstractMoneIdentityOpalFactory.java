package com.opal;

import java.util.Collections;
import java.util.Collection;
import java.util.Set;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

import com.siliconage.util.UnimplementedOperationException;

public abstract class AbstractMoneIdentityOpalFactory<U extends IdentityUserFacing, O extends IdentityOpal<U>> extends AbstractIdentityOpalFactory<U, O> {
//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(AbstractMoneIdentityOpalFactory.class.getName());
	
	protected AbstractMoneIdentityOpalFactory() {
		super();
	}
	
	/* The instantiate methods are responsible for creating an Opal object and populating it with an array of values loaded
	 * from the persistent store.  These arrays have generally been created by populateValueArray().  An instantiate method
	 * will typically call invoke the constructor for the WidgetOpal and pass in the array of values.
	 * 
	 * When instantiate is used to create a new Opal (i.e., one that doesn't exist in the persistent store), a null value is
	 * passed to argValues (rather than, say, an array of nulls.).
	 */

	/* THINK: Should this get pushed up to AbstractIdentityOpalFactory? */
	protected abstract O instantiate(Object[] argValues);
	
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
		return;
	}
	
	
	/* This method returns the Opal-specific array of member types used by the fields taken from the
	 * database.
	 */
	@Override
	protected abstract Class<?>[] getFieldTypes();
	
	/* insertInternal is responsible for handling the tasks specific to this kind of persistence 
	 * engine (in this case a generic relational database) that occur when an Opal is inserted.  For
	 * convenience, all insertInternal does is cast its TransactionParameter to a DatabaseTransactionParameter
	 * and pass it to an overloaded method.
	 */
	@Override
	protected void insertInternal(TransactionParameter argTP, O argOpal) throws PersistenceException {
		return;
	}
	
	
	@Override
	public Set<O> getAll() throws PersistenceException {
		return Collections.emptySet();
	}
	
	
	@Override
	public void acquireForQuery(Collection<O> argCollection, Query argQuery) throws PersistenceException {
		throw new UnimplementedOperationException();
	}
	
	@Override
	protected Object[] loadValuesFromPersistentStore(OpalKey<O> argOpalKey) {
		/* There is no persistent store, so the joke's on you! */
		return null;
	}
	
	@Override
	protected O loadFromPersistentStore(OpalKey<O> argOK) throws PersistenceException {
		/* There is no persistent store! */
		return null;
	}
	
	@Override
	protected void updateInternal(TransactionParameter argTP, O argOpal) throws PersistenceException {
		return;
	}
	
	/* I'd like this to be protected */
	@Override
	public TransactionParameter extractTransactionParameter(Map<DataSource, TransactionParameter> argTPMap) throws PersistenceException {
		Validate.notNull(argTPMap);

		return null;
	}
	
	@Override
	public void reloadForQuery(Collection<? extends O> argC, Query argQ) throws PersistenceException {
		throw new UnimplementedOperationException();
	}
}
