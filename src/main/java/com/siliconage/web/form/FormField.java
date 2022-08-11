package com.siliconage.web.form;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.Flat3Map;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.util.Fast3Set;
import com.siliconage.util.Trinary;

public abstract class FormField<T extends FormField<?, V>, V> {
	// private static final org.apache.log4j.Logger ourLogger = org.apache.log4j.Logger.getLogger(FormField.class.getName());
	
	private static final String USE_DEFAULT_ID = "%%UseDefaultId%%";
	private static final String USE_INHERITED_CSS = "%%UseInheritedCss%%";
	
	private static final int DEFAULT_LENGTH = 1024;
	
	// TODO: Document whether this is a local name or a fully qualified name (including parent names and slashes)
	private String myName;
	private Collection<V> mySavedValues;
	private FormValueProvider myEnteredValueProvider;
	
	private String myId = USE_DEFAULT_ID;
	private String myScript;
	private String myCssClass = USE_INHERITED_CSS;
	private String myIncorrectCssClass = "incorrect";
	private String myChangedCssClass = "changed";
	private String myDefault;
	private FormFieldRequirement myRequirement;
	private boolean myRealField = true;
	private Trinary myEnabled = Trinary.UNKNOWN;
	
	private Fast3Set<HiddenField<?>> myHiddenParameters = null;
	private Flat3Map<String, String> myAttributes = null;
	
	protected FormField(String argName, Collection<V> argSavedValues, FormValueProvider argEnteredValueProvider) {
		super();
		
		myName = Validate.notNull(argName); // but it may be empty
		mySavedValues = argSavedValues; // which may be null
		myEnteredValueProvider = argEnteredValueProvider; // which may be null
		
		if (isRequirable()) {
			if (argEnteredValueProvider == null) {
				myRequirement = FormFieldRequirement.NOT_REQUIRED; // who knows? best we can do
			} else {
				myRequirement = argEnteredValueProvider.determineRequirement(argName);
			}
		} else {
			myRequirement = FormFieldRequirement.NOT_APPLICABLE;
		}
	}
	
	public static String scrub(String argS) {
		return StringEscapeUtils.escapeHtml4(argS);
	}
	
	public static String unscrub(String argS) {
		return StringEscapeUtils.unescapeHtml4(argS);
	}
	
	protected String outputAttribute(String argName) {
		if (StringUtils.isBlank(argName)) {
			return "";
		} else {
			return " " + argName; // Note initial space 
		}
	}
	
	protected String outputAttribute(String argName, String argValue) {
		if (StringUtils.isBlank(argName)) {
			return "";
		} else if (argValue == null) {
			return "";
		} else {
			return " " + argName + "=\"" + scrub(argValue) + "\""; // Note initial space; CHECK: Does argName need to be scrubbed?
		}
		// THINK: What about empty or all-whitespace values; will HTTP return those?  If not, should we not emit them in the first place?
	}
	
	public String getName() {
		return myName;
	}
	
	public String nameAttribute() {
		return outputAttribute("name", getName());
	}
	
	@SuppressWarnings("unchecked")
	protected <U extends FormField<?, V>> U castThis() {
		return (U) this;
	}

	// This method exists for neat in-place disablement using a local variable
	public final T disableIf(boolean arg) {
		if (arg) {
			disable();
		}
		
		return castThis();
	}
	
	public final T disable() {
		return setEnabled(false);
	}
	
	public final T enable() {
		return setEnabled(true);
	}
	
	public final boolean isEnabled() {
		boolean lclEnabled = getEnteredValueProvider() == null || getEnteredValueProvider().isEnabled(getId());
		
		// ourLogger.debug(getId() + " isEnabled(): myEnabled = " + myEnabled + "; EVP = " + getEnteredValueProvider() + " says " + (getEnteredValueProvider() == null ? null : lclEnabled));
		return myEnabled.asBoolean(lclEnabled);
	}
	
	public final boolean isDisabled() {
		return isEnabled() == false;
	}
	
	protected String disabledAttribute() {
		// ourLogger.debug(getId() + "::isDisabled() -> " + isDisabled());
		return isDisabled() ? outputAttribute("disabled", "disabled") : "";
	}
	
	public final T setEnabled(boolean argE) {
		myEnabled = Trinary.valueOf(argE);
		
		if (getEnteredValueProvider() != null) {
			getEnteredValueProvider().setEnabled(getId(), argE);
		}
		
		return castThis();
	}
	
	public T style(String argStyleAttributeValue) {
		return attribute("style", argStyleAttributeValue);
	}
	
	public String getCssClass() {
		if (myCssClass == null) {
			return null;
		} else if (USE_INHERITED_CSS.equals(myCssClass)) {
			return null;
		} else {
			return myCssClass;
		}
	}
	
	public T addCssClass(String argClass) {
		Validate.notNull(argClass);
		
		setCssClass((ObjectUtils.firstNonNull(getCssClass(), "") + ' ' + argClass).trim());
		return castThis();
	}
	
	public T inheritCss() {
		return addCssClass(USE_INHERITED_CSS);
	}
	
	public void setCssClass(String argClass) {
		myCssClass = argClass;
	}
	
	protected String outputCssClassAttribute() {
		String lclS;
		if (getEnteredValueProvider() != null && getEnteredValueProvider().isIncorrect(getId())) {
			if (!StringUtils.isBlank(getCssClass())) {
				lclS = getCssClass() + ' ' + incorrectCssClass();
			} else {
				lclS = incorrectCssClass(); // might be null
			}
		} else {
		  lclS = getCssClass(); // might be null
		}
		
		if (StringUtils.isBlank(lclS)) {
			return "";
		} else {
			return outputAttribute("class", lclS);
		}
	}
	
	public String getIncorrectCssClass() {
		return myIncorrectCssClass;
	}
	
	protected String incorrectCssClass() {
		return getIncorrectCssClass(); // which may be null
	}
	
	public T incorrectCssClass(String argIncorrectClass) {
		setIncorrectCssClass(argIncorrectClass);
		return castThis();
	}
	
	public void setIncorrectCssClass(String argIncorrectCssClass) {
		myIncorrectCssClass = argIncorrectCssClass;
	}
	
	public String getChangedCssClass() {
		return myChangedCssClass;
	}
	
	protected String changedCssClass() {
		return getChangedCssClass(); // which may be null
	}
	
	public T changedCssClass(String argChangedClass) {
		setChangedCssClass(argChangedClass);
		return castThis();
	}
	
	public void setChangedCssClass(String argChangedCssClass) {
		myChangedCssClass = argChangedCssClass;
	}
	
	public String getScript() {
		return myScript;
	}
	
	/* Manually setting a script will prevent the automatic generation of the onChange script that marks the field
	 * as changed (primarily to allow users to set their own onChange event handler).  I'm not sure how to elegantly
	 * allow both functions to co-exist.  For now, if you want the default behavior, your custom script must (also)
	 * include an onChange handler that executes
	 * 
	 * this.classList.add('changed');
	 * 
	 * (possibly in addition to custom onChange behavior that you want). 
	 */
	public T script(String argScript) {
		setScript(argScript);
		return castThis();
	}
	
	public void setScript(String argScript) {
		myScript = argScript;
	}
	
	public T autofocus() {
		return attribute("autofocus", "autofocus");
	}
	
	public T autofocusIf(boolean argValue) {
		if (argValue) {
			return attribute("autofocus", "autofocus");
		} else {
			return castThis();
		}
	}
	
	public T formId(String argFormId) {
		Validate.notEmpty(argFormId);
		
		return attribute("form", argFormId);
	}
	
	public T readonly() {
		return attribute("readonly", "readonly");
	}
	
	public T readonlyIf(boolean argValue) {
		if (argValue) {
			return readonly();
		} else {
			return castThis();
		}
	}
	
	public T required() {
		return setRequired(true);
	}
	
	public T optional() {
		return setRequired(false);
	}
	
	public FormFieldRequirement getRequirement() {
		return myRequirement;
	}
	
	public Trinary isRequired() {
		return getRequirement().asTrinary();
	}
	
	public T setRequired(boolean argV) {
		return setRequirement(FormFieldRequirement.of(argV));
	}
	
	public T setRequirement(FormFieldRequirement argR) {
		myRequirement = Validate.notNull(argR);
		return castThis();
	}
	
	public T tabindex(int argIndex) {
		return attribute("tabindex", String.valueOf(argIndex));
	}
	
	public T tooltip(String argTooltip) {
		return attribute("title", argTooltip);
	}
	
	public T noSpellCheck() {
		return attribute("spellcheck", "false");
	}
	
	protected boolean mayThereBeOtherFieldsWithTheSameNameAsThisOne() {
		return false;
	}
	
	protected String generateDefaultId() {
		return getName() + (mayThereBeOtherFieldsWithTheSameNameAsThisOne() ? "_" + StringUtils.join(determineStringsToDisplay(), '_') : "");
	}
	
	public String getCustomId() {
		return myId;
	}
	
	public String getId() {
		return usingDefaultId() ? generateDefaultId() : getCustomId();
	}
	
	protected String idAttribute() {
		return outputAttribute("id", getId());
	}
	
	public T id(String argId) {
		setId(argId);
		return castThis();
	}
	
	public T defaultId() {
		return id(USE_DEFAULT_ID);
	}
	
	public boolean usingDefaultId() {
		return USE_DEFAULT_ID.equals(myId);
	}
	
	public void setId(String argId) {
		myId = argId;
	}
	
	public String hidden() {
		return "hidden";
	}
	
	public String getDefault() {
		return myDefault;
	}
	
	public T defaultValue(String argDefault) {
		setDefault(argDefault);
		return castThis();
	}
	
	public void setDefault(String argDefault) {
		myDefault = argDefault;
	}
	
	public boolean isRealField() {
		return myRealField;
	}
	
	public T setRealField(boolean argRealField) {
		myRealField = argRealField;
		return castThis();
	}
	
	public T realField() {
		return setRealField(true);
	}
	
	public T notRealField() {
		return setRealField(false);
	}
	
	protected FormValueProvider getEnteredValueProvider() {
		return myEnteredValueProvider;
	}
	
	protected Collection<V> getSavedValues() {
		return mySavedValues;
	}
	
	protected Collection<String> getSavedValuesAsString() {
		return getSavedValues().stream()
			.map(argV -> argV == null ? "" : argV.toString())
			.collect(Collectors.toList());
	}
	
	// A null return value indicates that no entry has been given (which is distinct from an empty string having been entered/left in place)
	protected Collection<String> getEnteredValues() {
		return getEnteredValueProvider() == null ? null : getEnteredValueProvider().getAll(getName());
	}
	
	protected boolean hasEnteredValue() {
		if (getEnteredValueProvider() == null) {
			return false;
		} else {
			return getEnteredValueProvider().hasValueFor(getId());
		}
	}
	
	protected Collection<String> determineStringsToDisplay() {
		// ourLogger.debug("determineStringToDisplay for " + getId());
		if (getEnteredValueProvider() == null) {
			return getSavedValuesAsString();
		} else {
			if (hasEnteredValue()) {
				// ourLogger.debug("hasEnteredValue of " + (getEnteredValue() == null ? "null" : "\"" + getEnteredValue() + "\""));
				return getEnteredValues();
			} else {
				// ourLogger.debug("hasSavedValue of " + (getSavedValueAsString() == null ? "null" : "\"" + getSavedValueAsString() + "\""));
				return getSavedValuesAsString();
			}
		}
	}
	
	/* Methods for constructing input fields */
	
	// FIXME: This should be able to be changed by the user
	protected String onChangeScript() {
		return " onChange=\"this.classList.add('" + getChangedCssClass() + "');\""; // Note leading space!
	}
	
	protected Set<HiddenField<?>> getHiddenParametersInternal() {
		return myHiddenParameters; // which may be null
	}
	
	protected Set<HiddenField<?>> getHiddenParameters() {
		return myHiddenParameters == null ? Set.of() : myHiddenParameters;
	}
	
	public T hiddenParameter(String argName, String argValue) {
		if (myHiddenParameters == null) {
			myHiddenParameters = new Fast3Set<>();
		}
		HiddenField<?> lclHF = new HiddenField<>(argName, argValue);
		myHiddenParameters.add(lclHF);
		
		return castThis();
	}
	
	protected String hiddenParametersToString() {
		StringBuilder lclSB = new StringBuilder(128 * myHiddenParameters.size());
		appendHiddenParameters(lclSB);
		return lclSB.toString();
	}
	
	protected void appendHiddenParameters(StringBuilder argSB) {
		Validate.notNull(argSB);
		if (myHiddenParameters == null || myHiddenParameters.size() == 0) {
			return;
		}
		for (HiddenField<?> lclHF : myHiddenParameters) {
			argSB.append(lclHF.toString());
		}
		return;
	}
	
	protected Map<String, String> getAttributes() {
		return myAttributes == null ? Map.of() : myAttributes;
	}
	
	public boolean hasAttribute(String argName) {
		Validate.notNull(argName);
		
		return getAttributes().keySet().stream().anyMatch(argName::equalsIgnoreCase);
	}
	
	public T attribute(String argName, String argValue) {
		if (myAttributes == null) {
			myAttributes = new Flat3Map<>();
		}
		myAttributes.put(argName, argValue);
		
		return castThis();
	}
	
	public T removeAttribute(String argName) {
		if (myAttributes != null) {
			myAttributes.remove(argName);
		}
		
		return castThis();
	}
	
	public T dataAttribute(String argDashedName, String argValue) {
		Validate.notBlank(argDashedName);
		// argValue can be null
		
		return attribute("data-" + argDashedName, ObjectUtils.firstNonNull(argValue, ""));
	}
	
	protected String attributesToString() {
		if (getAttributes() == null || getAttributes().isEmpty()) {
			return "";
		}
		
		StringBuilder lclSB = new StringBuilder(64 * getAttributes().size());
		for (Map.Entry<String, String> lclE : getAttributes().entrySet()) {
			Validate.notNull(lclE, "Attribute map entry is null");
			lclSB.append(outputAttribute(lclE.getKey(), lclE.getValue()));
		}
		
		return lclSB.toString();
	}
	
	protected int getDefaultLength() {
		return DEFAULT_LENGTH;
	}
	
	protected abstract void appendFormField(StringBuilder argSB);
	
	protected void addFieldTypeSpecificHiddenParameters() {
		return;
	}
	
	protected boolean isRequirable() {
		return true;
	}
	
	protected void addRequirement() {
		if (isRequired() == Trinary.TRUE) {
			attribute("required", "required");
		}
		dataAttribute("requirement", getRequirement().toString());
	}
	
	@Override
	public String toString() {
		StringBuilder lclSB = new StringBuilder(getDefaultLength());
		
		addRequirement();
		addFieldTypeSpecificHiddenParameters();
		appendHiddenParameters(lclSB);
		appendFormField(lclSB);
		
		return lclSB.toString();
	}
}
