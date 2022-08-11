package com.siliconage.util;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Collections;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

/**
 * @author topquark
 */
public class Tally<K> {
	private final HashMap<K, AtomicInteger> myHashMap;
	
	public Tally() {
		super();
		
		myHashMap = new HashMap<>();
	}
	
	public Tally(Iterable<K> argI) {
		super();
		
		myHashMap = new HashMap<>();
		
		tally(argI);
	}
	
	public Tally(Iterator<K> argI) {
		super();
		
		myHashMap = new HashMap<>();
		
		tally(argI);
	}
	
	public Tally(Map<K, Integer> argMap) {
		super();
		
		Validate.notNull(argMap);
		
		myHashMap = new HashMap<>(argMap.size());
		for (Map.Entry<K, Integer> lclEntry : argMap.entrySet()) {
			K lclKey = lclEntry.getKey();
			
			Integer lclValueObj = lclEntry.getValue();
			Validate.notNull(lclValueObj, "Null value for " + lclKey);
			int lclValue = lclValueObj.intValue();
			Validate.isTrue(lclValue >= 0, "Negative value (" + lclValue + ") for " + lclKey);
			
			myHashMap.put(lclKey, new AtomicInteger(lclValue));
		}
	}
	
	protected HashMap<K, AtomicInteger> getMap() {
		return myHashMap;
	}
	
	public Tally<K> tally(K argO) {
		if (argO != null) {
			AtomicInteger lclVal = getMap().get(argO);
			if (lclVal == null) {
				getMap().put(argO, new AtomicInteger(1));
			} else {
				lclVal.incrementAndGet();
			}
		}
		
		return this;
	}
	
	public List<K> keyList() {
		return new ArrayList<>(getMap().keySet());
	}
	
	public List<K> keysSorted() {
		return keysSortedBy(null); // will throw an exception if K is not Comparable
	}
	
	public List<K> keysSortedBy(Comparator<? super K> argComparator) {
		// argComparator may be null, but that will cause an exception if K is not Comparable
		
		List<K> lclKeys = keyList();
		lclKeys.sort(argComparator);
		return lclKeys;
	}
	
	public Set<K> keySet() {
		return new HashSet<>(getMap().keySet());
	}
	
	public Tally<K> decrement(K argO) {
		if (argO != null) {
			AtomicInteger lclVal = getMap().get(argO);
			if (lclVal == null) {
				throw new IllegalStateException("None yet tallied");
			} else if (lclVal.intValue() == 0) {
				throw new IllegalStateException("Already zero");
			} else {
				lclVal.decrementAndGet();
			}
		}
		
		return this;
	}
	
	public Tally<K> tally(K argO, int argCount) {
		Validate.isTrue(argCount >= 0);
		
		if (argO != null) {
			AtomicInteger lclVal = getMap().get(argO);
			if (lclVal == null) {
				getMap().put(argO, new AtomicInteger(argCount));
			} else {
				lclVal.addAndGet(argCount);
			}
		}
		
		return this;
	}
	
	public Tally<K> tally(Iterable<K> argI) {
		if (argI == null) {
			return this;
		} else {
			return tally(argI.iterator());
		}
	}
	
	public Tally<K> tally(Iterator<K> argI) {
		if (argI == null) {
			return this;
		}
		
		while (argI.hasNext()) {
			tally(argI.next());
		}
		
		return this;
	}
	
	public <T> Tally<K> tally(T[] argTs, Function<T, K> argKeyExtractor) {
		return tally(Arrays.asList(argTs), argKeyExtractor);
	}
	
	public <T> Tally<K> tally(Iterable<T> argI, Function<T, K> argKeyExtractor) {
		if (argI == null) {
			return this;
		} else {
			return tally(argI.iterator(), argKeyExtractor);
		}
	}
	
	public <T> Tally<K> tally(Iterator<T> argI, Function<T, K> argKeyExtractor) {
		if (argI == null) {
			return this;
		}
		Validate.notNull(argKeyExtractor);
		
		while (argI.hasNext()) {
			T lclT = argI.next();
			tally(argKeyExtractor.apply(lclT));
		}
		return this;
	}
	
	public static <T, K> Tally<K> of(T[] argT, Function<T, K> argKeyExtractor) {
		return of(Arrays.asList(argT), argKeyExtractor);
	}
	
	public static <T, K> Tally<K> of(Iterable<T> argI, Function<T, K> argKeyExtractor) {
		return of(argI.iterator(), argKeyExtractor);
	}
	
	public static <T, K> Tally<K> of(Iterator<T> argI, Function<T, K> argKeyExtractor) {
		Tally<K> lclT = new Tally<>();
		lclT.tally(argI, argKeyExtractor);
		return lclT;
	}
	
	public Tally<K> clear() {
		getMap().clear();
		return this;
	}
	
	public Tally<K> clear(K argO) {
		getMap().remove(argO);
		return this;
	}
	
	public int get(K argK) {
		AtomicInteger lclVal = getMap().get(argK);
		if (lclVal == null) {
			return 0;
		} else {
			int lclIntVal = lclVal.intValue();
			Validate.isTrue(lclIntVal >= 0, "Value is negative");
			return lclIntVal;
		}
	}
	
	public double getPercentage(K argK) {
		int lclTotal = getTotal();
		
		if (lclTotal == 0) {
			return Double.NaN;
		} else {
			return 1.0d * get(argK) / lclTotal;
		}
	}
	
	public int getSubtotal(Predicate<K> argFilter) {
		Validate.notNull(argFilter);
		
		return getMap().keySet().stream()
			.filter(argFilter)
			.mapToInt(this::get)
			.sum();
	}
	
	public int getSubtotal(Collection<K> argMatches) {
		if (argMatches == null || argMatches.isEmpty()) {
			return getTotal();
		} else {
			return getSubtotal(argMatches::contains);
		}
	}
	
	public int getSubtotal(@SuppressWarnings("unchecked") K... argMatches) {
		return getSubtotal(Arrays.asList(argMatches));
	}
	
	public int getTotal() {
		return getMap().values().stream().mapToInt(AtomicInteger::intValue).sum();
	}
	
	public int size() {
		return getMap().size();
	}
	
	// THINK: Should these really be equivalent to getTotal() == 0 and getTotal() > 0, respectively?
	public boolean isEmpty() {
		return size() == 0;
	}
	
	public boolean isNonempty() {
		return isEmpty() == false;
	}
	
	public Collection<K> getModes() {
		int lclModeValue = getMap().values().stream()
			.mapToInt(AtomicInteger::intValue)
			.max().orElse(-1);
		
		if (lclModeValue < 0) {
			return Collections.emptySet();
		}
		
		return getMap().keySet().stream()
			.filter(argK -> get(argK) == lclModeValue)
			.collect(Collectors.toList());
	}
	
	public K getSingleModeOrNull() {
		Collection<K> lclModes = Validate.notNull(getModes());
		
		if (lclModes.size() == 1) {
			return lclModes.iterator().next();
		} else {
			return null;
		}
	}
	
	public void report(PrintStream argPS) {
		if (argPS == null) {
			return;
		}
		for (Map.Entry<K, AtomicInteger> lclEntry : myHashMap.entrySet()) {
			argPS.println(lclEntry.getKey() + " -> " + lclEntry.getValue().intValue());
		}
	}
	
	public Tally<K> copy() {
		Tally<K> lclClone = new Tally<>();
		for (Map.Entry<K, AtomicInteger> lclEntry : myHashMap.entrySet()) {
			lclClone.tally(lclEntry.getKey(), lclEntry.getValue().intValue());
		}
		
		return lclClone;
	}
	
	@Override
	public String toString() {
		return getMap().toString() + "; total -> " + getTotal();
	}
}
