package com.opal.types;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractTreeNode<T> implements TreeNode<T> {

	/* This next method may be quite inefficient; subclasses are encouraged to override it! */
	@Override
	public boolean hasChildren() {
		return childIterator().hasNext();
	}
	
	@Override
	public Iterator<TreeNode<T>> childIterator() {
		return children().iterator();
	}
	
	@Override
	public Iterator<T> childValueIterator() {
		return new ValueIterator<>(childIterator());
	}
	
	protected static class ValueIterator<T> implements Iterator<T> {
		private final Iterator<TreeNode<T>> myIterator;
		
		protected ValueIterator(Iterator<TreeNode<T>> argIterator) {
			super();
			
			myIterator = argIterator;
		}
		
		@Override
		public boolean hasNext() {
			return myIterator.hasNext();
		}
		
		@Override
		public T next() {
			TreeNode<T> lclNext = myIterator.next();
			if (lclNext == null) {
				return null;
			} else {
				return lclNext.value();
			}
		}
		
		@Override
		public void remove() {
			myIterator.remove();
		}
	}
	
	@Override
	public Iterator<TreeNode<T>> ancestorIterator() {
		return new AncestorIterator<>(this);
	}
	
	@Override
	public Iterator<T> ancestorValueIterator() {
		return new ValueIterator<>(ancestorIterator());
	}
	
	protected static class AncestorIterator<T> implements Iterator<TreeNode<T>> {
		private TreeNode<T> myTreeNode;
		
		protected AncestorIterator(TreeNode<T> argStartNode) {
			super();
			
			myTreeNode = argStartNode;
		}
		
		@Override
		public boolean hasNext() {
			return myTreeNode.isRoot() == false;
		}
		
		@Override
		public TreeNode<T> next() {
			if (hasNext() == false) {
				throw new NoSuchElementException("Iterator has reached the root of the tree.");
			}
			myTreeNode = myTreeNode.parent();
			
			return myTreeNode;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException("What would this even mean?");
		}
		
	}
	
	@Override
	public Iterator<TreeNode<T>> descendantIterator() {
		return descendantIterator(true, true);
	}
	
	@Override
	public Iterator<TreeNode<T>> descendantIterator(boolean argOrder, boolean argInclude) {
		return new DescendantIterator<>(this, argOrder, argInclude);
	}
	
	@Override
	public Iterator<T> descendantValueIterator() {
		return descendantValueIterator(true, true);
	}
	
	@Override
	public Iterator<T> descendantValueIterator(boolean argOrder, boolean argInclude) {
		return new ValueIterator<>(descendantIterator(argOrder, argInclude));
	}
	
	protected static class DescendantIterator<T> implements Iterator<TreeNode<T>> {
		private final boolean myOrder;
		private boolean myInclude;
		private Iterator<TreeNode<T>> myChildIterator = null;
		private Iterator<TreeNode<T>> myDescendantIterator = null;
		private boolean myHasNext;
		private TreeNode<T> myNext;
		private final TreeNode<T> myStartNode;
		
		private DescendantIterator(TreeNode<T> argStartNode) {
			this(argStartNode, true, true);
		}
		
		private DescendantIterator(TreeNode<T> argStartNode, boolean argOrder, boolean argInclude) {
			super();
			
			myOrder = argOrder;
			myInclude = argInclude;
			myStartNode = argStartNode;
			advance();
		}
		
		private void advance() {
			if (myOrder) {
				if (myInclude) {
					myInclude = false;
					myHasNext = true;
					myNext = myStartNode;
					
					return;
				}
			}
			if (myDescendantIterator == null) {
				myChildIterator = myStartNode.childIterator();
			}
			while ((myDescendantIterator == null || !myDescendantIterator.hasNext()) && myChildIterator.hasNext()) {
				myDescendantIterator = myChildIterator.next().descendantIterator(myOrder, true);
			}
			if (myDescendantIterator == null || !myDescendantIterator.hasNext()) {
				if (!myOrder) {
					if (myInclude) {
						myInclude = false;
						myNext = myStartNode; /* And this? */
						myHasNext = true;
						return;
					}
				}
				myNext = null;
				myHasNext = false;
			} else {
				myHasNext = true;
				myNext = myDescendantIterator.next();
			}
		}
		
		@Override
		public boolean hasNext() {
			return myHasNext;
		}
		
		@Override
		public TreeNode<T> next() {
			if (!myHasNext) {
				throw new NoSuchElementException("next() called when hasNext() is false");
			}
			TreeNode<T> lclO = myNext;
			advance();
			return lclO;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public String toString() {
		return value().toString();
	}
	
	@Override
	public int hashCode() {
		return value().hashCode();
	}
	
	@Override
	public boolean equals(Object argO) {
		if (argO instanceof TreeNode) {
			return value().equals(((TreeNode<?>) argO).value());
		} else {
			return false;
		}
	}
	
//	protected static <T> Iterator<TreeNode<T>> nodeIterator(Iterable<T> argIterable) {
//		return new NodeIterator<>(argIterable);
//	}
//	
//	protected static class NodeIterator<T> implements Iterator<TreeNode<T>> {
//		
//		private final Iterator<T> myIterator; 
//
//		protected NodeIterator(Iterable<T> argIterable) {
//			this (argIterable.iterator());
//		}
//		
//		protected NodeIterator(Iterator<T> argIterator) {
//			super();
//			myIterator = argIterator;
//		}
//		
//		@Override
//		public boolean hasNext() {
//			return myIterator.hasNext();
//		}
//		
//		@Override
//		public TreeNode<T> next() {
//			return myIterator.next().asTreeNode();
//		}
//		
//		@Override
//		public void remove() {
//			myIterator.remove();
//		}
//	}
}
