package com.opal.cma;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Predicate;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.util.Trinary;
import com.siliconage.web.form.BooleanDropdownField;
import com.siliconage.web.form.ButtonField;
import com.siliconage.web.form.DateTimeField;
import com.siliconage.web.form.DateField;
import com.siliconage.web.form.TimeField;
import com.siliconage.web.form.EmailField;
import com.siliconage.web.form.FormField;
import com.siliconage.web.form.FormFieldRequirement;
import com.siliconage.web.form.FormValueProvider;
import com.siliconage.web.form.HiddenField;
import com.siliconage.web.form.LabelField;
import com.siliconage.web.form.MoneyField;
import com.siliconage.web.form.NameCodeExtractor;
import com.siliconage.web.form.NullField;
import com.siliconage.web.form.NumberField;
import com.siliconage.web.form.PasswordField;
import com.siliconage.web.form.PhoneField;
import com.siliconage.web.form.PriorInput;
import com.siliconage.web.form.RadioField;
import com.siliconage.web.form.SearchField;
import com.siliconage.web.form.TextBasedHTMLInputField;
import com.siliconage.web.form.TextField;
import com.siliconage.web.form.TextAreaField;
import com.siliconage.web.form.TrinaryField;
import com.siliconage.web.form.UrlField;
import com.siliconage.web.form.ZipField;

import com.opal.AbstractFactoryMap;
import com.opal.IdentityFactory;
import com.opal.FactoryCreator;
import com.opal.FactoryPolymorphicCreator;
import com.opal.IdentityUserFacing;
import com.opal.FieldUtility;
import com.opal.OpalUtility;

public abstract class OpalForm<U extends IdentityUserFacing> implements FormValueProvider {
	/* package */ static final String FULLY_QUALIFIED_NAME_SEPARATOR = "/";
	
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalForm.class.getName());
	
	protected static final String FACTORY_CLASS_NAME_FORM_PARAMETER_SUFFIX = "_FACTORY_CLASS_NAME";
	
	public static final String FACTORY_MAP_JNDI_KEY = "FactoryMap";
	
	private final String myLocalPrefix;
	private boolean myOpened = false;
	private boolean myClosed = false;
	
	private Class<? extends Validator<U>> myValidatorClass;
	private Class<? extends OpalFormUpdater<U>> myUpdaterClass;
	
	private final Set<String> myDisplayedFields = new HashSet<>();
	
	protected OpalForm(String argLocalPrefix) {
		super();
		
		myLocalPrefix = Validate.notNull(argLocalPrefix);
	}
	
	public static <U extends IdentityUserFacing> OpalMainForm<U> create(HttpSession argSession, HttpServletRequest argRequest, String argFormAction, IdentityFactory<U> argFactory, String argParameterName) {
		Validate.notNull(argSession);
		Validate.notNull(argRequest);
		Validate.notNull(argFactory);
		
		return new OpalMainForm<>(argSession, argRequest, argFormAction, argFactory, argParameterName);
	}
	
	public static <U extends IdentityUserFacing> OpalMainForm<U> create(HttpSession argSession, HttpServletRequest argRequest, String argFormAction, U argUserFacing, IdentityFactory<U> argFactory) {
		return create(
				argSession,
				argRequest,
				argFormAction,
				argUserFacing,
				argFactory,
				OpalMainForm.DEFAULT_UNIQUE_STRING_PARAMETER_NAME
				);
	}
	
	public static <U extends IdentityUserFacing> OpalMainForm<U> create(HttpSession argSession, HttpServletRequest argRequest, String argFormAction, U argUserFacing, IdentityFactory<U> argFactory, String argParameterName) {
		Validate.notNull(argSession);
		Validate.notNull(argRequest);
		Validate.notNull(argFactory);
		
		return new OpalMainForm<>(argSession, argRequest, argFormAction, argUserFacing, argFactory, argParameterName);
	}
	
	public static <U extends IdentityUserFacing> OpalMainForm<U> create(HttpSession argSession, HttpServletRequest argRequest, String argFormAction, IdentityFactory<U> argFactory) {
		return create(argSession, argRequest, argFormAction, argFactory, OpalMainForm.DEFAULT_UNIQUE_STRING_PARAMETER_NAME);
	}
	
	public abstract HttpSession getSession();
	
	public abstract HttpServletRequest getRequest();
	
	public abstract IdentityFactory<U> getFactory();
	
	public abstract U getUserFacing();
	
	public boolean isNew() {
		return getUserFacing() == null;
	}
	
	public boolean alreadyExists() {
		return getUserFacing() != null;
	}
	
	protected Object getSavedValue(String argFieldName) {
		return FieldUtility.getValue(getUserFacing(), argFieldName);
	}
	
	protected String getSavedValueAsString(String argName) {
		Validate.notNull(argName);
		
		Object lclO = getSavedValue(argName);
		return lclO == null ? null : String.valueOf(lclO);
	}
	
	protected String getPriorInputValue(String argName) {
		Validate.notNull(argName);
		
		return getPriorInput().get(generateFullyQualifiedName(argName));
	}
	
	protected String getEnteredValue(String argFieldName) {
		return getPriorInputValue(argFieldName);
	}
	
	protected String getUserFacingUniqueString() {
		IdentityUserFacing lclUF = getUserFacing();
		return lclUF == null ? null : lclUF.getUniqueString();
	}
	
	public String open() {
		setOpened();
		
		return beforeOpen() + openInternal() + afterOpen();
	}
	
	protected abstract String beforeOpen();
	
	protected abstract String openInternal();
	
	protected abstract String afterOpen();
	
	protected String generateIdentifyingFields() {
		return generateUserFacingFactoryField().toString() +
				generateUserFacingUniqueStringField() +
				generateValidatorField() +
				generateUpdaterField();
	}
	
	protected FormField<?, ?> generateUserFacingFactoryField() {
		return hidden("UserFacingFactoryClassName", getFactory().getClass().getName());
	}
	
	protected FormField<?, ?> generateUserFacingUniqueStringField() {
		return hidden("UserFacingUniqueString", isNew() ? "" : getUserFacing().getUniqueString());
	}
	
	protected FormField<?, ?> generateValidatorField() {
		return getValidatorClass() == null ? NullField.getInstance() : hidden("ValidatorClassName", getValidatorClass().getName());
	}
	
	protected FormField<?, ?> generateUpdaterField() {
		return getUpdaterClass() == null ? NullField.getInstance() : hidden("UpdaterClassName", getUpdaterClass().getName());
	}
	
	protected void setOpened() {
		myOpened = true;
	}
	
	protected boolean isOpened() {
		return myOpened;
	}
	
	protected void setClosed() {
		if (isClosed()) {
			ourLogger.warn("Form " + this + " on " + getRequest().getRequestURI() + " for " + getFactory().getUserFacingInterface() + " is being closed more than once");
		}
		
		myClosed = true;
	}
	
	protected boolean isClosed() {
		return myClosed;
	}
	
	protected void requireOpened() {
		if (!isOpened()) {
			throw new IllegalStateException("Form has not been opened");
		}
	}
	
	public abstract String close();
	
	protected abstract void outputUponClose(String argS);
	
	protected String getLocalPrefix() {
		return myLocalPrefix;
	}
	
	public abstract String getPrefix();
	
	protected abstract PriorInput getPriorInput();
	
	/* package */ static String generateFullyQualifiedName(String argPrefix, String argName) {
		Validate.notNull(argName);
		
		String lclPrefix = StringUtils.trimToEmpty(argPrefix);
		if (argName.startsWith(lclPrefix)) {
			lclPrefix = "";
		}
		
		if (argName.startsWith(FULLY_QUALIFIED_NAME_SEPARATOR)) {
			return lclPrefix + argName;
		} else {
			return lclPrefix + FULLY_QUALIFIED_NAME_SEPARATOR + argName;
		}
	}
	
	/* package */ String generateFullyQualifiedName(String argName) {
		return generateFullyQualifiedName(getPrefix(), argName);
	}
	
	/* Methods for constructing input fields */
	
	public HiddenField<?> hidden(String argName, String argValue) {
		requireOpened();
		
		return new HiddenField<>(generateFullyQualifiedName(argName), argValue);
	}
	
	protected abstract void recordDescendant(OpalForm<?> argOF);
	
	public void noteDisplayedField(String argName, boolean argMultipleAllowed) {
		Validate.notNull(argName);
		
		if (argName.startsWith("Target:") == false) {
			requireOpened();
		}
		
		if (isNew()) {
			if (getFactory() instanceof FactoryCreator == false && getFactory() instanceof FactoryPolymorphicCreator == false) {
				throw new IllegalStateException(getFactory() + " does not allow creation.");
			}
		} else {
			Class<U> lclInterfaceClass = getFactory().getUserFacingInterface();
			if (FieldUtility.isUpdatable(lclInterfaceClass, argName) == Trinary.FALSE) {
				throw new IllegalStateException("The '" + argName + "' field cannot be used as a form field because it is not updatable.");
			}
		}
		
		if (myDisplayedFields.contains(argName) && argMultipleAllowed == false) {
			throw new IllegalStateException("The '" + argName + "' field has already been displayed; displaying it again will lead to unexpected behavior.");
		}
		
		myDisplayedFields.add(fullyQualifyIfNecessary(argName));
	}
	
	// CAUTION: These fields might not be enabled!
	protected Collection<String> getDisplayedFields() {
		return myDisplayedFields;
	}
	
	protected Collection<String> getEnabledDisplayedFields() {
		return getDisplayedFields().stream()
			.filter(this::isEnabled)
			.toList();
	}
	
	protected String generateHiddenIndicatorsOfEnabledDisplayedFields() {
		requireOpened();
		
		Collection<String> lclEnabledDisplayedFields = getEnabledDisplayedFields();
		
		StringBuilder lclSB = new StringBuilder(64 * lclEnabledDisplayedFields.size());
		
		for (String lclFieldName : lclEnabledDisplayedFields) {
			lclSB.append(display(lclFieldName));
		}
		
		return lclSB.toString();
	}
	
	protected FormField<?, String> display(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		return hidden("Displayed", argName);
	}
	
	public String id(String argName) {
		return generateFullyQualifiedName(argName);
	}
	
	public LabelField<?> label(String argFieldName, String argLabelContents) {
		
		requireOpened();
		
		return new LabelField<>(id(argFieldName), argLabelContents);
	}
	
	public TextField<?> text(String argName, int argSize) {
		Validate.notNull(argName);
		Validate.isTrue(argSize > 0);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new TextField<>(generateFullyQualifiedName(argName), getSavedValueAsString(argName), this, argSize), argName);
	}
	
	public EmailField<?> email(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new EmailField<>(generateFullyQualifiedName(argName), getSavedValueAsString(argName), this), argName);
	}
	
	public NumberField<?> number(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new NumberField<>(generateFullyQualifiedName(argName), (Number) getSavedValue(argName), this), argName);
	}
	
	public PhoneField<?> phone(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new PhoneField<>(generateFullyQualifiedName(argName), getSavedValueAsString(argName), this), argName);
	}
	
	public ZipField<?> zip(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new ZipField<>(generateFullyQualifiedName(argName), getSavedValueAsString(argName), this), argName);
	}
	
	public SearchField<?> search(String argName, int argSize) {
		Validate.notNull(argName);
		Validate.isTrue(argSize > 0);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new SearchField<>(generateFullyQualifiedName(argName), getSavedValueAsString(argName), this, argSize), argName);
	}
	
	public UrlField<?> url(String argName, int argSize) {
		Validate.notNull(argName);
		Validate.isTrue(argSize > 0);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new UrlField<>(generateFullyQualifiedName(argName), getSavedValueAsString(argName), this, argSize), argName);
	}
	
	public PasswordField<?> password(String argName, int argSize) {
		Validate.notNull(argName);
		Validate.isTrue(argSize > 0);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new PasswordField<>(generateFullyQualifiedName(argName), getSavedValueAsString(argName), this, argSize), argName);
	}
	
	public static String generateVerifyName(String argS) {
		Validate.notNull(argS);
		
		return argS.equals("") ? "Verify" : argS + "_Verify";
	}
	
	public PasswordField<?> passwordVerify(String argName, int argSize) {
		Validate.notNull(argName);
		Validate.isTrue(argSize > 0);
		
		requireOpened();
		
		String lclVerifyName = generateVerifyName(argName);
		
		noteDisplayedField(lclVerifyName, false);
		
		return prepareField(new PasswordField<>(generateFullyQualifiedName(lclVerifyName), getSavedValueAsString(argName), this, argSize).notRealField(), argName);
	}
	
	public <T extends IdentityUserFacing> TextFieldWithHandler<?> special(String argName, int argSize, Class<? extends SpecialHandler<T>> argHandlerClass) {
		Validate.notNull(argName);
		Validate.isTrue(argSize > 0);
		Validate.notNull(argHandlerClass);
		
		requireOpened();
		
		try {
			SpecialHandler<T> lclHandler = argHandlerClass.getDeclaredConstructor().newInstance();
			
			noteDisplayedField(argName, false);
			
			return prepareField(new TextFieldWithHandler<>(this, argName, argSize, lclHandler), argName);
		} catch (InstantiationException|IllegalAccessException|NoSuchMethodException|InvocationTargetException lclE) {
			throw new IllegalStateException("Could not instantiate SpecialHandler class \"" + argHandlerClass.getName() + "\".", lclE);
		}
	}
	
	public OpalCheckboxField<?> checkbox(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		Boolean lclChecked = (Boolean) getSavedValue(argName);
		if (lclChecked == null) {
			lclChecked = Boolean.FALSE;
		}

		return new OpalCheckboxField<>(generateFullyQualifiedName(argName), lclChecked.booleanValue(), this);
	}
	
	protected OpalCheckboxField<?> checkbox(String argName, String argValue) {
		Validate.notNull(argName);
		
		requireOpened();

		Boolean lclChecked = (Boolean) getSavedValue(argName);
		if (lclChecked == null) {
			lclChecked = Boolean.FALSE;
		}

		return checkbox(argName, argValue, lclChecked.booleanValue());
	}
	
	protected OpalCheckboxField<?> checkbox(String argName, String argValue, boolean argChecked) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return new OpalCheckboxField<>(generateFullyQualifiedName(argName), argValue, argChecked, this);
	}
	
	public String field(String argName) {
		return field(argName, "-");
	}
	
	public String field(String argName, String argNullString) {
		Validate.notNull(argName);
		
		requireOpened();
		
		return isNew() ? argNullString : ObjectUtils.firstNonNull(getEnteredValue(argName), getSavedValueAsString(argName), argNullString);
	}
	
	public TextAreaField<?> textarea(String argName, int argCols, int argRows) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new TextAreaField<>(generateFullyQualifiedName(argName), getSavedValueAsString(argName), this, argCols, argRows), argName);
		
		/* TextAreaField<?> lclF = new TextAreaField<>(generateFullyQualifiedName(argName), getSavedValueAsString(argName), this, argCols, argRows);
		
		OptionalLong lclMaxLengthOpt = FieldUtility.getMaximumLength(getFactory().getUserFacingInterface(), argName);
		if (lclMaxLengthOpt.isPresent()) {
			long lclMaxLength = lclMaxLengthOpt.getAsLong();
			lclF.maxlength(lclMaxLength);
		}
		
		return lclF; */
	}
	
	public DateField<?> date(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		DateField<?> lclField = null;
		try {
			lclField = new DateField<>(generateFullyQualifiedName(argName), (LocalDate) getSavedValue(argName), this);
		} catch (ClassCastException lclE) {
			LocalDateTime lclLDT = (LocalDateTime) getSavedValue(argName);
			lclField = new DateField<>(generateFullyQualifiedName(argName), lclLDT == null ? (LocalDate) null : lclLDT.toLocalDate(), this);
		}
		return prepareField(lclField, argName);
	}
	
	public DateTimeField<?> datetime(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new DateTimeField<>(generateFullyQualifiedName(argName), (LocalDateTime) getSavedValue(argName), this), argName);
	}
	
	public TimeField<?> time(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		TimeField<?> lclField = null;
		try {
			lclField = new TimeField<>(generateFullyQualifiedName(argName), (LocalTime) getSavedValue(argName), this);
		} catch (ClassCastException lclE) {
			LocalDateTime lclLDT = (LocalDateTime) getSavedValue(argName);
			lclField = new TimeField<>(generateFullyQualifiedName(argName), lclLDT == null ? (LocalTime) null : lclLDT.toLocalTime(), this);
		}
		return prepareField(lclField, argName);
	}
	
	public MoneyField<?> money(String argName) {
		return money(argName, MoneyField.DEFAULT_SIZE);
	}
	
	public MoneyField<?> money(String argName, int argSize) {
		Validate.notNull(argName);
		
		requireOpened();
		
		noteDisplayedField(argName, false);
		
		return prepareField(new MoneyField<>(generateFullyQualifiedName(argName), (Number) getSavedValue(argName), this).size(argSize), argName);
	}
	
	public String link(String argPage) {
		return link(argPage, "object", "Edit");
	}
	
	public String link(String argPage, String argParameter) {
		return link(argPage, argParameter, "Edit");
	}
	
	public String link(String argPage, String argParameter, String argLinkText) {
		Validate.notNull(argPage);
		Validate.notNull(argParameter);
		
		requireOpened();
		
		if (this.isNew()) {
			return "&nbsp;";
		} else {
			String lclURL;
			if (argPage.contains("?")) {
				lclURL = argPage + '&' + argParameter + '=' + this.getUserFacing().getUniqueString();
			} else {
				lclURL = argPage + '?' + argParameter + '=' + this.getUserFacing().getUniqueString();
			}
			// THINK: What about percent-escaping argPage?  Is it okay to assume that has already been done?
			
			return "<a href=\"" + lclURL + "\">" + argLinkText + "</a>";
		}
	}
	
	public BooleanDropdownField<?> booleanDropdown(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		String lclValueString = ObjectUtils.firstNonNull(getPriorInputValue(argName), getSavedValueAsString(argName));
		Boolean lclCurrentValueObj = OpalUtility.convertTo(Boolean.class, lclValueString);
		
		noteDisplayedField(argName, false);
		
		return prepareField(new BooleanDropdownField<>(generateFullyQualifiedName(argName), lclCurrentValueObj, this), argName);
	}
	
	public TrinaryField<?> trinary(String argName) {
		Validate.notNull(argName);
		
		requireOpened();
		
		Trinary lclCurrentValue = Trinary.fromStringCaseInsensitive(ObjectUtils.firstNonNull(getPriorInputValue(argName), getSavedValueAsString(argName)));
		
		noteDisplayedField(argName, false);
		
		return prepareField(new TrinaryField<>(generateFullyQualifiedName(argName), lclCurrentValue), argName);
	}
	
	public <T extends IdentityUserFacing> OpalDropdownField<?, T> dropdown(String argName) {
		return dropdown(argName, (IdentityFactory<T>) null, (Comparator<? super T>) null, (NameCodeExtractor<? super T>) null);
	}
	
	public <T extends IdentityUserFacing> OpalDropdownField<?, T> dropdown(String argName, Comparator<? super T> argComparator) {
		return dropdown(argName, (IdentityFactory<T>) null, argComparator, (NameCodeExtractor<T>) null);
	}
	
	public <T extends IdentityUserFacing> OpalDropdownField<?, T> dropdown(String argName, IdentityFactory<T> argFactory, Comparator<? super T> argComparator) {
		return dropdown(argName, argFactory, argComparator, (NameCodeExtractor<T>) null);
	}
	
	public <T extends IdentityUserFacing> OpalDropdownField<?, T> dropdown(String argName, Comparator<T> argComparator, NameCodeExtractor<? super T> argNCE) {
		return dropdown(argName, (IdentityFactory<T>) null, argComparator, argNCE);
	}
	
	public <T extends IdentityUserFacing> OpalDropdownField<?, T> dropdown(String argName, IdentityFactory<T> argFactory, Comparator<? super T> argComparator, NameCodeExtractor<? super T> argNCE) {
		Validate.notNull(argName);
		
		requireOpened();
		
		IdentityFactory<T> lclIF;
		if (argFactory == null) {
			IdentityFactory<U> lclFormFactory = getFactory();
			
			Class<U> lclInterfaceClass = lclFormFactory.getUserFacingInterface();
			
			String lclAccessorName = "get" + argName;
			try {
				Method lclAccessor = lclInterfaceClass.getMethod(lclAccessorName);
				@SuppressWarnings("unchecked")
				Class<T> lclReturnTypeOfAccessor = (Class<T>) lclAccessor.getReturnType();
				
				// CHECK: How slow is this?
				try {
					InitialContext lclC = new InitialContext();
					AbstractFactoryMap lclAFM = (AbstractFactoryMap) lclC.lookup("FactoryMap");
					if (lclAFM == null) {
						throw new IllegalStateException("Could not find instance of AbstractOpalFactoryFactory in the InitialContext under the key OpalFactoryFactory");
					}
					@SuppressWarnings("unchecked")
					IdentityFactory<T> lclTempIF = (IdentityFactory<T>) lclAFM.get(lclReturnTypeOfAccessor); // lclTempIF only exists to suppress warnings
					lclIF = lclTempIF;
					
				} catch (NamingException lclE) {
					throw new IllegalStateException("Could not get an InitialContext or look up the OpalFactoryFactory.");
				}
			} catch (NoSuchMethodException lclE) {
				throw new IllegalStateException("Could not find accessor \"" + lclAccessorName + "\" on " + lclInterfaceClass.getName() + " to determine the proper return type.", lclE);
			}
		} else {
			lclIF = argFactory;
		}
		
		noteDisplayedField(argName, false);
		
		OpalDropdownField<?, T> lclField = prepareField(new OpalDropdownField<>(this, argName, lclIF, argNCE), argName);
		
		if (argComparator != null) {
			lclField.comparator(argComparator);
		}
		
		return lclField;
	}
	
	public <T extends IdentityUserFacing> RadioField<?> radio(String argName, T argValue, NameCodeExtractor<T> argNCE, boolean argChecked) {
		Validate.notNull(argName);
		Validate.notNull(argValue);
		Validate.notNull(argNCE);
		
		requireOpened();
		
		noteDisplayedField(argName, true);
		
		return prepareField(new RadioField<>(generateFullyQualifiedName(argName), argNCE.extractCode(argValue), argChecked, this), argName);
	}
	
	public ButtonField<?> button(String argName, String argValue) {
		requireOpened();
		
		noteDisplayedField(argName, true);
		
		return new ButtonField<>(generateFullyQualifiedName(argName), argValue);
	}
	
	public FormField<?, String> customAction(String argName, String argLabel, boolean argExistingOnly) {
		requireOpened();
		
		if (getUserFacing() != null || argExistingOnly == false) {
			return button(
				StringUtils.isBlank(argName) ? "ActionButton" : generateFullyQualifiedName(argName),
				StringUtils.isBlank(argLabel) ? "Action" : argLabel
			);
		} else {
			return NullField.getInstance();
		}
	}
	
	public boolean handlesDeletionFor(String argChildName) {
		Validate.notBlank(argChildName);
		
		Class<? extends OpalFormUpdater<?>> lclUpdaterClass = getUpdaterClass();
		if (lclUpdaterClass == null) {
			return false;
		} else {
			return OpalFormUpdater.handlesDeletionFor(lclUpdaterClass, argChildName);
		}
	}
	
	public boolean isDeletable() {
		if (isNew()) {
			return false;
		} else if (isDisabled("Delete")) {
			return false;
		} else {
			// Make sure all children are being handled
			for (var lclChildDefinition : FieldUtility.getChildNamesAndTypes(getFactory().getUserFacingInterface())) {
				String lclChildName = lclChildDefinition.getLeft();
				Set<?> lclChildren = FieldUtility.getChildren(getUserFacing(), lclChildName);
				if (lclChildren.isEmpty() == false && handlesDeletionFor(lclChildName) == false) {
					return false;
					// TODO: If lclChildName can be removed as a child, but still remain around--that is, Child's foreign key to Parent is nullable--then deleting the parent (i.e., getUserFacing()) isn't a problem, right?
				}
			}
			
			return true;
		}
	}
	
	public FormField<?, ?> delete() {
		requireOpened();
		
		return delete(null);
	}
	
	public abstract FormField<?, ?> delete(String argLabel);
	
	/* Methods for getting subforms */
	
	public <T extends IdentityUserFacing> List<OpalForm<T>> children(String argName, IdentityFactory<T> argFactory) {
		return children(argName, argFactory, 0, null, null);
	}
	
	public <T extends IdentityUserFacing> List<OpalForm<T>> children(String argName, IdentityFactory<T> argFactory, Comparator<? super T> argComparator) {
		return children(argName, argFactory, 0, null, argComparator);
	}
	
	public <T extends IdentityUserFacing> List<OpalForm<T>> children(String argName, IdentityFactory<T> argFactory, int argNew) {
		return children(argName, argFactory, argNew, null, null);
	}
	
	public <T extends IdentityUserFacing> List<OpalForm<T>> children(String argName, IdentityFactory<T> argFactory, int argNew, Comparator<? super T> argComparator) {
		return children(argName, argFactory, argNew, null, argComparator);
	}

	public <T extends IdentityUserFacing> List<OpalForm<T>> children(String argName, IdentityFactory<T> argFactory, int argNew, Predicate<? super T> argFilter, Comparator<? super T> argComparator) {
		return children(argName, "get" + argName + "Set", argFactory, argNew, argFilter, argComparator);
	}

	public <T extends IdentityUserFacing> List<OpalForm<T>> children(String argName, String argAccessorName, IdentityFactory<T> argFactory, int argNew, Predicate<? super T> argFilter, Comparator<? super T> argComparator) {
		Validate.notNull(argName);
		Validate.notNull(argAccessorName);
		
		// argNew should be positive to put argNew blank records after all the existing child records.
		// If argNew is negative, we will put abs(argNew) blank records *before* the existing child records.
		
		requireOpened();
		
		int lclNew = argNew < 0 ? -1*argNew : argNew;
		
		List<OpalForm<T>> lclOFs = new ArrayList<>();
		int lclIndex = 0;
		
		/* Add blank records to allow new data to be inserted */
		if (argNew < 0) {
			for (int lclNewCount = 0; lclNewCount < lclNew; ++lclNewCount) {
				OpalForm<T> lclOF = new OpalSubform<>(this, argName + FULLY_QUALIFIED_NAME_SEPARATOR + lclIndex, null, argFactory);
				recordDescendant(lclOF);
				lclOFs.add(lclOF);
				++lclIndex;
			}
		}
		
		if (alreadyExists()) {
			Set<T> lclChildrenSet = FieldUtility.<U, T>getChildren(getUserFacing(), argName, argAccessorName);
			List<T> lclChildren = new ArrayList<>(lclChildrenSet);
			
			if (argFilter != null) {
				lclChildren.removeIf(argFilter.negate()); // Irritating that we have to negate this.
			}
			
			if (argComparator != null) {
				lclChildren.sort(argComparator);
			} else if (Comparable.class.isAssignableFrom(argFactory.getUserFacingInterface())) { // In other words, is the class of children Comparable?
				lclChildren.sort(null); // Use its natural order
			}
			
			for (T lclChild : lclChildren) {
				OpalForm<T> lclOF = new OpalSubform<>(this, argName + FULLY_QUALIFIED_NAME_SEPARATOR + lclIndex, lclChild, argFactory);
				recordDescendant(lclOF);
				lclOFs.add(lclOF);
				++lclIndex;
			}
		}
		
		/* Add blank records to allow new data to be inserted */
		if (argNew > 0) {
			for (int lclNewCount = 0; lclNewCount < argNew; ++lclNewCount) {
				OpalForm<T> lclOF = new OpalSubform<>(this, argName + FULLY_QUALIFIED_NAME_SEPARATOR + lclIndex, null, argFactory);
				recordDescendant(lclOF);
				lclOFs.add(lclOF);
				++lclIndex;
			}
		}
		
		return lclOFs;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends IdentityUserFacing> T target(String argName, IdentityFactory<T> argFactory) {
		Validate.notNull(argName);
		Validate.notNull(argFactory);
		
		U lclUF = getUserFacing();
		if (lclUF == null) {
			return null;
		}
		
		try {
			Method lclM = lclUF.getClass().getMethod("get" + argName);
			
			/* This could easily throw a ClassCastException if the wrong factory is passed by the user. */
			return (T) lclM.invoke(lclUF);
		} catch (NoSuchMethodException lclE) {
			throw new IllegalArgumentException("Could not find target named \"" + argName + "\"", lclE);
		} catch (InvocationTargetException lclE) {
			throw new IllegalArgumentException("Could not get target named \"" + argName + "\"", lclE);
		} catch (IllegalAccessException lclE) {
			throw new IllegalArgumentException("Could not access target named \"" + argName + "\"", lclE);
		}
	}
	
	public Class<? extends Validator<U>> getValidatorClass() {
		return myValidatorClass;
	}
	
	public void setValidatorClass(Class<? extends Validator<U>> argValidatorClass) {
		myValidatorClass = argValidatorClass;
	}
	
	public <T extends IdentityUserFacing> OpalForm<T> targetForm(String argName, IdentityFactory<T> argFactory) {
		noteDisplayedField("Target:" + argName, true); // THINK: should this be false?
		
		OpalForm<T> lclOF = new OpalSubform<>(this, argName, target(argName, argFactory), argFactory);
		recordDescendant(lclOF);
		
		return lclOF;
	}
	
	public Class<? extends OpalFormUpdater<U>> getUpdaterClass() {
		return myUpdaterClass;
	}
	
	public void setUpdaterClass(Class<? extends OpalFormUpdater<U>> argUpdaterClass) {
		myUpdaterClass = argUpdaterClass;
	}
	
	@Override
	public void disable(String argFieldName) {
		Validate.notNull(argFieldName);
		getPriorInput().disable(generateFullyQualifiedName(argFieldName));
	}
	
	@Override
	public void enable(String argFieldName) {
		Validate.notNull(argFieldName);
		getPriorInput().enable(generateFullyQualifiedName(argFieldName));
	}
	
	public abstract boolean isCompletelyDisabled();
	
	public abstract void disableCompletely();
	
	public abstract void undisableCompletely();
	
	protected <F extends FormField<?, ?>> F prepareField(F argField, String argName) {
		Validate.notNull(argField);
		Validate.notBlank(argName);
		
		if (argField instanceof TextBasedHTMLInputField && ((argField instanceof TextFieldWithHandler) == false)) {
			incorporateMaximumLength((TextBasedHTMLInputField<?, ?>) argField, argName);
		}
		
		argField.dataAttribute("opal-form-prefix", getPrefix())
				.dataAttribute("opal-form-isnew", String.valueOf(this.isNew()))
				.dataAttribute("opal-form-type", this.getClass().getSimpleName());
		
		return argField;
	}
	
	protected <F extends TextBasedHTMLInputField<?, ?>> F incorporateMaximumLength(F argField, String argFieldName) {
		Validate.notBlank(argFieldName);
		Validate.notNull(argField);
		
		if (argField instanceof NumberField) {
			// Do nothing.  NumberFields are not allowed to have a maxlength attribute.
			return argField;
		} else {
			OptionalLong lclMaxLengthOpt = FieldUtility.getMaximumLength(getFactory().getUserFacingInterface(), argFieldName);
			
			if (lclMaxLengthOpt.isPresent()) {
				long lclMaxLength = lclMaxLengthOpt.getAsLong();
				argField.maxlength(lclMaxLength);
				return argField;
			} else {
				return argField;
			}
		}
	}
	
	protected boolean existsDefault(String argFieldName) {
		Validate.notBlank(argFieldName);
		
		Optional<?> lclClassDefault = FieldUtility.getDefault(getFactory().getUserFacingInterface(), argFieldName);
		if (lclClassDefault.isPresent()) {
			return true;
		} else {
			Class<? extends OpalFormUpdater<?>> lclUpdaterClass = getUpdaterClass();
			if (lclUpdaterClass == null) {
				return false;
			} else {
				return OpalFormUpdater.specifiesDefault(lclUpdaterClass, argFieldName);
			}
		}
	}
	
	// FormValueProvider methods
	@Override
	public long getLoadTime() {
		return getPriorInput().getLoadTime();
	}
	
	@Override
	public Collection<String> getAll(String argKey) {
		return getPriorInput().getAll(fullyQualifyIfNecessary(argKey));
	}
	
	@Override
	public boolean hasValueFor(String argKey) {
		return getPriorInput().hasValueFor(fullyQualifyIfNecessary(argKey));
	}
	
	@Override
	public void setDisabled(String argKey, boolean argValue) {
		getPriorInput().setDisabled(fullyQualifyIfNecessary(argKey), argValue);
	}
	
	@Override
	public final boolean isDisabled(String argFieldName) {
		Validate.notNull(argFieldName);
		
		return isCompletelyDisabled() || getPriorInput().isDisabled(fullyQualifyIfNecessary(argFieldName));
	}
	
	protected abstract String fullyQualifyIfNecessary(String argFieldName);
	
	@Override
	public boolean isIncorrect(String argKey) {
		return getPriorInput().isIncorrect(fullyQualifyIfNecessary(argKey));
	}
	
	@Override
	public FormFieldRequirement determineRequirement(String argFieldName) {
		Validate.notBlank(argFieldName);
		
		// This is kind of a dirty trick.
		String lclUnqualifiedFieldName = getUnqualifiedFieldName(argFieldName);
		
		Trinary lclNullability = FieldUtility.isNullable(getFactory().getUserFacingInterface(), lclUnqualifiedFieldName);
		switch (lclNullability) {
			case TRUE:
				return FormFieldRequirement.NOT_REQUIRED;
			case UNKNOWN:
				// THINK: What should we do here?
				return FormFieldRequirement.NOT_REQUIRED;
			case FALSE:
				return existsDefault(lclUnqualifiedFieldName) ? FormFieldRequirement.NOT_REQUIRED : FormFieldRequirement.REQUIRED;
			default:
				throw new IllegalStateException("Unknown nullability status " + lclNullability);
		}
	}
	
	protected abstract OpalFormConfiguration getConfiguration();
	
	/* package */ static String getUnqualifiedFieldName(String argPossiblyQualifiedFieldName) {
		Validate.notBlank(argPossiblyQualifiedFieldName);
		
		// THINK: Can this be replaced with StringUtils.removeStart(argPossiblyQualifiedFieldName, getPrefix())?  We'd need to make this an instance method, but I don't think that's a problem.
		
		if (argPossiblyQualifiedFieldName.contains(FULLY_QUALIFIED_NAME_SEPARATOR) && !argPossiblyQualifiedFieldName.endsWith(FULLY_QUALIFIED_NAME_SEPARATOR)) {
			return argPossiblyQualifiedFieldName.substring(argPossiblyQualifiedFieldName.lastIndexOf(FULLY_QUALIFIED_NAME_SEPARATOR) + 1);
		} else {
			return argPossiblyQualifiedFieldName;
		}
	}
}
