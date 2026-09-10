package com.opal.types;

import com.opal.AbstractTransactionAware;
import com.opal.TransactionalOpal;

/* C = child opal (contained in the Set), P = parent opal (owner of the Set) */
/* THINK: Can we just remove the type parameter completely? */
public abstract class AbstractOpalBackCollectionSet<C extends TransactionalOpal<?>, P extends TransactionalOpal<?>> extends AbstractTransactionAware implements OpalBackCollectionSet<C, P> {
	/* FIXME: What can go here? */
}
