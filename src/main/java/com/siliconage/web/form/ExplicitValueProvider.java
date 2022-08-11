package com.siliconage.web.form;

import java.util.Collections;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import com.siliconage.util.Fast3Set;

public class ExplicitValueProvider implements FormValueProvider {
	private final long myLoadTime;
	private final Map<String, Collection<String>> myMap;
	private final Set<String> myDisabledFields;
	private final Set<String> myIncorrectFields;
	private final Map<String, FormFieldRequirement> myFieldRequirements;
	private final Set<Pair<String, String>> myCheckedNameValuePairs;
	
	public ExplicitValueProvider(Map<String, Collection<String>> argMap, long argLoadTime, Collection<String> argDisabledFields, Collection<String> argIncorrectFields, Map<String, FormFieldRequirement> argFieldRequirements, Collection<Pair<String, String>> argCheckedFields) {
		super();
		
		Validate.notNull(argMap); // but it could be empty. I guess.
		myMap = new HashMap<>(argMap); // defensive copy
		
		myLoadTime = argLoadTime;
		
		if (argDisabledFields == null || argDisabledFields.isEmpty()) {
			myDisabledFields = Collections.emptySet();
		} else {
			myDisabledFields = new Fast3Set<>(argDisabledFields);
		}
		
		if (argIncorrectFields == null || argIncorrectFields.isEmpty()) {
			myIncorrectFields = Collections.emptySet();
		} else {
			myIncorrectFields = new Fast3Set<>(argIncorrectFields);
		}
		
		if (argFieldRequirements == null || argFieldRequirements.isEmpty()) {
			myFieldRequirements = Collections.emptyMap();
		} else {
			myFieldRequirements = new HashMap<>(argFieldRequirements);
		}
		
		if (argCheckedFields == null || argCheckedFields.isEmpty()) {
			myCheckedNameValuePairs = Collections.emptySet();
		} else {
			myCheckedNameValuePairs = new Fast3Set<>(argCheckedFields);
		}
	}
	
	public ExplicitValueProvider(String... argInterleavedKeysAndValues) {
		myLoadTime = System.currentTimeMillis();
		
		Validate.notNull(argInterleavedKeysAndValues);
		Validate.isTrue(argInterleavedKeysAndValues.length % 2 == 0, "Must have an even number of arguments");
		
		myMap = new HashMap<>(argInterleavedKeysAndValues.length / 2);
		for (int lclI = 0; lclI < argInterleavedKeysAndValues.length; lclI += 2) {
			myMap.put(argInterleavedKeysAndValues[lclI], Collections.singleton(argInterleavedKeysAndValues[lclI + 1]));
		}
		
		myDisabledFields = Collections.emptySet();
		myIncorrectFields = Collections.emptySet();
		myFieldRequirements = Collections.emptyMap();
		myCheckedNameValuePairs = Collections.emptySet();
	}
	
	protected Map<String, Collection<String>> getMap() {
		return myMap;
	}
	
	protected Set<String> getDisabledFields() {
		return myDisabledFields;
	}
	
	protected Set<String> getIncorrectFields() {
		return myIncorrectFields;
	}
	
	protected Map<String, FormFieldRequirement> getFieldRequirements() {
		return myFieldRequirements;
	}
	
	protected Set<Pair<String, String>> getCheckedNameValuePairs() {
		return myCheckedNameValuePairs;
	}
	
	@Override
	public long getLoadTime() {
		return myLoadTime;
	}
	
	@Override
	public Collection<String> getAll(String argKey) {
		Validate.notNull(argKey);
		
		return getMap().get(argKey); // which may be null, either because argKey is literally mapped to null, or there is no mapping for argKey
	}
	
	@Override
	public boolean hasValueFor(String argKey) {
		return getMap().containsKey(argKey);
	}
	
	@Override
	public boolean isIncorrect(String argKey) {
		return getIncorrectFields().contains(argKey);
	}
	
//	@Override
//	public boolean isChecked(String argKey, String argValue) {
//		return getCheckedNameValuePairs().contains(Pair.of(argKey, argValue)); // relies on Pair.equals comparing argKey and argValue with equals()
//	}
	
	@Override
	public boolean isDisabled(String argKey) {
		return getDisabledFields().contains(argKey);
	}
	
	@Override
	public void setDisabled(String argKey, boolean argValue) {
		if (argValue) {
			getDisabledFields().add(argKey);
		} else {
			getDisabledFields().remove(argKey);
		}
	}
	
	@Override
	public FormFieldRequirement determineRequirement(String argKey) {
		return getFieldRequirements().getOrDefault(argKey, FormFieldRequirement.NOT_REQUIRED);
	}
}
