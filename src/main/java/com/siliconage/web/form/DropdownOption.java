package com.siliconage.web.form;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.siliconage.util.WebDataFilter;

public class DropdownOption<E extends DropdownEntry<?>> extends DropdownEntry<E> {
	private final String myValue;
	private boolean mySelected = false;
	private String myTitle = null;
	
	public DropdownOption(String argLabel, String argValue) {
		super(argLabel);
		
		myValue = StringUtils.isBlank(argValue) ? "" : argValue; // THINK: This once used " " instead of "".  Why did I do that?  In Chrome, it makes an unacceptable option acceptable, which is why I changed it back to "".
	}
	
	public String getValue() {
		return myValue;
	}
	
	protected String outputValueAttribute() {
		return " value=\"" + getValue() + "\"";
	}
	
	public E setSelected(boolean argSelected) {
		mySelected = argSelected;
		return castThis();
	}
	
	public E selected() {
		return setSelected(true);
	}
	
	public boolean isSelected() {
		return mySelected;
	}
	
	protected String outputSelectedAttribute() {
		if (isSelected()) {
			return " selected=\"selected\"";
		} else {
			return "";
		}
	}
	
	public E title(String argTitle) {
		if (StringUtils.isBlank(argTitle)) {
			myTitle = null;
		} else {
			myTitle = argTitle.trim();
		}
		
		return castThis();
	}
	
	protected String outputTitleAttribute() {
		if (myTitle == null) {
			return "";
		} else {
			return " title=\"" + StringEscapeUtils.escapeHtml4(myTitle) + "\"";
		}
	}
	
	@Override
	public String toString() {
		StringBuilder lclSB = new StringBuilder(); // We don't pre-estimate the size. Can we do so effectively?
		
		lclSB.append("<option")
			.append(outputValueAttribute())
			.append(outputSelectedAttribute())
			.append(outputDisabledAttribute())
			.append(outputCssClassAttribute())
			.append(outputStyleAttribute())
			.append(outputTitleAttribute())
			.append('>')
			.append(WebDataFilter.scrubForHTMLDisplay(getLabel()))
			.append("</option>");
		
		return lclSB.toString();
	}
	
	public static DropdownOption<?> instructions(String argInstructions, boolean argSelected) {
		return new DropdownOption<>(StringUtils.trimToEmpty(argInstructions), "").setSelected(argSelected).disable().addCssClass("placeholder").castThis();
	}
}
