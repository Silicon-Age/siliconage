package com.siliconage.tree;
import java.util.Iterator;
import java.util.function.Function;

import com.siliconage.util.NullIterator;

/**
 * @author topquark
 */
public class Tree<T> implements Iterable<T> {
	private TreeNode<T> myRootNode;
	
	public Tree() {
		this(null);
	}
	
	public Tree(T argO) {
		super();
		
		myRootNode = new TreeNode<>(argO);
	}
	
	public TreeNode<T> getRootNode() {
		return myRootNode;
	}
	
	public Iterator<TreeNode<T>> nodeIterator(boolean argOrder, boolean argInclude) {
		if (myRootNode == null) {
			return new NullIterator<>();
		} else {
			return myRootNode.descendantNodeIterator(argOrder, argInclude);
		}
	}
	
	public Iterator<T> valueIterator(boolean argOrder, boolean argInclude) {
		if (myRootNode == null) {
			return new NullIterator<>();
		} else {
			return myRootNode.descendantValueIterator(argOrder, argInclude);
		}
	}
	
	@Override
	public Iterator<T> iterator() {
		return valueIterator(true, true);
	}
	
	/* FIXME:  Possibly these should be T extends ? and ? extends U; I haven't quite internalized
	 * that notation yet.
	 */

	protected <U> void duplicateWithTransform(TreeNode<T> argSource, TreeNode<U> argDest, Function<? super T, ? extends U> argTransform) {
		argDest.set(argTransform.apply(argSource.get()));
		
		Iterator<TreeNode<T>> lclI = argSource.childNodeIterator();
		while (lclI.hasNext()) {
			duplicateWithTransform(lclI.next(), argDest.addChild(null), argTransform);
		}
	}
	
	public <U> Tree<U> duplicateWithTransform(Function<? super T, ? extends U> argTransform) {
		Tree<U> lclNewTree = new Tree<>();
		
		duplicateWithTransform(getRootNode(), lclNewTree.getRootNode(), argTransform);
		
		return lclNewTree;
	}
	
	public Iterable<TreeNode<T>> asNodeList() {
		return new Iterable<>() {
			@Override
			public Iterator<TreeNode<T>> iterator() {
				return nodeIterator(true, true);
			}
		};
	}
	
	public Iterable<T> asValueList() {
		return new Iterable<>() {
			@Override
			public Iterator<T> iterator() {
				return valueIterator(true, true);
			}
		};
	}
}
