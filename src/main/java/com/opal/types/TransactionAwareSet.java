package com.opal.types;

import java.util.Set;
import com.opal.TransactionAware;

public interface TransactionAwareSet<T> extends Set<T>, TransactionAware {
	/* No implementation here. */
}
