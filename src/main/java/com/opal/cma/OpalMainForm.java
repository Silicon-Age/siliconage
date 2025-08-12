package com.opal.cma;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.util.Trinary;
import com.siliconage.web.ControllerServlet;
import com.siliconage.web.form.FormField;
import com.siliconage.web.form.FormFieldRequirement;
import com.siliconage.web.form.HiddenField;
import com.siliconage.web.form.NullField;
import com.siliconage.web.form.PriorInput;

import com.opal.IdentityFactory;
import com.opal.IdentityUserFacing;

public class OpalMainForm<U extends IdentityUserFacing> extends OpalForm<U> {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalMainForm.class.getName());
	
	protected static final String DEFAULT_UNIQUE_STRING_PARAMETER_NAME = "object";
	
	private final HttpSession mySession;
	private final HttpServletRequest myRequest;
	
	private final IdentityFactory<U> myFactory;
	private final U myUserFacing;
	
	private final String myFormAction;
	private String myFormMethod;
	private final String myPassBackCode;
	
	private final String myUniqueStringParameterName;
	
	private boolean myCompletelyDisabled = false;
	
	private boolean myErrorsAcknowledged = false;
	
	protected List<OpalForm<?>> myDescendants = new ArrayList<>();
	
	private final StringBuilder myOutputUponClose = new StringBuilder();
	
	private String mySuccessURI;
	private String myFailureURI;
	private String myCancelURI;
	private String myDeleteURI;
	private String myContinueURI;
	
	private PriorInput myCachedPriorInput;
	
	private OpalFormConfiguration myConfiguration = OpalFormConfiguration.getInstance();
	
	public OpalMainForm(HttpSession argSession, HttpServletRequest argRequest, String argFormAction, IdentityFactory<U> argFactory, String argParameterName) {
		super("");
		
		mySession = Validate.notNull(argSession);
		myRequest = Validate.notNull(argRequest);
		myFormAction = Validate.notNull(argFormAction);
		myFactory = Validate.notNull(argFactory);
		myUniqueStringParameterName = Validate.notNull(argParameterName);
		myUserFacing = extractUserFacing(); // May be null
		myPassBackCode = Validate.notNull(generatePassBackCode());
		
		setFormMethod("post");
		
		setSuccessURI(getRequest().getRequestURI());
		setFailureURI(getRequest().getRequestURI());
		setCancelURI(getRequest().getRequestURI());
		setDeleteURI(null);
	}
	
	public OpalMainForm(HttpSession argSession, HttpServletRequest argRequest, String argFormAction, U argUserFacing, IdentityFactory<U> argFactory, String argParameterName) {
		super("");
		
		mySession = Validate.notNull(argSession);
		myRequest = Validate.notNull(argRequest);
		myFormAction = Validate.notNull(argFormAction);
		myFactory = Validate.notNull(argFactory);
		myUniqueStringParameterName = Validate.notNull(argParameterName);
		if (argUserFacing != null) {
			myUserFacing = argUserFacing;
		} else {
			myUserFacing = extractUserFacing(); // May be null
		}
		myPassBackCode = generatePassBackCode();
		
		setFormMethod("post");
		
		setSuccessURI(getRequest().getRequestURI());
		setFailureURI(getRequest().getRequestURI());
		setCancelURI(getRequest().getRequestURI());
		setDeleteURI(null);
	}
	
	public OpalMainForm(HttpSession argSession, HttpServletRequest argRequest, String argFormAction, IdentityFactory<U> argFactory) {
		this(argSession, argRequest, argFormAction, argFactory, DEFAULT_UNIQUE_STRING_PARAMETER_NAME);
	}
	
	@Deprecated
	protected String generatePassBack() {
		/* Ideally this will create an essentially unique PASSBACK code (for use by the 
		 * PriorInput and ControllerServlet mechanisms that includes both the name of the
		 * page receiving the input and the identity of the object being edited).  It's possible
		 * that editing the URL (by replacing a + with %20, say) would cause the system to break.
		 * That's something to think about later.
		 */
		return getRequest().getRequestURI();
	}
	
	protected U extractUserFacing() {
		String lclUniqueString = StringUtils.trimToNull(getRequest().getParameter(getUniqueStringParameterName()));
		if (lclUniqueString == null) {
			return null;
		} else {
			try {
				U lclUF = getFactory().forUniqueString(lclUniqueString);
				return lclUF;
			} catch (Exception lclE) {
				ourLogger.warn("Could not extract UserFacing object given unique string " + lclUniqueString + " and factory class " + getFactory().getClass().getName() + " due to a " + lclE.getClass().getSimpleName());
				return null;
			}
		}
	}
	
	public String getUniqueStringParameterName() {
		return myUniqueStringParameterName;
	}
	
	@Override
	public HttpSession getSession() {
		return mySession;
	}
	
	@Override
	public HttpServletRequest getRequest() {
		return myRequest;
	}

	@Override
	public IdentityFactory<U> getFactory() {
		return myFactory;
	}
	
	@Override
	public U getUserFacing() {
		return myUserFacing;
	}
	
	protected String getFormAction() {
		return myFormAction;
	}
	
	protected String getPassBackCode() {
		return myPassBackCode;
	}
	
//	protected void setFormAction(String argFormAction) {
//		myFormAction = Validate.notNull(argFormAction);
//	}
	
	protected String getFormMethod() {
		return myFormMethod;
	}
	
	protected void setFormMethod(String argFormMethod) {
		Validate.notNull(argFormMethod);
		if (argFormMethod.equalsIgnoreCase("get")) {
			myFormMethod = argFormMethod;
		} else if (argFormMethod.equalsIgnoreCase("post")) {
			myFormMethod = argFormMethod;
		} else {
			throw new IllegalArgumentException(("argFormMethod must be either \"get\" or \"post\"."));
		}
		return;
	}
	
	@Override
	protected OpalFormConfiguration getConfiguration() {
		return myConfiguration;
	}
	
	public void setConfiguration(OpalFormConfiguration argConfig) {
		myConfiguration = Validate.notNull(argConfig);
	}
	
	@Override
	protected String openInternal() {
		return getConfiguration().beforeFormTagOpen() +
			"<form id=\"of-" + getPassBackCode() + "\" data-opal-form-passback=\"" + getPassBackCode() + "\" accept-charset=\"utf-8\" action=\"" + getFormAction() + "\" method=\"" + getFormMethod() + "\">" + // THINK: Do we need to escape these?
			new HiddenField<>("PASSBACK", getPassBackCode()) + // Not using the hidden() method because that does stuff with the prefix
			hidden("SuccessURI", getSuccessURI()) +
			hidden("FailureURI", getFailureURI()) +
			hidden("CancelURI", getCancelURI()) +
			hidden("DeleteURI", getDeleteURI()) +
			hidden("UniqueStringParameterName", getUniqueStringParameterName()) +
			hidden("SubmissionConflictMessage", getConfiguration().getSubmissionConflictMessage()) +
			getConfiguration().afterOpen();
	}
	
	@Override
	protected String beforeOpen() {
		return "";
	}
	
	@Override
	protected String afterOpen() {
		outputUponClose(generateIdentifyingFields());
		
		return "";
	}
	
	
	@Override
	protected void recordDescendant(OpalForm<?> argOF) {
		myDescendants.add(argOF);
	}
	
	protected List<OpalForm<?>> getDescendants() {
		return myDescendants;
	}
	
	protected boolean hasAnyNewChildSubform() {
		return getDescendants().stream().anyMatch(OpalForm::isNew);
	}
	
	public String generatePassBackCode() {
		if (getRequest().getQueryString() == null) {
			return getRequest().getRequestURI();
		} else {
			return getRequest().getRequestURI() + "?" + getRequest().getQueryString();
		}
	}
	
	public String getSuccessURI() {
		return mySuccessURI;
	}
	
	public void setSuccessURI(String argSuccessURI) {
		mySuccessURI = argSuccessURI;
	}
	
	public String getFailureURI() {
		return myFailureURI;
	}
	
	public void setFailureURI(String argFailureURI) {
		myFailureURI = argFailureURI;
	}
	
	public String getCancelURI() {
		return myCancelURI;
	}
	
	public void setCancelURI(String argCancelURI) {
		myCancelURI = argCancelURI;
	}
	
	public String getDeleteURI() {
		return myDeleteURI;
	}
	
	public void setDeleteURI(String argDeleteURI) {
		myDeleteURI = argDeleteURI;
	}
	
	public String getContinueURI() {
		return myContinueURI;
	}
	
	public void setContinueURI(String argContinueURI) {
		myContinueURI = argContinueURI;
	}
	
	protected String outputScript() {
		List<String> lclScripts = new ArrayList<>(2);
		
		if (hasAnyNewChildSubform()) {
			lclScripts.add(
				"	var lclSubformNewFields = document.querySelectorAll('[data-opal-form-type=\"" + OpalSubform.class.getSimpleName() + "\"][data-opal-form-isnew=\"true\"]');\n" +
				"	for (var lclI = 0; lclI < lclSubformNewFields.length; ++lclI) {\n" +
				"		lclSubformNewFields[lclI].addEventListener('change', function() {newSubformChanged(this)});\n" +
				"	}\n" +
				// "	\n" +
				"	function newSubformChanged(argField) {\n" +
				"		var lclFormPrefix = argField.dataset.opalFormPrefix;\n" +
				"		var lclFormFields = document.querySelectorAll('[data-opal-form-prefix=\"' + lclFormPrefix + '\"]');\n" +
				// "		\n" +
				"		var lclRequireConditionallyRequiredInThisForm = false;\n" +
				"		for (var lclI = 0; lclI < lclFormFields.length; ++lclI) {\n" +
				"			if (lclFormFields[lclI].value) {\n" +
				"				lclRequireConditionallyRequiredInThisForm = true;\n" +
				"			}\n" +
				"		}\n" +
				// "		\n" +
				"		var lclConditionallyRequiredInForm = document.querySelectorAll('[data-opal-form-prefix=\"' + lclFormPrefix + '\"][data-requirement=\"" + FormFieldRequirement.CONDITIONALLY_REQUIRED + "\"');\n" +
				"		for (var lclI = 0; lclI < lclConditionallyRequiredInForm.length; ++lclI) {\n" +
				"			if (lclRequireConditionallyRequiredInThisForm) {\n" +
				"				lclConditionallyRequiredInForm[lclI].setAttribute('required', 'required');\n" +
				"			} else {\n" +
				"				lclConditionallyRequiredInForm[lclI].removeAttribute('required');\n" +
				"			}\n" +
				"		}\n" +
				"	}\n"
			);
		}
		
		// OpalForms load with submit (and submit-esque) buttons disabled, because the hidden fields defining things like the unique string and Factory are last, and we don't want the form submitted before those are loaded.  In the scripts that run after loading is complete, we enable them.
		lclScripts.add(
			"var lclFieldsToEnableOnCompleteLoad = document.querySelectorAll('.of-enable-on-load');\n" +
			"for (var lclI = 0; lclI < lclFieldsToEnableOnCompleteLoad.length; ++lclI) {\n" +
			"	lclFieldsToEnableOnCompleteLoad[lclI].disabled = false;\n" +
			"}\n"
		);
		
		if (lclScripts.isEmpty()) {
			return "";
		} else {
			return "<script>\n" + StringUtils.join(lclScripts, '\n') + "</script>";
		}
	}
	
	@Override
	public String close() {
		requireOpened();
		
		if (errorsHaveBeenAcknowledged() == false) {
			ourLogger.warn("Errors not acknowledged on form " + getRequest().getRequestURI());
		}
		
		for (OpalForm<?> lclDesc : getDescendants()) {
			if (lclDesc.isClosed() == false) {
				if (lclDesc.isOpened()) {
					ourLogger.warn("Descendant form " + lclDesc + " for " + lclDesc.getUserFacing() + " on " + getRequest().getRequestURI() + " (" + lclDesc.getFactory().getUserFacingInterface() + ") has not been closed");
				} else {
					ourLogger.warn("Descendant form " + lclDesc + " for " + lclDesc.getUserFacing() + " on " + getRequest().getRequestURI() + " (" + lclDesc.getFactory().getUserFacingInterface() + ") has not been opened (or closed)");
				}
			}
		}
		
		setClosed();
		
		// Clear the passed-back info about the previous submission
		Map<String, Object> lclPassBack = ControllerServlet.getPassBack(getSession(), getPassBackCode());
		if (lclPassBack.containsKey(OpalFormController.ACTION_KEY)) {
			lclPassBack.put(OpalFormController.ACTION_KEY, null);
		}
		if (lclPassBack.containsKey(OpalFormController.SUCCESS_KEY)) {
			lclPassBack.put(OpalFormController.SUCCESS_KEY, Trinary.UNKNOWN);
		}
		
		return getConfiguration().beforeClose() +
			hidden("LoadTime", String.valueOf(getLoadTime())) +
			generateHiddenIndicatorsOfEnabledDisplayedFields() +
			generateSecurityFields() +
			getOutputUponClose() +
			"\n</form>" +
			getConfiguration().afterCloseBeforeScript() +
			outputScript() +
			getConfiguration().afterCloseScript();
	}
	
	@Override
	protected void outputUponClose(String argS) {
		if (StringUtils.isNotBlank(argS)) {
			myOutputUponClose.append(argS);
		}
	}
	
	protected String getOutputUponClose() {
		return myOutputUponClose.toString();
	}
	
	@Override
	public String getPrefix() {
		return getLocalPrefix();
	}

	@Override
	public PriorInput getPriorInput() {
		if (myCachedPriorInput == null) {
			myCachedPriorInput = ControllerServlet.getPriorInput(getSession(), getPassBackCode());
		}
		return myCachedPriorInput;
	}
	
	@Override
	public FormField<?, String> delete(String argLabel) {
		requireOpened();
		
		if (StringUtils.isBlank(getDeleteURI())) {
			ourLogger.warn("No delete URI specified on form " + getRequest().getRequestURI());
		}
		
		if (isDeletable()) {
			return button("DeleteButton", ObjectUtils.firstNonNull(argLabel, getConfiguration().getDefaultDeleteButtonLabel()))
				.attribute("formnovalidate", "formnovalidate")
				.disable().addCssClass("of-enable-on-load")
				.addCssClass(getConfiguration().getDefaultDeleteButtonCssClass());
		} else {
			if (isNew()) {
				return NullField.getInstance();
			} else {
				return button("DeleteButton", ObjectUtils.firstNonNull(argLabel, getConfiguration().getDefaultDeleteButtonLabel()))
					.addCssClass(getConfiguration().getDefaultDeleteButtonCssClass())
					.disable();
			}
		}
	}

	public FormField<?, String> delete(String argLabel, String argDeleteURI) {
		requireOpened();
		
		if (isDeletable()) {
			return delete(argLabel).hiddenParameter("DeleteURI", argDeleteURI);
		} else {
			return NullField.getInstance();
		}
	}
	
	public FormField<?, String> done(String argContinueURI) {
		return done(null, argContinueURI);
	}
	
	public FormField<?, String> done(String argLabel, String argContinueURI) {
		requireOpened();
		
		if (isCompletelyDisabled()) {
			return NullField.getInstance();
		} else {
			return button("ContinueButton", ObjectUtils.firstNonNull(argLabel, getConfiguration().getDefaultContinueButtonLabel())).hiddenParameter(generateFullyQualifiedName("ContinueURI"), argContinueURI).disable().addCssClass("of-enable-on-load").addCssClass(getConfiguration().getDefaultContinueButtonCssClass());
		}
	}
	
	public OpalFormAction determineAttemptedAction() {
		Map<String, Object> lclPassBack = ControllerServlet.getPassBack(getSession(), getPassBackCode());
		
		Object lclActionObj = lclPassBack.get(OpalFormController.ACTION_KEY);
		
		if (lclActionObj == null) {
			return null;
		} else if (lclActionObj instanceof OpalFormAction) {
			return (OpalFormAction) lclActionObj;
		} else {
			ourLogger.warn("Passed-back action is not an OpalFormAction but a " + lclActionObj.getClass().getName() + ": " + lclActionObj.toString());
			return null;
		}
	}
	
	public Trinary wasSuccessful() {
		Map<String, Object> lclPassBack = ControllerServlet.getPassBack(getSession(), getPassBackCode());
		
		Object lclSuccessObj = lclPassBack.get(OpalFormController.SUCCESS_KEY);
		
		if (lclSuccessObj == null) {
			return Trinary.UNKNOWN;
		} else if (lclSuccessObj instanceof Boolean) {
			return Trinary.valueOf(((Boolean) lclSuccessObj).booleanValue());
		} else if (lclSuccessObj instanceof Trinary) {
			return (Trinary) lclSuccessObj;
		} else {
			ourLogger.warn("Passed-back success indicator is not a boolean but a " + lclSuccessObj.getClass().getName() + ": " + lclSuccessObj.toString());
			return Trinary.UNKNOWN;
		}
	}
	
	public boolean hasErrors() {
		acknowledgeErrors();
		return getErrors().isEmpty() == false;
	}
	
	public List<String> getErrors() {
		return ControllerServlet.getErrors(getSession(), getPassBackCode());
	}
	
	public String displayResultOrErrors() {
		if (hasErrors()) {
			return displayErrors();
		} else {
			return displayResult();
		}
	}
	
	public String displayResult() {
		return getConfiguration().displayResult(determineAttemptedAction(), wasSuccessful());
	}
	
	public String displayErrors() {
		requireOpened(); // THINK: Really?
		
		acknowledgeErrors();
		
		return getConfiguration().formatErrors(getErrors());
	}
	
	public boolean hasMessages() {
		return getMessages().isEmpty() == false;
	}
	
	public List<String> getMessages() {
		return ControllerServlet.getMessages(getSession(), getPassBackCode());
	}
	
	public String displayMessages() {
		requireOpened(); // THINK: Really?
		
		return getConfiguration().formatMessages(getMessages());
	}

	@Override
	public boolean isCompletelyDisabled() {
		return myCompletelyDisabled;
	}
	
	@Override
	public void disableCompletely() {
		myCompletelyDisabled = true;
	}
	
	@Override
	public void undisableCompletely() {
		myCompletelyDisabled = false;
	}
	
	public boolean errorsHaveBeenAcknowledged() {
		return myErrorsAcknowledged;
	}
	
	public void acknowledgeErrors() {
		myErrorsAcknowledged = true;
	}
	
	public FormField<?, String> cancel() {
		return cancel(null);
	}
	
	public FormField<?, String> cancel(String argLabel) {
		if (isCompletelyDisabled()) {
			return NullField.getInstance();
		} else {
			return button("CancelButton", ObjectUtils.firstNonNull(argLabel, getConfiguration().getDefaultCancelButtonLabel())).attribute("formnovalidate", "formnovalidate").disable().addCssClass("of-enable-on-load").addCssClass(getConfiguration().getDefaultCancelButtonCssClass());
		}
	}
	
	public FormField<?, String> submit() {
		return submit(null);
	}
	
	public FormField<?, String> submit(String argLabel) {
		if (isCompletelyDisabled()) {
			return NullField.getInstance();
		} else {
			if (isNew()) {
				return button("SubmitButton", ObjectUtils.firstNonNull(argLabel, getConfiguration().getDefaultInsertButtonLabel())).disable().addCssClass("of-enable-on-load").addCssClass(getConfiguration().getDefaultInsertButtonCssClass());
			} else {
				return button("SubmitButton", ObjectUtils.firstNonNull(argLabel, getConfiguration().getDefaultUpdateButtonLabel())).disable().addCssClass("of-enable-on-load").addCssClass(getConfiguration().getDefaultUpdateButtonCssClass());
			}
		}
	}
	
	@Override
	protected String fullyQualifyIfNecessary(String argFieldName) {
		Validate.notNull(argFieldName);
		
		if (argFieldName.startsWith(OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR)) {
			return argFieldName;
		} else {
			Validate.isTrue(argFieldName.contains(FULLY_QUALIFIED_NAME_SEPARATOR) == false, "Field names should not contain slashes");
			return generateFullyQualifiedName(argFieldName);
		}
	}
	
	private String generateSecurityFields() {
		List<String> lclEnabledDisplayedFieldsEntireForm = new ArrayList<>(getEnabledDisplayedFields());
		for (OpalForm<?> lclForm : getDescendants()) {
			lclEnabledDisplayedFieldsEntireForm.addAll(lclForm.getEnabledDisplayedFields());
		}
		
		return OpalFormSecurityUtil.generateSecurityFields(lclEnabledDisplayedFieldsEntireForm);
	}
}
