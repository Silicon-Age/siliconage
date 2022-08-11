package com.siliconage.tree;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Iterator;
import com.siliconage.util.NullIterator;

/**
 * @author topquark
 */
public class TreeNode<T> {
	private T myValue;
	private TreeNode<T> myParentNode;
	private ArrayList<TreeNode<T>> myChildNodes;
	
	public T get() {
		return myValue;
	}
	
	public void set(T argO) {
		myValue = argO;
	}
	
	/* Used to create root nodes */
	
	/* package */ TreeNode(T argValue) {
		super();
		
		/* We don't invoke "this" constructor to avoid the null check. */
		setParentNode(null);
		set(argValue);
	}
	
	public TreeNode(TreeNode<T> argParentNode) {
		this(argParentNode, null);
	}
	
	public TreeNode(TreeNode<T> argParentNode, T argValue) {
		super();
		
		if (argParentNode == null) {
			throw new IllegalArgumentException("argParentNode is null");
		}
		
//		setTree(argParentNode.getTree());
		setParentNode(argParentNode);
		set(argValue);
	}
	
	/* package */ void setParentNode(TreeNode<T> argParentNode) {
		/* Check for being in the same tree */
		
		if (myParentNode != null) {
			myParentNode.removeChildNode(this);
		}
		
		myParentNode = argParentNode;
		
		if (argParentNode != null) {
			argParentNode.addChildNode(this);
		}
	}
	
	public TreeNode<T> getParentNode() {
		return myParentNode;
	}
		
	/* package */ TreeNode<T> addChildNode(TreeNode<T> argChildNode) {
		if (argChildNode == null) {
			throw new IllegalArgumentException("argChildNode is null");
		}
		
		if (myChildNodes == null) {
			myChildNodes = new ArrayList<>();
		}
		
		myChildNodes.add(argChildNode);
		
		return argChildNode;
	}
	
	/* package */ void removeChildNode(TreeNode<T> argChildNode) {
		if (argChildNode == null) {
			throw new IllegalArgumentException("argChildNode is null");
		}
		
		if (myChildNodes == null) {
			throw new IllegalArgumentException("Not a child");
		}
		
		if (!myChildNodes.remove(argChildNode)) {
			throw new IllegalArgumentException("Not a child");
		}
		
		if (myChildNodes.size() == 0) {
			myChildNodes = null;
		}
		
	}
	
	public TreeNode<T> addChild(T argObject) {
		return new TreeNode<>(this, argObject);
	}
	
	public int getChildNodeCount() {
		return myChildNodes == null ? 0 : myChildNodes.size();
	}
	
	@SuppressWarnings ("unchecked")
	public TreeNode<T>[] getChildNodeArray(TreeNode<T>[] argArray) {
		return myChildNodes == null ? (TreeNode<T>[]) new TreeNode<?>[0] : myChildNodes.toArray(argArray);
	}
	
	protected ArrayList<TreeNode<T>> getChildNodes() {
		return myChildNodes;
	}
	
	public Iterator<TreeNode<T>> childNodeIterator() {
		if (myChildNodes == null) {
			return new NullIterator<>();
		} else {				
			return new Iterator<>() {
				private Iterator<TreeNode<T>> myIterator = getChildNodes().iterator();
				
				@Override
				public boolean hasNext() {
					return myIterator.hasNext();
				}
				
				@Override
				public TreeNode<T> next() {
					return myIterator.next();
				}
				
				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}
	
	public Iterator<TreeNode<T>> descendantNodeIterator(boolean argOrder, boolean argInclude) {
		if (argInclude == false) {
			if (myChildNodes == null || myChildNodes.size() == 0) {
				return new NullIterator<>();
			}
		}
		
		return new DescendantNodeIterator(argOrder, argInclude);
	}
	
	private class DescendantNodeIterator implements Iterator<TreeNode<T>> {
		private final boolean myOrder;
//		final boolean myValues;
		private boolean myInclude;
		private Iterator<TreeNode<T>> myChildIterator = null;
		private Iterator<TreeNode<T>> myDescendantIterator = null;
		private boolean myHasNext;
		private TreeNode<T> myNext;
		
		private DescendantNodeIterator(boolean argOrder, boolean argInclude) {
			super();
			myOrder = argOrder;
//			myValues = argValues;
			myInclude = argInclude;
			
			advance();
		}
		
		private void advance() {
			if (myOrder) {
				if (myInclude) {
					myInclude = false;
					myNext = TreeNode.this; /* How do I make this work without a cast? */
					myHasNext = true;
					return;
				}
			}
			if (myDescendantIterator == null) {
				myChildIterator = TreeNode.this.childNodeIterator(); /* And this? */
			}
			while ((myDescendantIterator == null || !myDescendantIterator.hasNext()) && myChildIterator.hasNext()) {
				myDescendantIterator = myChildIterator.next().descendantNodeIterator(myOrder, true);
			}
			if (myDescendantIterator == null || !myDescendantIterator.hasNext()) {
				if (!myOrder) {
					if (myInclude) {
						myInclude = false;
						myNext = TreeNode.this; /* And this? */
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
//			return myValues ? ((TreeNode) lclO).getValue() : lclO;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
		
	public Iterator<T> descendantValueIterator(boolean argOrder, boolean argInclude) {
		return new DescendantValueIterator(argOrder, argInclude);
	}
	
	private class DescendantValueIterator implements Iterator<T> {
		private final Iterator<TreeNode<T>> myIterator;
		
		private DescendantValueIterator(boolean argOrder, boolean argInclude) {
			super();
			myIterator = descendantNodeIterator(argOrder, argInclude);
		}
		
		@Override
		public boolean hasNext() {
			return myIterator.hasNext();
		}
		
		@Override
		public T next() {
			return myIterator.next().get();
		}
		
		@Override
		public void remove() {
			myIterator.remove();
		}
	}
	
	public int getLevel() {
		TreeNode<T> lclParent = getParentNode();
		return lclParent == null ? 0 : 1 + lclParent.getLevel();
	}
}
