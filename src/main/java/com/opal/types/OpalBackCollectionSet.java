package com.opal.types;

import com.opal.Opal;

/* C = child opal (contained in the Set), P = parent opal (owner of the Set) */
/* THINK: Can we just remove the type parameter completely? */
public interface OpalBackCollectionSet<C extends Opal<?>, P extends Opal<?>> extends TransactionAwareSet<C> {
	/* If a malevolent user takes a child collection and casts it to an OpalBackCollectionSet (rather than
	 * using it as a Set, like a normal person), then they will get access to these methods and will be able
	 * to make changes that disrupt Opal invariants.  I don't know how to eliminate that possibility without
	 * reimplementing Opal's transaction-aware Collections in a really clunky way.
	 */
	public boolean removeInternal(C argC);
	public boolean addInternal(C argC);
}
