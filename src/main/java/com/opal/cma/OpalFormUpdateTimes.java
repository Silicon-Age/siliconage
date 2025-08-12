package com.opal.cma;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Set;

import com.opal.IdentityUserFacing;

/**
 * @author jonah
 */
public class OpalFormUpdateTimes implements Map<IdentityUserFacing, Long>, Serializable {
	private static final long serialVersionUID = 1L;
	private static final OpalFormUpdateTimes ourInstance = new OpalFormUpdateTimes();
	
	public static OpalFormUpdateTimes getInstance() {
		return ourInstance;
	}
	
	private transient WeakHashMap<IdentityUserFacing, Long> myMap;
	
	private OpalFormUpdateTimes() {
		super();
		
		myMap = new WeakHashMap<>();
	}
	
	protected WeakHashMap<IdentityUserFacing, Long> getMap() {
		return myMap;
	}
	
	public void setUpdatedNow(IdentityUserFacing argUF) {
		this.put(argUF, System.currentTimeMillis());
	}
	
	@Override
	public void clear() {
		getMap().clear();
	}
	
	@Override
	public boolean containsKey(Object argKey) {
		return getMap().containsKey(argKey);
	}
	
	@Override
	public boolean containsValue(Object argValue) {
		return getMap().containsValue(argValue);
	}
	
	@Override
	public Set<Map.Entry<IdentityUserFacing, Long>> entrySet() {
		return getMap().entrySet();
	}
	
	@Override
	public Long get(Object argKey) {
		return getMap().get(argKey);
	}
	
	@Override
	public boolean isEmpty() {
		return getMap().isEmpty();
	}
	
	@Override
	public Set<IdentityUserFacing> keySet() {
		return getMap().keySet();
	}
	
	@Override
	public Long put(IdentityUserFacing argUF, Long argI) {
		return getMap().put(argUF, argI);
	}
	
	@Override
	public void putAll(Map<? extends IdentityUserFacing, ? extends Long> argMap) {
		getMap().putAll(argMap);
	}
	
	@Override
	public Long remove(Object argKey) {
		return getMap().remove(argKey);
	}
	
	@Override
	public int size() {
		return getMap().size();
	}
	
	@Override
	public Collection<Long> values() {
		return getMap().values();
	}
	
	private void readObject(ObjectInputStream argIn) throws IOException, ClassNotFoundException {
		argIn.defaultReadObject();
		myMap = new WeakHashMap<>();
	}
}
