package com.siliconage.util;
import java.util.Map;

/**
 * @author topquark
 */
public interface Factory<E> {
	public abstract E create() throws FactoryException;
	public abstract E create(Object argParameters) throws FactoryException;
	public abstract E create(Map<String, Object> argParameters) throws FactoryException;
//	public abstract E create(ReadableStringKeyMap argParameters) throws FactoryException;
//	public abstract ArrayList<E> createArrayList();
//	public abstract ArrayList<E> createArrayList(int argSize);
}
