package com.siliconage.web.form;

import java.util.Collection;

import com.siliconage.web.HTMLUtility;

public abstract class CheckableField<T extends CheckableField<?>> extends HTMLInputField<T, Boolean> {
	// private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(CheckableField.class.getName());
	public static final String ID_NAME_VALUE_SEPARATOR = "_";

	private String myHTMLValueAttribute;
	
	// I played around the idea of introducing an EditableFormField class, but it introduced a diamond problem wherein we would want many fields to inherit from both that and HTMLInputField, but some HTMLInputField subclasses would not be EditableFormFields (namely HiddenField and ButtonField), and some non-HTMLInputField-subclasses would subclass EditableFormField (namely AbstractDropdownField).

	protected CheckableField(String argName, String argValue, boolean argChecked, FormValueProvider argEnteredValueProvider) {
		super(argName, Boolean.valueOf(argChecked), argEnteredValueProvider);

		myHTMLValueAttribute = argValue == null ? "" : argValue;
	}
	
	protected CheckableField(String argName, boolean argChecked, FormValueProvider argEnteredValueProvider) {
		this(argName, HTMLUtility.DEFAULT_TRUE_STRING, argChecked, argEnteredValueProvider);
	}
	
	protected CheckableField(String argName, FormValueProvider argEnteredValueProvider) {
		this(argName, HTMLUtility.DEFAULT_TRUE_STRING, false, argEnteredValueProvider);
	}
	
	public boolean isChecked() {
		// ourLogger.info("Asking whether " + getName() + " is checked.");
		// ourLogger.info("Entered value provider: " + getEnteredValueProvider());
		if (getEnteredValueProvider() == null) {
			// ourLogger.info("No EVP");
			return savedValueMatch();
		} else if (getEnteredValueProvider().hasValueFor(getName())) {
			Collection<String> lclValues = getEnteredValues();
			// ourLogger.info("EVP has a value.  lclValues = " + lclValues);
			if (lclValues == null) {
				// ourLogger.info("getSavedValue() == " + getSavedValue().booleanValue());
				return false;
			} else {
				// ourLogger.info("getEnteredValue() == " + lclValues.contains(getHTMLValueAttribute()));;
				return lclValues.contains(getHTMLValueAttribute());
			}
		} else {
			// ourLogger.info("No match in EVP");
			// ourLogger.info("Using saved value: " + getSavedValue());
			return savedValueMatch();
		}
	}
	
	protected boolean savedValueMatch() {
		return getSavedValue().booleanValue();
	}
	
	// public T setChecked(boolean argV) {
		// myChecked = argV;
		
		// return castThis();
	// }
	
	// public T checked() {
		// return setChecked(true);
	// }
	
	// public T unchecked() {
		// return setChecked(false);
	// }
	
	public String getHTMLValueAttribute() {
		return myHTMLValueAttribute;
	}
	
	@Override
	public String getId() {
		if (usingDefaultId()) {
			return getName() + ID_NAME_VALUE_SEPARATOR + getHTMLValueAttribute();
		} else {
			return getCustomId();
		}
	}
	
	@Override
	protected String valueAttribute() {
		return outputAttribute("value", getHTMLValueAttribute());
	}
	
	protected String checkedAttribute() {
		return isChecked() ? " checked=\"checked\"" : ""; // Note initial space
	}
	
	@Override
	protected String fieldTypeSpecificAttributes() {
		return checkedAttribute();
	}
}
