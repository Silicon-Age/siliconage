package com.opal.cma;

import java.util.Objects;
import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.siliconage.web.form.AssembledDropdownField;
import com.siliconage.web.form.NameCodeExtractor;

import com.opal.DatabaseQuery;
import com.opal.Factory;
import com.opal.IdentityFactory;
import com.opal.IdentityUserFacing;
import com.opal.ImplicitTableDatabaseQuery;

public class OpalDropdownField<T extends OpalDropdownField<?, U>, U extends IdentityUserFacing> extends AssembledDropdownField<T, U> {
	// private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalDropdownField.class.getName());
	
	private IdentityFactory<U> myFactory;
	private DatabaseQuery myQuery;
	private Predicate<U> myFilter = x -> true;
	private boolean myNewObjectForm;
	private boolean myNullable = false;
	
	@SuppressWarnings("unchecked")
	protected OpalDropdownField(OpalForm<?> argOF, String argName, IdentityFactory<U> argFactory, NameCodeExtractor<? super U> argNCE) { // Should this just be Function<? super U, String>?
		super(argOF.generateFullyQualifiedName(argName), (U) argOF.getSavedValue(argName), argOF);
		
		if (argNCE == null) {
			namer(DefaultOpalNameCodeExtractor.getInstance()); // Why does this work?
		} else {
			namer(argNCE::extractName);
		}
		
		// No sense in Validate.notNull()'ing argOF, since the superclass constructor call will already have thrown an NPE if argOF == null
		myFactory = Validate.notNull(argFactory);
		
		myNewObjectForm = argOF.isNew();
	}
	
	public T setNullability(boolean argV) {
		myNullable = argV;
		
		if (argV) {
			addCssClass("nullable");
			attribute("data-allow-clear", "true");
		} else {
			addCssClass("non-null");
			attribute("data-allow-clear", "false");
		}
		
		return castThis();
	}
	
	public T nullable() {
		return setNullability(true);
	}
	
	public T nonNull() {
		return setNullability(false);
	}
	
	public boolean isNullable() {
		return myNullable;
	}
	
	public IdentityFactory<U> getFactory() {
		return myFactory;
	}
	
	public T factory(IdentityFactory<U> argFactory) {
		myFactory = argFactory;
		
		return castThis();
	}
	
	public DatabaseQuery getQuery() {
		return myQuery;
	}
	
	public T query(DatabaseQuery argQuery) {
		myQuery = argQuery;
		return castThis();
	}
	
	public T query(String argSQL, Object... argParameters) {
		Validate.notNull(argSQL);
		String lclSQL = argSQL.trim();
		if (lclSQL.length() > 6) { // 6 == "select".length();
			if (argSQL.toLowerCase().startsWith("select")) {
				return query(new DatabaseQuery(lclSQL, argParameters));
			}
		}
		return query(new ImplicitTableDatabaseQuery(lclSQL, argParameters));
	}
	
	public T filter(Predicate<U> argFilter) {
		Validate.notNull(argFilter);
		
		myFilter = myFilter.and(argFilter);
		
		return castThis();
	}
	
	public Predicate<U> getFilter() {
		return myFilter;
	}
	
	public T namer(Function<? super U, String> argNamer) {
		return namer(argNamer, IdentityUserFacing::getUniqueString).castThis();  // This line of code embodies many weird and wonderful things . . .
	}
	
	@Override
	protected List<U> determineChoices() {
		Collection<U> lclSpecifiedChoices = getChoices();
		
		List<U> lclChoices = getFilter() != null ? new LinkedList<>() : new ArrayList<>();
		
		if (isNullable()) {
			lclChoices.add(null);
		}
		
		if (lclSpecifiedChoices != null) {
			lclChoices.addAll(lclSpecifiedChoices);
		} else {
			Factory<U> lclFactory = Validate.notNull(getFactory());
			
			DatabaseQuery lclDQ = getQuery();
			
			if (lclDQ == null) {
				lclChoices.addAll(lclFactory.getAll());
			} else {
				lclFactory.acquireForQuery(lclChoices, lclDQ);
			}
		}
		
		Predicate<? super U> lclFilter = getFilter();
		if (lclFilter != null) {
			lclChoices.removeIf(argU -> (argU != null) && (lclFilter.test(argU) == false));
		}
		
		Collection<U> lclCurrents = getSavedValues();
		for (U lclCurrent : lclCurrents) {
			if (!lclChoices.contains(lclCurrent)) {
				if (lclCurrent != null || isNewObjectForm() == false) {
					lclChoices.add(0, lclCurrent);
				}
			}
		}
		
		if (isDoNotSort() == false) {
			if (getComparator() != null) {
				lclChoices.sort(Comparator.nullsFirst(getComparator()));
			} else if (Comparable.class.isAssignableFrom(getFactory().getUserFacingInterface())) { // In other words, is the class of options Comparable?
				List<U> lclChoicesSorted = lclChoices.stream().filter(Objects::nonNull).sorted().collect(Collectors.toList());
				if (lclChoices.contains(null)) {
					lclChoicesSorted.add(0, null);
				}
				lclChoices = lclChoicesSorted;
			}
		}
		
		return lclChoices;
	}
	
	protected boolean isNewObjectForm() {
		return myNewObjectForm;
	}
}
