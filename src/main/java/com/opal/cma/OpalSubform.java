package com.opal.cma;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.util.Trinary;
import com.siliconage.web.HTMLUtility;
import com.siliconage.web.form.FormField;
import com.siliconage.web.form.FormFieldRequirement;
import com.siliconage.web.form.NullField;
import com.siliconage.web.form.PriorInput;

import com.opal.IdentityFactory;
import com.opal.IdentityUserFacing;

public class OpalSubform<U extends IdentityUserFacing> extends OpalForm<U> {
	// private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalSubform.class.getName());
	
	private final OpalForm<?> myParent;
	
	private final IdentityFactory<U> myFactory;
	private final U myUserFacing;
	
	private Trinary myDisabledCompletely = Trinary.UNKNOWN; // UNKNOWN -> use parent
	
	protected OpalSubform(OpalForm<?> argParent, String argLocalPrefix, U argUF, IdentityFactory<U> argFactory) {
		super(argLocalPrefix);
		
		Validate.notNull(argParent);
		myParent = argParent;
		
		myUserFacing = argUF;
		
		Validate.notNull(argFactory);
		myFactory = argFactory;
	}
	
	public OpalForm<?> getParent() {
		return myParent;
	}
	
	@Override
	public IdentityFactory<U> getFactory() {
		return myFactory;
	}
	
	@Override
	public String getPrefix() {
		return getParent().getPrefix() + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + getLocalPrefix();
	}
	
	@Override
	protected PriorInput getPriorInput() {
		return getParent().getPriorInput();
	}
	
	@Override
	public HttpServletRequest getRequest() {
		return getParent().getRequest();
	}
	
	@Override
	public HttpSession getSession() {
		return getParent().getSession();
	}
	
	@Override
	public U getUserFacing() {
		return myUserFacing;
	}
	
	@Override
	protected String beforeOpen() {
		return "";
	}
	
	@Override
	protected String openInternal() {
		return "";
	}
	
	@Override
	protected String afterOpen() {
		getParent().outputUponClose(generateIdentifyingFields());
		
		return "";
	}
	
	@Override
	public String close() {
		setClosed();
		getParent().outputUponClose(generateHiddenIndicatorsOfEnabledDisplayedFields());
		
		return "";
	}
	
	@Override
	protected void outputUponClose(String argS) {
		getParent().outputUponClose(argS);
	}
	
	@Override
	protected void recordDescendant(OpalForm<?> argOF) {
		getParent().recordDescendant(argOF);
	}
	
	@Override
	public FormField<?, Boolean> delete() {
		return delete(getConfiguration().getDefaultSubformDeleteLabel(), getConfiguration().getDefaultSubformUndeleteLabel(), null);
	}
	
	@Override
	public FormField<?, Boolean> delete(String argValue) {
		return delete(getConfiguration().getDefaultSubformDeleteLabel(), getConfiguration().getDefaultSubformUndeleteLabel(), argValue);
	}
	
	public FormField<?, Boolean> delete(String argDeleteLabel, String argUndeleteLabel) {
		return delete(argDeleteLabel, argUndeleteLabel, null);
	}
	
	public FormField<?, Boolean> delete(String argDeleteLabel, String argUndeleteLabel, String argValue) {
		requireOpened();
		
		if (isDeletable()) {
			return checkbox("Delete", ObjectUtils.firstNonNull(argValue, HTMLUtility.DEFAULT_TRUE_STRING), false)
				.addCssClass(getConfiguration().getSubformDeleteCssClass())
				.dataAttribute("of-delete-button-string", argDeleteLabel) // TODO escape
				.dataAttribute("of-undelete-button-string", argUndeleteLabel) // TODO escape
				.notRealField();
		} else {
			if (isNew()) {
				return NullField.getInstance();
			} else {
			return checkbox("Delete", ObjectUtils.firstNonNull(argValue, HTMLUtility.DEFAULT_TRUE_STRING), false)
				.addCssClass(getConfiguration().getSubformDeleteCssClass())
				.dataAttribute("of-delete-button-string", argDeleteLabel) // TODO escape
				.dataAttribute("of-undelete-button-string", argUndeleteLabel) // TODO escape
				.notRealField()
				.disable();
			}
		}
	}
	
	@Override
	public boolean isCompletelyDisabled() {
		if (myDisabledCompletely == Trinary.UNKNOWN) {
			return getParent().isCompletelyDisabled();
		} else {
			return myDisabledCompletely.asBooleanPrimitive(false);
		}
	}
	
	@Override
	public void disableCompletely() {
		myDisabledCompletely = Trinary.TRUE;
	}
	
	@Override
	public void undisableCompletely() {
		myDisabledCompletely = Trinary.FALSE;
	}
	
	@Override
	public FormFieldRequirement determineRequirement(String argFieldName) {
		Validate.notBlank(argFieldName);
		
		FormFieldRequirement lclTheoretical = super.determineRequirement(argFieldName);
		
		if (isNew()) {
			// No field in a form for creating a new child is ever required, because the child may not be created at all (even though the parent form is being submitted).  However, it may be "conditionally required" in the sense that if any field in the child is filled out, this field becomes required:
			if (lclTheoretical == FormFieldRequirement.REQUIRED) {
				return FormFieldRequirement.CONDITIONALLY_REQUIRED;
			} else {
				return lclTheoretical;
			}
		} else {
			return lclTheoretical;
		}
	}
	
	@Override
	protected OpalFormConfiguration getConfiguration() {
		return getParent().getConfiguration();
	}
	
	@Override
	protected String fullyQualifyIfNecessary(String argFieldName) {
		Validate.notNull(argFieldName);
		
		if (argFieldName.startsWith(getPrefix())) {
			return argFieldName;
		} else {
			Validate.isTrue(argFieldName.contains(FULLY_QUALIFIED_NAME_SEPARATOR) == false, "Field names should not contain slashes");
			return generateFullyQualifiedName(argFieldName);
		}
	}
}
