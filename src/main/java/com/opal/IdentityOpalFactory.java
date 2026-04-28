package com.opal;

import java.util.Collection;

/**
 * @author topquark
 */

public interface IdentityOpalFactory<U extends IdentityUserFacing/*<U>*/, O extends IdentityOpal<U> > extends OpalFactory<U, O> { // OPALFIXME
	
	public O forUniqueString(String argUniqueString);
	
	public void reload(O argOpal);
	public void reloadForQuery(Collection<? extends O> argCollection, Query argQuery);
	
	public void commitPhaseOne(TransactionParameter argTP, O argOpal);
	public void commitPhaseTwo(TransactionParameter argTP, O argOpal);

}
