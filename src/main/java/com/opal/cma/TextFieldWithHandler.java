package com.opal.cma;

import org.apache.commons.lang3.Validate;

import com.siliconage.web.form.TextField;

public class TextFieldWithHandler<T extends TextFieldWithHandler<?>> extends TextField<T> {
	private final OpalForm<?> myForm;
	private final SpecialHandler<?> myHandler;
	private final String myUnprefixedName;
	
	public TextFieldWithHandler(OpalForm<?> argOF, String argName, int argSize, SpecialHandler<?> argHandler) {
		super(argOF.generateFullyQualifiedName(argName), null, argOF, argSize);
		
		myForm = argOF; // No sense in Validate.notNull()'ing, since the superclass constructor call will already have thrown an NPE if argOF == null
		myUnprefixedName = Validate.notNull(argName);
		myHandler = Validate.notNull(argHandler);
	}
	
	public OpalForm<?> getOpalForm() {
		return myForm;
	}

	public SpecialHandler<?> getHandler() {
		return myHandler;
	}
	
	@Override
	protected void addFieldTypeSpecificHiddenParameters() {
		hiddenParameter(getOpalForm().generateFullyQualifiedName("SpecialHandler_" + myUnprefixedName), getHandler().getClass().getName()); // THINK: Should SpecialHandler_ be a suffix?
	}
	
	@Override
	public String determineStringToDisplay() {
		String lclCurrentValue;
		if (getOpalForm().alreadyExists() == false) {
			lclCurrentValue = getHandler().getDefault(getOpalForm(), myUnprefixedName);
		} else { /* FIXME:  Would this be better using reflection? */
			lclCurrentValue = getHandler().getDisplay(getOpalForm(), myUnprefixedName);
		}
		
		return lclCurrentValue;
	}
}
