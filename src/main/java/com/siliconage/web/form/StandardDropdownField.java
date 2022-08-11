package com.siliconage.web.form;

import java.util.Collections;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang3.Validate;

public class StandardDropdownField<T extends StandardDropdownField<?, C>, C> extends DropdownField<T, C> {
	private List<DropdownEntry<?>> myPreEntries = new ArrayList<>();
	private List<DropdownEntry<?>> myEntries = new ArrayList<>();
	private List<DropdownEntry<?>> myPostEntries = new ArrayList<>();
	
	public StandardDropdownField(String argName, Collection<C> argSavedValues, FormValueProvider argEnteredValueProvider) {
		super(argName, argSavedValues, argEnteredValueProvider);
	}
	
	public StandardDropdownField(String argName, C argSavedValue, FormValueProvider argEnteredValueProvider) {
		this(argName, Collections.singleton(argSavedValue), argEnteredValueProvider);
	}
	
	protected T add(List<DropdownEntry<?>> argTarget, Collection<? extends DropdownEntry<?>> argSource) {
		Validate.notNull(argTarget);
		Validate.notNull(argSource);
		
		argTarget.addAll(argSource);
		
		return castThis();
	}
	
	public T addPre(DropdownEntry<?>... argEntries) {
		return add(myPreEntries, Arrays.asList(argEntries));
	}
	
	public T addPre(Collection<? extends DropdownEntry<?>> argEntries) {
		return add(myPreEntries, argEntries);
	}
	
	public T addPre(DropdownEntry<?> argEntry) {
		return add(myPreEntries, Collections.singleton(argEntry));
	}
	
	public List<DropdownEntry<?>> getPreEntries() {
		return myPreEntries;
	}
	
	public T add(DropdownEntry<?>... argEntries) {
		return add(myEntries, Arrays.asList(argEntries));
	}
	
	public T add(Collection<? extends DropdownEntry<?>> argEntries) {
		return add(myEntries, argEntries);
	}
	
	public T add(DropdownEntry<?> argEntry) {
		return add(myEntries, Collections.singleton(argEntry));
	}
	
	public T entries(DropdownEntry<?>... argEntries) {
		return entries(Arrays.asList(argEntries));
	}
	
	public T entries(Collection<? extends DropdownEntry<?>> argEntries) {
		myEntries = new ArrayList<>(argEntries);
		
		return castThis();
	}
	
	public List<DropdownEntry<?>> getEntries() {
		return myEntries;
	}
	
	public T addPost(DropdownEntry<?>... argEntries) {
		return add(myPostEntries, Arrays.asList(argEntries));
	}
	
	public T addPost(Collection<? extends DropdownEntry<?>> argEntries) {
		return add(myPostEntries, argEntries);
	}
	
	public T addPost(DropdownEntry<?> argEntry) {
		return add(myPostEntries, Collections.singleton(argEntry));
	}
	
	public List<DropdownEntry<?>> getPostEntries() {
		return myPostEntries;
	}
	
	@Override
	protected boolean isAnyOptionSelected() {
		if (getPreEntries().stream().anyMatch(argO -> argO instanceof DropdownOption && ((DropdownOption<?>) argO).isSelected())) {
			return true;
		}
		
		if (getEntries().stream().anyMatch(argO -> argO instanceof DropdownOption && ((DropdownOption<?>) argO).isSelected())) {
			return true;
		}
		
		if (getPostEntries().stream().anyMatch(argO -> argO instanceof DropdownOption && ((DropdownOption<?>) argO).isSelected())) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public List<DropdownEntry<?>> determineEntries() {
		List<DropdownEntry<?>> lclMain = determineMainEntries();
		
		List<DropdownEntry<?>> lclAll = new ArrayList<>(getPreEntries().size() + lclMain.size() + getPostEntries().size());
		lclAll.addAll(getPreEntries());
		lclAll.addAll(lclMain);
		lclAll.addAll(getPostEntries());
		
		return lclAll;
	}
	
	public List<DropdownEntry<?>> determineMainEntries() {
		return Collections.unmodifiableList(myEntries);
	}
}
