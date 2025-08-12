package com.opal.types;

import com.opal.Opal;

/* C = child opal (contained in the Set), P = parent opal (owner of the Set) */
public interface OpalBackCollectionSet<C extends Opal<?>, P extends Opal<?>> extends TransactionAwareSet<C> {
	public boolean removeInternal(C argC);
	public boolean addInternal(C argC);
	
//	public boolean isMutated();
//	public boolean isSetLoaded();
}
