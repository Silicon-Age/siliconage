package com.siliconage.util;

import java.util.Collection;
import java.util.ArrayList;

import org.apache.commons.lang3.Validate;

public abstract class AssertUtility {

	private AssertUtility() {
		throw new UnimplementedOperationException();
	}
	
	public static <E> void deepCompareNonNullCollections(Collection<E> argC1, Collection<E> argC2) {
		if (argC1 == null) {
			if (argC2 == null) {
				return;
			} else {
				throw new IllegalStateException("argC1 is null, argC2 is not null.");
			}
		} else if (argC2 == null) {
			throw new IllegalStateException("argC1 is not null, argC2 is null.");
		}
		
		Validate.notNull(argC1);
		Validate.notNull(argC2);
		
		ArrayList<E> lclAL1 = new ArrayList<>(argC1.size());
		ArrayList<E> lclAL2 = new ArrayList<>(argC2.size());
		
		lclAL1.addAll(argC1);
		lclAL2.addAll(argC2);
		
		for (int lclIndex1 = 0; lclIndex1 < lclAL1.size(); lclIndex1++) {
			Object lclO1 = lclAL1.get(lclIndex1);
			if (lclO1 == null) {
				throw new IllegalStateException("null found in argC1");
			}
			int lclIndex2 = lclAL2.indexOf(lclO1);
			if (lclIndex2 < 0) {
				throw new IllegalStateException(lclO1 + " not found in argC2");
			}
			Object lclO2 = lclAL2.get(lclIndex2);
			if (lclO2 == null) {
				throw new IllegalStateException("null found in argC2");
			}
			if (lclO1.equals(lclO2)) {
				if (lclO2.equals(lclO1)) {
					lclAL2.remove(lclIndex2);
					
					if (lclO1 == lclO2) {
						continue;
					}
					
					if (lclO1 instanceof DeepEquals) {
						if (lclO2 instanceof DeepEquals) {
							((DeepEquals) lclO1).ensureDeepEquality((DeepEquals) lclO2);
						} else {
							throw new IllegalStateException(lclO1 + " implements DeepEquals but " + lclO2 + " does not");
						}
					} else {
						if (lclO2 instanceof DeepEquals) {
							throw new IllegalStateException(lclO2 + " implements DeepEquals but " + lclO1 + " does not");
						} else {
						/* Neither is capable of deep equality */
						}
					}
				} else {
					throw new IllegalStateException(lclO1 + " .equals " + lclO2 + " but not vice versa");
				}
			} else {
				throw new IllegalStateException(lclO1 + " not equal to " + lclO2 + "; probably original collections used compareTo and it is not equivalent to equals()");
			}
		}
		
		if (lclAL2.size() != 0) {
			throw new IllegalStateException("Objects existed in argC2 that were not in argC1.");
		}
	}
}
