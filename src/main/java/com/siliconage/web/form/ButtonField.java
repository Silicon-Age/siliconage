package com.siliconage.web.form;

public class ButtonField<T extends ButtonField<?>> extends HTMLInputField<T, String> {

	private static final String HTML_INPUT_TYPE = "submit";
	
	// Note that a FormValueProvider cannot be given in the constructor, as it would have no meaning for a ButtonField, since the user cannot change the value.
	// I played around the idea of introducing an EditableFormField class, but it introduced a diamond problem wherein we would want many fields to inherit from both that and HTMLInputField, but some HTMLInputField subclasses would not be EditableFormFields (namely HiddenField and ButtonField), and some non-HTMLInputField-subclasses would subclass EditableFormField (namely AbstractDropdownField).
	public ButtonField(String argName, String argLabel) {
		super(argName, argLabel);
	}

	public String getLabel() {
		return getSavedValue();
	}
	
	@Override
	public String valueAttribute() {
		return outputAttribute("value", getLabel());
	}
	
	@Override
	protected String getInputType() {
		return HTML_INPUT_TYPE;
	}

	@Override
	protected String fieldTypeSpecificAttributes() {
		return "";
	}
}
