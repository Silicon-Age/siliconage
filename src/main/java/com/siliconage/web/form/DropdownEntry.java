package com.siliconage.web.form;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public abstract class DropdownEntry<E extends DropdownEntry<?>> {
	private final String myLabel;
	private boolean myDisabled = false;
	private StringBuilder myCssClass = null;
	private String myStyle = null;
	
	protected DropdownEntry(String argLabel) {
		super();
		
		String lclLabel = StringUtils.isBlank(argLabel) ? "-" : argLabel;
		myLabel = lclLabel.trim();
	}
	
	public String getLabel() {
		return myLabel;
	}
	
	public boolean isDisabled() {
		return myDisabled;
	}
	
	public E setDisabled(boolean argV) {
		myDisabled = argV;
		return castThis();
	}
	
	public E disable() {
		return setDisabled(true).castThis();
	}
	
	public E enable() {
		return setDisabled(false).castThis();
	}
	
	protected String outputDisabledAttribute() {
		if (isDisabled()) {
			return " disabled=\"disabled\"";
		} else {
			return "";
		}
	}
	
	public String getCssClass() {
		if (myCssClass == null) {
			return null;
		} else {
			return myCssClass.toString();
		}
	}
	
	public E addCssClass(String argClass) {
		Validate.notBlank(argClass);
		
		if (myCssClass == null) {
			myCssClass = new StringBuilder(argClass.length()); // will require expansion if more classes are added later
		}
		
		if (myCssClass.length() > 0) {
			myCssClass.append(' ');
		}
		
		myCssClass.append(argClass.trim());
		
		return castThis();
	}
	
	protected String outputCssClassAttribute() {
		String lclClass = getCssClass();
		
		if (StringUtils.isBlank(lclClass)) { // It should be either null or non-blank, but just in case
			return "";
		} else {
			return " class=\"" + lclClass + "\"";
		}
	}
	
	public E style(String argStyle) {
		myStyle = Validate.notBlank(argStyle);
		return castThis();
	}
	
	public String getStyle() {
		return myStyle; // May be null
	}
	
	protected String outputStyleAttribute() {
		if (StringUtils.isBlank(getStyle())) { // It should be either null or non-blank, but just in case
			return "";
		} else {
			return " style=\"" + getStyle() + "\"";
		}
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends DropdownEntry<?>> T castThis() {
		return (T) this;
	}
	
	// Force subclasses to override this
	@Override
	public abstract String toString();
}
