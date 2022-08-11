package com.siliconage.web.form;

import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang3.Validate;

import com.siliconage.util.WebDataFilter;

// Think: Should this implement Iterable<DropdownEntry<?>> or List or Collection or something?
public class DropdownOptionGroup<E extends DropdownEntry<?>> extends DropdownEntry<E> {
	private final List<DropdownEntry<?>> myChildren;
	
	public DropdownOptionGroup(String argLabel, Collection<DropdownEntry<?>> argChildren) {
		super(argLabel);
		
		myChildren = new ArrayList<>(argChildren); // Defensive copy
	}
	
	public DropdownOptionGroup(String argLabel) {
		this(argLabel, Collections.emptyList());
	}
	
	public List<DropdownEntry<?>> getChildren() {
		return Collections.unmodifiableList(myChildren);
	}
	
	protected List<DropdownEntry<?>> getChildrenInternal() {
		return myChildren;
	}
	
	public E addAtBeginning(DropdownEntry<?> argEntry) {
		Validate.notNull(argEntry);
		
		getChildrenInternal().add(0, argEntry);
		
		return castThis();
	}
	
	public E add(DropdownEntry<?> argEntry) {
		Validate.notNull(argEntry);
		
		getChildrenInternal().add(argEntry);
		
		return castThis();
	}
	
	public int size() {
		return getChildrenInternal().size();
	}
	
	protected int calculateLikelyStringificationSize() {
		String lclCssClass = getCssClass(); // Avoid calling StringBuilder.toString() twice
		
		return
			32 + // for <optgroup label=""></optgroup> + newline
			(isDisabled() ? 20 : 0) + // disabled="disabled"
			(getStyle() == null ? 0 : 9 + getStyle().length()) + // style="" + the length of the contents + preceding space
			(lclCssClass == null ? 0 : 9 + lclCssClass.length()) + // class="" + the length of the contents + preceding space
			64 * getChildrenInternal().size(); // estimated
	}
	
	@Override
	public String toString() {
		StringBuilder lclSB = new StringBuilder(calculateLikelyStringificationSize());
		
		lclSB.append("<optgroup label=\"")
			.append(WebDataFilter.scrub(getLabel()))
			.append('"')
			.append(outputDisabledAttribute())
			.append(outputCssClassAttribute())
			.append(outputStyleAttribute())
			.append(">\n");
		
		for (DropdownEntry<?> lclE : getChildrenInternal()) {
			lclSB.append(lclE.toString()).append('\n');
		}
		
		lclSB.append("</optgroup>");
		
		return lclSB.toString();
	}
}
