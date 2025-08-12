package com.opal.types;

import java.util.Collection;
import java.util.Iterator;

public interface TreeNode<T> {
	public T value();
	public Collection<TreeNode<T>> children();
	public TreeNode<T> parent();
	
	default boolean isLeaf() {
		return hasChildren() == false;
	}
	
	default public boolean isRoot() {
		return parent() == null;
	}
	
	public boolean hasChildren();

	default TreeNode<T> getRoot() {
		TreeNode<T> lclN = this;
		TreeNode<T> lclP = lclN.parent();
		while (lclP != null) {
			lclP = (lclN = lclP).parent();
		}
		return lclN;		
	}
	
	default public int getDepth() {
		return isRoot() ? 0 : parent().getDepth() + 1;
	}

	public Iterator<TreeNode<T>> childIterator();
	public Iterator<T> childValueIterator();
	
	public Iterator<TreeNode<T>> ancestorIterator();
	public Iterator<T> ancestorValueIterator();
	
	public Iterator<TreeNode<T>> descendantIterator();
	public Iterator<TreeNode<T>> descendantIterator(boolean argOrder, boolean argInclude);
	public Iterator<T> descendantValueIterator();
	public Iterator<T> descendantValueIterator(boolean argOrder, boolean argInclude);
}
