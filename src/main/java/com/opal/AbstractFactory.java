package com.opal;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @author topquark
 */
public abstract class AbstractFactory<U extends UserFacing/*<U>*/, O extends Opal<U>> implements Factory<U> { // OPALFIXME
	private final OpalFactory<U, O> myOpalFactory;
	
	protected AbstractFactory(OpalFactory<U, O> argOF) {
		myOpalFactory = Objects.requireNonNull(argOF);
	}
	
	protected OpalFactory<U, O> getOpalFactory() {
		return myOpalFactory;
	}
	
	@Override
	public Set<U> getAll() {
		return getOpalFactory().getAll().stream().map(Opal::getUserFacing).collect(Collectors.toSet());
	}
	
	@Override
	public abstract U[] createArray(int argSize);
	
	@Override
	public <T extends Collection<? super U>> T acquireForQuery(T argCollection, Query argQuery) {
		Objects.requireNonNull(argCollection);
		Objects.requireNonNull(argQuery);
		
		ArrayList<O> lclAL = new ArrayList<>();
		getOpalFactory().acquireForQuery(lclAL, argQuery);
		lclAL.stream().map(Opal::getUserFacing).forEach(argCollection::add);

		return argCollection;
	}
	
	@Override
	public U getForQuery(Query argQuery) {
		O lclOpal = getOpalFactory().getOpalForQuery(argQuery);
		return lclOpal != null ? lclOpal.getUserFacing() : null;
	}
	
	@Override
	public int getFieldCount() {
		return getOpalFactory().getFieldCount();
	}
	
	@Override
	public String getFieldName(int argFieldIndex) {
		return getOpalFactory().getFieldName(argFieldIndex);
	}
	
	@Override
	public Class<?> getFieldType(int argFieldIndex) {
		return getOpalFactory().getFieldType(argFieldIndex);
	}
	
	@Override
	public boolean getFieldNullability(int argFieldIndex) {
		return getOpalFactory().getFieldNullability(argFieldIndex);
	}
	
	@Override
	public FieldValidator getFieldValidator(int argFieldIndex) {
		return getOpalFactory().getFieldValidator(argFieldIndex);
	}
	
	@Override
	public int getFieldIndex(String argFieldName) {
		return getOpalFactory().getFieldIndex(argFieldName);
	}
}
