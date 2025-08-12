package com.opal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;

public class Tree<T> implements Iterable<T> {
	private final T myNode;
	private final TreeAdapter<T> myAdapter;
	
	public Tree(T argNode, TreeAdapter<T> argAdapter) {
		super();
		
		myNode = argNode;
		myAdapter = argAdapter;
	}
	
	public T getNode() {
		return myNode;
	}
	
	public TreeAdapter<T> getAdapter() {
		return myAdapter;
	}
	
	public boolean isRoot() {
		return getParent() == null; /* TODO: Should we also handle implementations where the root's parent is itself? */
	}
	
	public ListIterator<T> childListIterator() {
		return getAdapter().childIterator(getNode());
	}
	
	public T getParent() {
		return getAdapter().getParent(getNode());
	}
	
	/* TODO: This is inefficient */
	public boolean isLeaf() {
		return getAdapter().getChildSet(getNode()).isEmpty();
	}
	
	public boolean isDescendantOf(T argParent) {
		T lclNode = this.getNode();
		do {
			if (lclNode == argParent) {
				return true;
			}
			lclNode = getAdapter().getParent(lclNode);
		} while (lclNode != null);
		return false;
	}
	
	@Override
	public Iterator<T> iterator() {
		return descendantIterator();
	}
	
	public TreeIterator<T> descendantIterator() {
		return descendantIterator(false);
	}
	
	public TreeIterator<T> descendantIterator(boolean argIncludeSelf) {
		return new DescendantIterator(argIncludeSelf);
	}
	
	private class DescendantIterator implements TreeIterator<T> {
		private ArrayList<Iterator<T>> myStack = new ArrayList<>();
		
		private DescendantIterator(boolean argIncludeSelf) {
			super();
			if (argIncludeSelf) {
				myStack.add(Collections.singletonList(getNode()).iterator());
			} else {
				myStack.add(getAdapter().childIterator(getNode()));
			}
		}
		
		@Override
		public boolean hasNext() {
			int lclI = myStack.size();
			while (lclI > 0) {
				if (myStack.get(--lclI).hasNext()) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public T next() {
			if (!hasNext()) {
				throw new NoSuchElementException("next() called when hasNext() is false");
			}
			int lclI = myStack.size();
			while (lclI > 0) {
				Iterator<T> lclIterator = myStack.get(--lclI);
				if (lclIterator.hasNext()) {
					T lclT = lclIterator.next();
					myStack.add(getAdapter().childIterator(lclT));
					return lclT;
				} else {
					myStack.remove(lclI);
				}
			}
			throw new IllegalStateException("Should never get here"); 
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	/* FIXME: Inefficient */
	public int getDepth() {
		int lclDepth = 0;
		Iterator<T> lclAI = ancestorIterator();
		while (lclAI.hasNext()) {
			lclAI.next();
			++lclDepth;
		}
		
		return lclDepth;
	}
	
	public Iterator<T> ancestorIterator() {
		return ancestorIterator(false);
	}
	
	public Iterator<T> ancestorIterator(boolean argIncludeSelf) {
		return new AncestorIterator(argIncludeSelf);
	}
	
	public class AncestorIterator implements Iterator<T> {
		private T myIteratingNode;
		
		private AncestorIterator(boolean argIncludeSelf) {
			super();
		
			if (argIncludeSelf) {
				myIteratingNode = myNode;
			} else {
				myIteratingNode = getAdapter().getParent(myNode);
			}
		}
		
		@Override
		public boolean hasNext() {
			return myIteratingNode != null && getAdapter().getParent(myIteratingNode) != null;
		}
		
		@Override
		public T next() {
			if (hasNext()) {
				return (myIteratingNode = getAdapter().getParent(myIteratingNode));
			} else {
				throw new NoSuchElementException();
			}
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	public List<T> ancestors() {
		return ancestors(false);
	}
	
	public List<T> ancestors(boolean argIncludeMyself) {
		ArrayList<T> lclAncestors = new ArrayList<>(10);
		acquireAncestors(lclAncestors, argIncludeMyself);
		return lclAncestors;
	}
	
	public void acquireAncestors(Collection<T> argCollection, boolean argIncludeMyself) {
		Validate.notNull(argCollection);
		
		Iterator<T> lclAI = ancestorIterator(argIncludeMyself);
		while (lclAI.hasNext()) {
			argCollection.add(lclAI.next());
		}
	}
	
	public void acquireAncestors(Collection<T> argCollection) {
		acquireAncestors(argCollection, false);
	}
	
	public Stream<T> streamChildren() {
		return getAdapter().getOrderedChildList(getNode()).stream();
	}
	
	public Stream<T> streamDescendants() {
		return streamDescendants(false);
	}
	
	public Stream<T> streamDescendantsIncludingMyself() {
		return streamDescendants(true);
	}
	
	/* FIXME: This is so horrendously offensive from a performance point of view that it makes me cry to have written it.
	 * I need to come back later and figure out a better way to generate this stream without putting everything into a
	 * temporary List.  This might end up being the first time that I actually implement the Stream interface.
	 */
	public Stream<T> streamDescendants(boolean argIncludeSelf) {
		List<T> lclList = new ArrayList<>();
		Iterator<T> lclDI = descendantIterator(argIncludeSelf);
		while (lclDI.hasNext()) {
			lclList.add(lclDI.next());
		}
		return lclList.stream();
	}

	public Stream<T> streamAncestors() {
		return streamAncestors(false);
	}
	
	public Stream<T> streamAncestorsIncludingMyself() {
		return streamAncestors(true);
	}
	
	public Stream<T> streamAncestors(boolean argIncludeSelf) {
		List<T> lclList = new ArrayList<>();
		Iterator<T> lclAI = ancestorIterator(argIncludeSelf);
		while (lclAI.hasNext()) {
			lclList.add(lclAI.next());
		}
		return lclList.stream();
	}
}
