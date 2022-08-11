package com.siliconage.web.form;

public class LabelField<T extends LabelField<T>> extends SingleValueFormField<T, String> { // More like NoValueFormField, alas
	private static final String NAME_SUFFIX = "_label";
	
	// Note that a FormValueProvider cannot be given in the constructor, as it would have no meaning for a LabelField, which has no value.
	public LabelField(String argFieldName, String argContents) {
		super(argFieldName + NAME_SUFFIX, argContents);
	}
	
	public String getFor() {
		String lclName = getName();
		
		return lclName.substring(0, lclName.length() - NAME_SUFFIX.length());
	}
	
	public String getContents() {
		return getSavedValue();
	}
	
	@Override
	public void appendFormField(StringBuilder argSB) {
		argSB.append("<label")
			.append(outputAttribute("for", getFor()))
			.append(attributesToString())
			.append(outputCssClassAttribute())
			.append('>')
			.append(getContents())
			.append("</label>");
	}
}
