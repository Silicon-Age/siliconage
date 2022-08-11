package com.siliconage.web.form;

public class HiddenField<T extends HiddenField<?>> extends HTMLInputField<T, String> {
	private static final String HTML_INPUT_TYPE = "hidden";
	
	// Note that a FormValueProvider cannot be given in the constructor, as it would have no meaning for a CheckboxField, since the user cannot change the value.
	// I played around with the idea of introducing an EditableFormField class, but it introduced a diamond problem wherein we would want many fields to inherit from both that and HTMLInputField, but some HTMLInputField subclasses would not be EditableFormFields (namely HiddenField and ButtonField), and some non-HTMLInputField-subclasses would subclass EditableFormField (namely AbstractDropdownField).
	public HiddenField(String argName, Object argValue) {
		super(argName, argValue == null ? null : argValue.toString());
	}
	
	@Override
	protected String getInputType() {
		return HTML_INPUT_TYPE;
	}
	
	@Override
	protected boolean mayThereBeOtherFieldsWithTheSameNameAsThisOne() {
		return true;
	}
	
	@Override
	protected String disabledAttribute() {
		return "";
	}
	
	@Override
	protected String fieldTypeSpecificAttributes() {
		return "";
	}
	
	@Override
	protected String valueAttribute() {
		return outputAttribute("value", getSavedValue());
	}
	
	// FIXME: This method should probably not even exist.  Do we need a DisplayableHTMLInputField that is not a superclass of HiddenField?
	@Override
	protected String outputCssClassAttribute() {
		return "";
	}
	
	// FIXME: This method should probably not even exist.  Do we need a DisplayableHTMLInputField that is not a superclass of HiddenField?
	@Override
	protected String onChangeScript() {
		return "";
	}
	
	@Override
	protected void addRequirement() {
		return;
	}
}
