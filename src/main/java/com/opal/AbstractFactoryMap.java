package com.opal;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;

public abstract class AbstractFactoryMap implements Map<Class<? extends IdentityUserFacing>, IdentityFactory<?>> {
	// Rather than extend HashMap, we implement Map and delegate everything to myMap.
	// This is to avoid being Serializable.
	
	public static final String NO_ARG_CTOR_SET_CREATOR_METHOD_NAME = "getNoArgCtorSetCreator";
	public static final String COLLECTION_ARG_CTOR_SET_CREATOR_METHOD_NAME = "getCollectionArgSetCreator";

	private final HashMap<Class<? extends IdentityUserFacing>, IdentityFactory<?>> myMap = new HashMap<>();
	
	protected AbstractFactoryMap() {
		super();
		
		initialize();
	}
	
	protected abstract void initialize();
	
	protected Map<Class<? extends IdentityUserFacing>, IdentityFactory<?>> getMap() {
		return myMap;
	}
	
	// Delegating methods
	@Override
	public void clear() {
		getMap().clear();
	}
	
	@Override
	public boolean containsKey(Object key) {
		return getMap().containsKey(key);
	}
	
	@Override
	public boolean containsValue(Object value) {
		return getMap().containsValue(value);
	}
	
	@Override
	public Set<Map.Entry<Class<? extends IdentityUserFacing>, IdentityFactory<?>>> entrySet() {
		return getMap().entrySet();
	}
	
	@Override
	public IdentityFactory<?> get(Object key) {
		return getMap().get(key);
	}
	
	@Override
	public boolean isEmpty() {
		return getMap().isEmpty();
	}
	
	@Override
	public Set<Class<? extends IdentityUserFacing>> keySet() {
		return getMap().keySet();
	}
	
	@Override
	public IdentityFactory<?> put(Class<? extends IdentityUserFacing> key, IdentityFactory<?> value) {
		return getMap().put(key, value);
	}
	
	@Override
	public void putAll(Map<? extends Class<? extends IdentityUserFacing>, ? extends IdentityFactory<?>> m) {
		getMap().putAll(m);
	}
	
	@Override
	public IdentityFactory<?> remove(Object key) {
		return getMap().remove(key);
	}
	
	@Override
	public int size() {
		return getMap().size();
	}
	
	@Override
	public Collection<IdentityFactory<?>> values() {
		return getMap().values();
	}
	
	@Override
	public boolean equals(Object that) {
		return that instanceof AbstractFactoryMap && this.getMap().equals(((AbstractFactoryMap) that).getMap());
	}
	
	@Override
	public int hashCode() {
		return getMap().hashCode();
	}
}
