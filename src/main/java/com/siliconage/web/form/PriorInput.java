package com.siliconage.web.form;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import com.siliconage.util.Fast3Set;
import com.siliconage.util.Trinary;
import com.siliconage.web.HTMLUtility;

public class PriorInput implements FormValueProvider {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(PriorInput.class.getName());
	
	private final long myLoadTime;
	
	/* myRequestMap might be null under non-error conditions.  If it is null, it means that there is no collection
	 * of previous inputs that should be use to display the form; this is a situation that needs to be differentiated
	 * from, for instance, all previous inputs being blank (and therefore missing from the RequestMap).
	 * 
	 * Generally speaking, the first time a form is displayed, myRequestMap will be null for the PriorInput
	 * requested by the JSP page.  If an error occurs, and the form is redisplayed, there will be a NON-null
	 * PriorInput.  If the form submission is successful, however, the PriorInput requested by the JSP page
	 * will also have a null RequestMap, so the values displayed will be those taken directly from the model
	 * object (like a UserFacing from the Opal project).  This won't matter often, but it is a nice bit of housecleaning
	 * and also provides the opportunity for the form submission process to provide non-user-entered values
	 * for some of the model's fields and to have them immediately displayed.  For instance, when creating
	 * an Opal, one of the classes involved in committing the changes might replace a user-entered blank value
	 * with a database default.  If the RequestMap weren't cleared, the form redisplay would continue to show
	 * a blank, but, since it is cleared, the form will display the proper default value (even though this wasn't
	 * entered by the user).  
	 */
	private final Map<String, Collection<String>> myRequestMap;

	private final Set<String> myDisabledFields; // Never null
	private final Set<String> myIncorrectFields; // Never null
	private final Map<String, FormFieldRequirement> myFieldRequirements; // Never null
//	private final Set<Pair<String, String>> myCheckedNameValuePairs; // Never null
	
//	private final static String ON_CHANGE_JAVASCRIPT = " onChange='this.style.backgroundColor=\"#FFFFCC\";'"; // Note leading space!
	
	public PriorInput(Map<String, Collection<String>> argRequestMap, long argLoadTime, Set<String> argDisabledFields, Set<String> argIncorrectFields, Map<String, FormFieldRequirement> argFieldRequirements, @SuppressWarnings("unused") Set<Pair<String, String>> argCheckedNameValuePairs) {
		super();
		
		if (argRequestMap == null) {
			myRequestMap = null;
		} else if (argRequestMap.isEmpty()) {
			myRequestMap = Map.of();
		} else {
			myRequestMap = new HashMap<>(argRequestMap); // defensive copy
		}
		
		myLoadTime = argLoadTime;
		
		if (argDisabledFields != null && !argDisabledFields.isEmpty()) {
			myDisabledFields = new HashSet<>(argDisabledFields); // defensive copy
		} else {
			myDisabledFields = new Fast3Set<>(); // not an empty or immutable set because we want the set to be modifiable in case a field is subsequently disabled
		}
		
		if (argIncorrectFields != null && !argIncorrectFields.isEmpty()) {
			myIncorrectFields = new HashSet<>(argIncorrectFields); // defensive copy
		} else {
			myIncorrectFields = new Fast3Set<>(); // not an empty or immutable set because we want the set to be modifiable in case a field is marked as incorrect
		}
		
		if (argFieldRequirements == null || argFieldRequirements.isEmpty()) {
			myFieldRequirements = Map.of();
		} else {
			myFieldRequirements = new HashMap<>(argFieldRequirements); // defensive copy
		}
		
//		if (argCheckedNameValuePairs != null && !argCheckedNameValuePairs.isEmpty()) {
//			myCheckedNameValuePairs = new HashSet<>(argCheckedNameValuePairs); // defensive copy
//		} else {
//			myCheckedNameValuePairs = new Fast3Set<>(); // not an empty or immutable set because we want the set to be modifiable in case a field is subsequently marked as checked
//		}
	}
	
	public PriorInput(Map<String, Collection<String>> argRequestMap) {
		this(argRequestMap, System.currentTimeMillis(), null, null, null, null); // argRequestMap might be null
	}
	
	public PriorInput() {
		this(null, System.currentTimeMillis(), null, null, null, null); // argRequestMap might be null
	}
	
	protected boolean hasRequestMap() {
		return myRequestMap != null;
	}
	
	protected Map<String, Collection<String>> getRequestMap() { // Might return null under normal circumstances
		return myRequestMap;
	}
	
	public boolean containsRequestData() {
		return hasRequestMap() && getRequestMap().isEmpty() == false;
	}
	
	protected Set<String> getDisabledFields() {
		return myDisabledFields;
	}
	
	protected Set<String> getIncorrectFields() {
		return myIncorrectFields;
	}
	
	protected Map<String, FormFieldRequirement> getFieldRequirements() {
		return myFieldRequirements;
	}
	
//	protected Set<Pair<String, String>> getCheckedNameValuePairs() {
//		return myCheckedNameValuePairs;
//	}
	
	protected void markField(String argFieldName) {
		Validate.notNull(argFieldName);
		
		getIncorrectFields().add(argFieldName);
	}
	
	@Override
	public long getLoadTime() {
		return myLoadTime;
	}
	
	@Override
	public boolean isIncorrect(String argKey) {
		Validate.notNull(argKey);
		
		Set<String> lclIF = getIncorrectFields();
		return lclIF != null && lclIF.contains(argKey);
	}
	
	@Override
	public FormFieldRequirement determineRequirement(String argKey) {
		Validate.notNull(argKey);
		
		return getFieldRequirements().getOrDefault(argKey, FormFieldRequirement.NOT_REQUIRED);
	}
	
//	@Override
//	public boolean isChecked(String argName, String argValue) {
//		return isChecked(argName, (Object) argValue);
//	}
	
//	public boolean isChecked(String argName, Object argValue) {
//		return getCheckedNameValuePairs().contains(Pair.of(argName, argValue == null ? "" : String.valueOf(argValue))); // relies on Pair.equals comparing argKey and argValue with equals()
//	}
	
	@Override
	public boolean hasValueFor(String argKey) {
		return hasRequestMap() && getRequestMap().containsKey(argKey);
	}
	
	@Override
	public Collection<String> getAll(String argKey) {
		if (hasRequestMap() == true) {
			return getRequestMap().get(argKey);
		} else {
			return null; // THINK: Should this be an empty Collection?
		}
	}
	
	public String get(String argKey, String argDefaultValue) {
		Validate.notNull(argKey);
		// argDefaultValue may be null
		
		if (hasRequestMap() == false) {
			return argDefaultValue;
		} else {
			Collection<String> lclValues = getRequestMap().get(argKey);
			if (lclValues == null || lclValues.isEmpty()) {
				return null;
			} else if (lclValues.size() == 1) {
				return lclValues.iterator().next();
			} else {
				// This indicates either a misconfiguration with the form or someone messing around with the request manually.
				ourLogger.warn("Multiple values for {}: {}", argKey, lclValues.toString());
				return lclValues.iterator().next();
			}
		}
	}
	
	@Override
	public String get(String argKey) {
		return get(argKey, null);
	}
	
	@Override
	public boolean isDisabled(String argKey) {
//		System.out.println("PriorInput " + this + " isDisabled() answer for " + argKey + " is " + getDisabledFields().contains(argKey));
		return getDisabledFields().contains(argKey);
	}
	
	@Override
	public void setDisabled(String argKey, boolean argValue) {
		if (argValue) {
			getDisabledFields().add(argKey);
		} else {
			getDisabledFields().remove(argKey);
		}
	}
	
	public RadioField<?> radio(String argName, String argValue, boolean argChecked) {
		return new RadioField<>(argName, argValue == null ? "" : argValue, argChecked, this);
	}
	
	public RadioField<?> radio(String argName, String argValue) {
		return radio(argName, argValue, false);
	}
	
	public CheckboxField<?> checkbox(String argName, String argValue, boolean argChecked) {
		return new CheckboxField<>(argName, argValue == null ? "" : argValue, argChecked, this);
	}
	
	public CheckboxField<?> checkbox(String argName, String argValue) {
		return checkbox(argName, argValue, false);
	}
	
	public CheckboxField<?> checkbox(String argName, boolean argChecked) {
		return checkbox(argName, HTMLUtility.DEFAULT_TRUE_STRING, argChecked);
	}
	
	public CheckboxField<?> checkbox(String argName) {
		return checkbox(argName, HTMLUtility.DEFAULT_TRUE_STRING, false);
	}
	
	public HiddenField<?> hidden(String argName, Object argValue) {
		return new HiddenField<>(argName, argValue == null ? "" : String.valueOf(argValue));
	}
	
	public TextField<?> text(String argName, int argSize, Object argValue) {
		return new TextField<>(argName, argValue == null ? "" : String.valueOf(argValue), this, argSize);
	}
	
	public TextField<?> text(String argName, int argSize) {
		return text(argName, argSize, null);
	}
	
	public DateField<?> date(String argName, LocalDate argValue) {
		return new DateField<>(argName, argValue, this);
	}
	
	public DateField<?> date(String argName) {
		return date(argName, null);
	}
	
	public DateTimeField<?> datetime(String argName, LocalDateTime argValue) {
		return new DateTimeField<>(argName, argValue, this);
	}
	
	public DateTimeField<?> datetime(String argName) {
		return datetime(argName, null);
	}
	
	public EmailField<?> email(String argName, Object argValue) {
		return new EmailField<>(argName, argValue == null ? "" : String.valueOf(argValue), this);
	}
	
	public EmailField<?> email(String argName) {
		return email(argName, null);
	}
	
	public MoneyField<?> money(String argName, Number argValue) {
		return new MoneyField<>(argName, argValue, this);
	}
	
	public NumberField<?> money(String argName) {
		return money(argName, null);
	}
	
	public NumberField<?> number(String argName, Number argValue) {
		return new NumberField<>(argName, argValue, this);
	}
	
	public NumberField<?> number(String argName, int argValue) {
		return new NumberField<>(argName, Integer.valueOf(argValue), this);
	}
	
	public NumberField<?> number(String argName) {
		return number(argName, null);
	}
	
	public PasswordField<?> password(String argName, int argSize, Object argValue) {
		return new PasswordField<>(argName, argValue == null ? "" : String.valueOf(argValue), this, argSize);
	}
	
	public PasswordField<?> password(String argName, int argSize) {
		return password(argName, argSize, null);
	}
	
	public PhoneField<?> phone(String argName, Object argValue) {
		return new PhoneField<>(argName, argValue == null ? "" : String.valueOf(argValue), this);
	}
	
	public PhoneField<?> phone(String argName) {
		return phone(argName, null);
	}
	
	public ZipField<?> zip(String argName, Object argValue) {
		return new ZipField<>(argName, argValue == null ? "" : String.valueOf(argValue), this);
	}
	
	public ZipField<?> zip(String argName) {
		return zip(argName, null);
	}
	
	public SearchField<?> search(String argName, int argSize, Object argValue) {
		return new SearchField<>(argName, argValue == null ? "" : String.valueOf(argValue), this, argSize);
	}
	
	public SearchField<?> search(String argName, int argSize) {
		return search(argName, argSize, null);
	}
	
	public UrlField<?> url(String argName, int argSize, Object argValue) {
		return new UrlField<>(argName, argValue == null ? "" : String.valueOf(argValue), this, argSize);
	}
	
	public UrlField<?> url(String argName, int argSize) {
		return url(argName, argSize, null);
	}
	
	public TextAreaField<?> textarea(String argName, int argCols, int argRows, Object argValue) {
		return new TextAreaField<>(argName, argValue == null ? "" : String.valueOf(argValue), argCols, argRows);
	}
	
	public TextAreaField<?> textarea(String argName, int argCols, int argRows) {
		return textarea(argName, argCols, argRows, null);
	}
	
	public BooleanDropdownField<?> booleanDropdown(String argName) {
		Validate.notNull(argName);
		
		Object lclCurrentValueObj = get(argName);
		Boolean lclCurrentValue = null;
		
		if (lclCurrentValueObj instanceof Number) {
			lclCurrentValue = (((Number) lclCurrentValueObj).intValue() != 0 ? Boolean.TRUE: Boolean.FALSE);
		} if (lclCurrentValueObj instanceof Trinary) {
			lclCurrentValue = ((Trinary) lclCurrentValueObj).asBoolean(null);
		} else {
			String lclS = String.valueOf(lclCurrentValueObj);
			if (Trinary.UNKNOWN.name().equals(lclS)) {
				lclCurrentValue = null;
			} else {
				lclCurrentValue = Boolean.valueOf(lclS);
			}
		}
		
		return new BooleanDropdownField<>(argName, (lclCurrentValue != null) ? lclCurrentValue : Boolean.FALSE, this);
	}
	
	public <C> AssembledDropdownField<?, C> dropdown(String argName, Collection<C> argChoices, C argCurrent, NameCodeExtractor<C> argNCE) {
		return dropdown(argName, argChoices, argCurrent == null ? Set.of() : Set.of(argCurrent), argNCE);
	}
	
	public <C> AssembledDropdownField<?, C> dropdown(String argName, Collection<C> argChoices, Collection<C> argCurrent, NameCodeExtractor<C> argNCE) {
		Validate.notNull(argName);
		// argCurrent may be null
		Validate.notEmpty(argChoices);
		
		AssembledDropdownField<?, C> lclD = new AssembledDropdownField<>(argName, argCurrent, this).choices(argChoices);
		
		if (argNCE != null) {
			lclD.namer(argNCE);
		}
		
		return lclD;
	}
	
	public <C> AssembledDropdownField<?, C> dropdown(String argName, Collection<C> argChoices, NameCodeExtractor<C> argNCE) {
		return dropdown(argName, argChoices, (C) null, argNCE);
	}

	public StandardDropdownField<?, Integer> dropdown(String argName, String[] argNames, int argCurrentIndex) {
		Validate.notEmpty(argNames);
		Validate.validIndex(argNames, argCurrentIndex);
		
		List<DropdownOption<?>> lclOptions = new ArrayList<>(argNames.length);
		for (int lclI = 0; lclI < argNames.length; ++lclI) {
			DropdownOption<?> lclO = new DropdownOption<>(argNames[lclI], String.valueOf(lclI));
			
			if (lclI == argCurrentIndex) {
				lclO.selected();
			}
			
			lclOptions.add(lclO);
		}
		
		return new StandardDropdownField<>(argName, Integer.valueOf(argCurrentIndex), this).entries(lclOptions);
	}

	public StandardDropdownField<?, String> dropdown(String argName, String[] argNames, String[] argCodes, String argCurrentCode) {
		Validate.notNull(argNames);
		Validate.notNull(argCodes);
		Validate.isTrue(argNames.length == argCodes.length, "Name and code arrays are of different sizes");
		
		List<DropdownOption<?>> lclOptions = new ArrayList<>(argNames.length);
		
		for (int lclI = 0; lclI < argNames.length; ++lclI) {
			DropdownOption<?> lclO = new DropdownOption<>(argNames[lclI], argCodes[lclI]);
			
			if (argCurrentCode != null && argCodes[lclI].equals(argCurrentCode)) {
				lclO.selected();
			}
			
			lclOptions.add(lclO);
		}
		
		return new StandardDropdownField<>(argName, argCurrentCode, this).entries(lclOptions);
	}
}