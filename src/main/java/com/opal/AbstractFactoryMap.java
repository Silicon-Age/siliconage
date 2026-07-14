package com.opal;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;

public abstract class AbstractFactoryMap implements Map<Class<? extends IdentityUserFacing>, IdentityFactory<?>> { // OPALFIXME
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
	
	/* The warning suppression here is a little frustrating.  Eclipse doesn't want me to pass an Object to the contains()
	 * method of the backing map, but I can't just instanceof Class? extends IdentityUserFacing> because that's not
	 * reifiable.  I could check to see that key is an instance of Class and that that Class is assignable to
	 * IdentityUserFacing, but that adds two type into something that might plausibly be called in a hot path.
	 */
	@Override
	@SuppressWarnings("unlikely-arg-type")
	public boolean containsKey(Object key) {
		return getMap().containsKey(key);
	}
	
	@Override
	@SuppressWarnings("unlikely-arg-type") // See note on containsKey()
	public boolean containsValue(Object value) {
		return getMap().containsValue(value);
	}
	
	@Override
	public Set<Map.Entry<Class<? extends IdentityUserFacing>, IdentityFactory<?>>> entrySet() {
		return getMap().entrySet();
	}
	
	@Override
	@SuppressWarnings("unlikely-arg-type") // See note on containsKey()
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
	@SuppressWarnings("unlikely-arg-type") // See note on containsKey()
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
