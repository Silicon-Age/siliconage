package com.opal.cma;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import com.siliconage.util.Fast3Set;
import com.siliconage.util.Trinary;
import com.siliconage.web.HTMLUtility;

import com.opal.ArgumentTooLongException;
import com.opal.FactoryCreator;
import com.opal.FactoryPolymorphicCreator;
import com.opal.FieldUtility;
import com.opal.IdentityFactory;
import com.opal.IdentityUserFacing;
import com.opal.OpalUtility;
import com.opal.TransactionContext;
import com.opal.UserFacing;
import com.opal.annotation.RequiresActiveTransaction;

public class OpalFormUpdater<U extends IdentityUserFacing> {
	private final HttpServletRequest myRequest;
	private final String myPrefix;
	private final IdentityFactory<U> myFactory;
	private U myUserFacing;
	private final String myUniqueStringParameterName;
	private final Validator<U> myValidator;
	private String myUsername;
	private String myPrefixedReferenceName;
	private UserFacing myParent;
	private final Collection<OpalFormUpdater<?>> myChildUpdaters = new ArrayList<>();
	
	private final List<String> myErrors = new ArrayList<>();
	private final Set<String> myIncorrectFields = new Fast3Set<>();
	private final Set<Pair<String, String>> myCheckedNameValuePairs = new Fast3Set<>();

	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalFormUpdater.class.getName());
	
	/* Used for the root (i.e., argPrefix == "") updater with a Validator */
	public OpalFormUpdater(HttpServletRequest argRequest, String argPrefix, String argParameterName, Validator<U> argValidator) {
		super();
		
		if (argRequest == null && argPrefix == null && argParameterName == null && argValidator == null) {
			// This should really only be done if you're constructing a fake instance to get access to the Updater-provided defaults.  In fact, this pathway shouldn't exist at all, but it's probably better than going back and modifying all the existing Updaters to have singleton default-provider instances, or something like that.
			myRequest = null;
			myPrefix = null;
			myFactory = null;
			myUniqueStringParameterName = null;
			myValidator = null;
		} else {
			myRequest = Validate.notNull(argRequest, "Null argRequest in OpalFormUpdater constructor (" + getClass().getName() + ")");
			
			myPrefix = Validate.notNull(argPrefix, "Null argPrefix in OpalFormUpdater constructor (" + getClass().getName() + ")");
			
			if ("".equals(myPrefix)) {
				Validate.notNull(argParameterName, "Null argParameterName with empty prefix in OpalFormUpdater constructor (" + getClass().getName() + "); referer " + argRequest.getHeader("referer"));
			}
			myUniqueStringParameterName = argParameterName; // Might be null
			
			myValidator = argValidator != null ? argValidator : NullValidator.getInstance();
			
			myFactory = determineFactory();
			myUserFacing = determineExistingUserFacing(); // Might be null
		}
	}
	
	/* Used for the root (i.e., argPrefix == "") updater without a Validator */
	public OpalFormUpdater(HttpServletRequest argRequest, String argPrefix, String argParameterName) {
		this(argRequest, argPrefix, argParameterName, null);
	}
	
	/* Used for child updaters; this might not be in use anymore */
	public OpalFormUpdater(HttpServletRequest argRequest, String argPrefix, Validator<U> argValidator) {
		this(argRequest, argPrefix, null, argValidator);
	}
	
	protected String getUniqueStringParameterName() {
		return myUniqueStringParameterName;
	}
	
	protected HttpServletRequest getRequest() {
		return myRequest;
	}
	
	protected String getPrefix() {
		return myPrefix;
	}
	
	protected Validator<U> getValidator() {
		return myValidator;
	}
	
	protected IdentityFactory<U> determineFactory() {
		String lclFactoryClassName = getPrefixedParameter("UserFacingFactoryClassName");
		Validate.notNull(lclFactoryClassName, "Could not determine the factory: null UserFacingFactoryClassName parameter");
		
		try {
			Class<?> lclFactoryClass = Class.forName(lclFactoryClassName);
			Method lclM = lclFactoryClass.getDeclaredMethod("getInstance");
			@SuppressWarnings("unchecked")
			IdentityFactory<U> lclFactory = (IdentityFactory<U>) lclM.invoke(null);
			return lclFactory;
		} catch (Exception lclE) {
			throw new IllegalStateException("Could not determine the factory", lclE);
		}
	}
	
	protected IdentityFactory<U> getFactory() {
		return myFactory;
	}
	
	protected U determineExistingUserFacing() {
		// ourLogger.debug("determineExistingUserFacing");
		String lclUniqueString = getPrefixedParameter("UserFacingUniqueString");
		// ourLogger.debug("lclUS = " + (lclUniqueString == null ? "null" : "\"" + lclUniqueString + "\""));
		final U lclU;
		if (StringUtils.isEmpty(lclUniqueString)) {
			lclU = null;
		} else {
			lclU = getFactory().forUniqueString(lclUniqueString);
		}
		// ourLogger.debug("Returning " + lclU);
		return lclU;
	}
	
	protected U getUserFacing() {
		return myUserFacing;
	}
	
	
	protected String generateFullyQualifiedName(String argName) {
		return OpalForm.generateFullyQualifiedName(getPrefix(), argName);
	}
	
	protected String getPrefixedParameter(String argName) {
		Validate.notNull(argName);
		
		return getRequest().getParameter(generateFullyQualifiedName(argName));
	}
	
	protected String getInstructions(String argName) {
		Validate.notNull(argName);
	
		return getPrefixedParameter(argName + "_Special");
	}
	
	protected String[] getPrefixedParameterValues(String argName) {
		Validate.notNull(argName);
		
		return getRequest().getParameterValues(generateFullyQualifiedName(argName));
	}
	
	public OpalFormAction determineAction() {
		if (shouldCancel()) {
			return OpalFormAction.CANCEL;
		} else if (shouldDelete()) {
			return OpalFormAction.DELETE;
		} else if (shouldSubmit()) {
			return OpalFormAction.SUBMIT;
		} else if (shouldContinue()) {
			return OpalFormAction.CONTINUE;
		} else {
			String lclParams = "{ " +
				getRequest().getParameterMap().entrySet().stream()
					.map(argE -> argE.getKey() + " -> " + Arrays.toString(argE.getValue()))
					.collect(Collectors.joining(", "))
				+ " }";
			throw new IllegalArgumentException("Could not determine action from request with parameters " + lclParams);
		}
	}
	
	protected boolean shouldCancel() {
		return getRequest().getParameter(OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "CancelButton") != null;
	}
	
	protected boolean shouldDelete() {
		return getRequest().getParameter(OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "DeleteButton") != null;
	}
	
	protected boolean shouldSubmit() {
		return getRequest().getParameter(OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "SubmitButton") != null;
	}
	
	protected boolean shouldContinue() {
		return getRequest().getParameter(OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "ContinueButton") != null;
	}
	
	@RequiresActiveTransaction
	protected void update() {
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug(getClass().getName() + "::update:  UserFacing = " + getUserFacing());
		}
		String lclDelete = getPrefixedParameter("Delete");
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("Delete parameter: " + lclDelete);
		}
		if (HTMLUtility.DEFAULT_TRUE_STRING.equalsIgnoreCase(lclDelete)) {
			validateLoadTimestamp();
			if (hasErrors()) {
				return;
			}
			delete();
		} else {
			beforeUpdate();
			createUserFacingIfNecessary();
			if (getUserFacing() == null) {
				addError("Unable to create new object.");
				return;
			} else {
				validateLoadTimestamp();
				if (hasErrors()) {
					return;
				}
				
				processSimpleFields();
				processReferences();
				processTargets();
				processChildren();
				
				processSpecial();
				
				/* If a Validator was supplied, run the method that checks the entire object. */
				if (getValidator() != null) {
					getValidator().validate(getUserFacing());
				}
				
				afterUpdate();
				
				// ourLogger.info("OpalFormUpdater.update:  Just finished afterUpdate.  hasErrors() = " + hasErrors());
			}
			appendValidatorErrors();
		}
	}
	
	/* Override this in subclasses to take some action before the update begins. */
	protected void beforeUpdate() {
		return;
	}
	
	/* Override this in subclasses to take some action after the update finishes. */
	protected void afterUpdate() {
		return;
	}
	
	/* Override this in subclasses to take some action after the update commits. */
	protected void afterCommit() {
		return;
	}
	
	/* package */ void runChildrenAfterCommits() {
		for (OpalFormUpdater<?> lclChildUpdater : getChildUpdaters()) {
			lclChildUpdater.afterCommit();
			lclChildUpdater.runChildrenAfterCommits();
		}
	}
	
	/* Override this in subclasses to take some action after the delete commits.  This will only be run if it is the updater for the MainForm. */
	protected void afterDeleteCommit() {
		return;
	}
	
	/* Override this in subclasses to handle special cases. */
	protected void processSpecial() {
		return;
	}
	
	protected void appendValidatorErrors() {
		Validator<?> lclV = getValidator();
		if (lclV != null) {
			// ourLogger.info("OpalFormUpdater.appendValidatorErrors:  Appending " + getErrors().size() + " errors.");
			getErrors().addAll(lclV.getErrors());
			for (String lclIF : lclV.getIncorrectFields()) {
				String lclFQName = generateFullyQualifiedName(lclIF);
				getIncorrectFields().add(lclFQName);
			}
		} else {
			// ourLogger.info("OpalFormUpdater.appendValidatorErrors: No validator.");
		}
		
		// ourLogger.debug("There are " + getErrors().size() + " errors.");
	}
	
	@RequiresActiveTransaction
	protected void delete() {
		if (getUserFacing() == null) {
			ourLogger.warn("Null UserFacing when deleting in " + getClass().getSimpleName() + "!  Referer: " + getRequest().getHeader("referer"));
			addError("The record you are trying to delete seems to already have been deleted.  You may need to reset the form and try again.");
		} else {
			beforeDelete();
			if (hasErrors()) {
				return;
			}
			
			unlinkChildrenAsNecessary();
			if (hasErrors()) {
				return;
			}
			
			deleteUserFacing();
			if (hasErrors()) {
				return;
			}
			
			afterDelete();
			if (hasErrors()) { // Okay, this block is redundant, but for consistency
				return;
			}
		}
	}
	
	/* Override this in subclasses to take additional actions before the UserFacing has been deleted ("unlinked") */
	protected void beforeDelete() {
		return;
	}
	
	@RequiresActiveTransaction
	protected void unlinkChildrenAsNecessary() {
		if (getUserFacing() != null) {
			for (String lclChildName : getChildrenToUnlinkUponDeletion()) {
				FieldUtility.getChildren(getUserFacing(), lclChildName).clear();
			}
		}
	}
	
	@RequiresActiveTransaction
	protected void deleteUserFacing() {
		getUserFacing().unlink();
	}
	
	/* Override this in subclasses to take additional actions after the UserFacing has been deleted ("unlinked") */
	protected void afterDelete() {
		return;
	}
	
	@RequiresActiveTransaction
	protected void createUserFacingIfNecessary() {
		// ourLogger.debug("createUserFacingIfNecessary UF = " + getUserFacing() + " Prefix = " + getPrefix());
		if (getUserFacing() == null) {
			IdentityFactory<U> lclFactory = getFactory();
			// ourLogger.debug("Creating UserFacing using " + lclFactory.getClass().getName());
			if (lclFactory instanceof FactoryCreator<?>) {
				@SuppressWarnings("unchecked")
				FactoryCreator<U> lclFactoryCreator = (FactoryCreator<U>) lclFactory;
				myUserFacing = lclFactoryCreator.create(); // Should this have a mutator?
			} else if (lclFactory instanceof FactoryPolymorphicCreator<?, ?>) {
				@SuppressWarnings("unchecked")
				FactoryPolymorphicCreator<U, ?> lclFactoryPolymorphicCreator = (FactoryPolymorphicCreator<U, ?>) lclFactory;
				myUserFacing = createProperSubclass(lclFactoryPolymorphicCreator);
			} else {
				throw new IllegalStateException("Factory is not a FactoryCreator, so new objects cannot be created.");
			}
			
			/* If we have been provided with a parent UserFacing and a method to add this child to the parent, invoke it. */
			if (getPrefixedReferenceName() != null) {
				OpalUtility.attachChild(getUserFacing(), getParent(), getPrefixedReferenceName());
			}
		}
	}
	
	protected U createProperSubclass(@SuppressWarnings("unused") FactoryPolymorphicCreator<U, ?> argFactory) {
		throw new IllegalStateException("Default implementation for createProperSubclass should never execute; if you have subclassed OpalFormUpdater to edit an Opal exhibiting polymorphism, you need to override this method to figure out what concrete class implementing the UserFacing should be constructed.");
	}
	
	protected void validateLoadTimestamp() {
		U lclUserFacing = getUserFacing();
		
		if (lclUserFacing == null) {
			return;
		}
		
		if (lclUserFacing.isNew()) {
			// No edit conflict is possible
			return;
		}
		
		String lclFormLoadTimestampString = StringUtils.trimToNull(getRequest().getParameter(OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "LoadTime")); // not getPrefixedParameter because it is global to the OpalMainForm
		if (lclFormLoadTimestampString == null) {
			ourLogger.warn("Null LoadTime");
			// But we'll let it slide.  A relatively graceful failure, but is it actually the right way to go?
			return;
		} else {
			try {
				long lclThisFormLoaded = Long.parseLong(lclFormLoadTimestampString);
				Long lclPreviousSubmission = OpalFormUpdateTimes.getInstance().get(lclUserFacing); // may be null
				
				if (lclPreviousSubmission != null && lclThisFormLoaded <= lclPreviousSubmission.longValue()) { // Equality is a weird situation.  Complaining seems like the safest choice.
					String lclConflictMessage = getPrefixedParameter("SubmissionConflictMessage");
					if (lclConflictMessage != null) {
						addError(lclConflictMessage);
						return;
					}
				} else {
					// There is no problem.
				}
			} catch (NumberFormatException lclE) {
				ourLogger.error("Received LoadTime of '" + lclFormLoadTimestampString + "'; could not parse that as a long");
			}
		}
	}
	
	@RequiresActiveTransaction
	protected void processSimpleFields() {
		automaticSimpleFields();
	}
	
	@RequiresActiveTransaction
	protected void automaticSimpleFields() {
		U lclUF = getUserFacing();
		
		Set<String> lclFieldNamesAlreadySet = new HashSet<>();
		
		for (Method lclM : lclUF.getClass().getMethods()) {
			String lclName = lclM.getName();
			if (lclName.length() > 3 && lclM.getName().startsWith("set")) {
				Class<?>[] lclParameterTypes = lclM.getParameterTypes();
				if (lclParameterTypes.length == 1) {
					/* FIXME: Maybe this is a place to use Annotations like @SimpleField rather than mucking about trying to take a guess */
					if (UserFacing.class.isAssignableFrom(lclParameterTypes[0]) == false) {
						String lclFieldName = lclM.getName().substring(3);
						
						if (lclFieldNamesAlreadySet.contains(lclFieldName)) {
							continue;
						}
						
						automaticSimpleField(lclFieldName);
						lclFieldNamesAlreadySet.add(lclFieldName);
					}
				}
			}
		}
	}
	
	@RequiresActiveTransaction
	protected void automaticSimpleField(String argFieldName) {
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("automaticSimpleField(\"" + argFieldName + "\")");
		}
//		System.out.println("automaticSimpleField(\"" + argFieldName + "\")");
		Validator<?> lclValidator = getValidator();
		if (displayed(argFieldName)) {
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("displayed = true");
			}
			
			String lclProvidedValue = getPrefixedParameter(argFieldName);
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("Raw provided value for " + argFieldName + ": '" + lclProvidedValue + "'");
			}
			
			String lclInstructions = getInstructions(argFieldName);
			
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("Instructions for " + argFieldName + " = " + lclInstructions);
			}
			boolean lclTrim = true;
			if (lclInstructions != null) {
				lclInstructions = lclInstructions.trim().toLowerCase();
				lclTrim = lclInstructions.contains("no_trim") == false;
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("lclTrim = " + lclTrim);
				}
			}
			
			if (lclTrim) {
				lclProvidedValue = StringUtils.trimToNull(lclProvidedValue);
			}
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("lclProvidedValue = \"" + lclProvidedValue + "\"");
			}
			
			String lclFQName = generateFullyQualifiedName(argFieldName);
			
			/* Update the field */
			
			U lclUF = getUserFacing();
			Class<? extends UserFacing> lclUFClass = lclUF.getClass();
			
			boolean lclUpdateField = true;
			
			/* Check validation conditions encoded in Annotations on the UserFacing */
			{
				int lclOldErrorCount = getErrors().size();
				validateAgainstAnnotations(argFieldName, lclProvidedValue);
				int lclNewErrorCount = getErrors().size();
				if (lclNewErrorCount > lclOldErrorCount) {
					getIncorrectFields().add(lclFQName);
					lclUpdateField = false;
				}
			}
			
			/* Run any validator, if necessary. */
			if (lclValidator != null) {
				int lclOldErrorCount = lclValidator.getErrors().size();
				lclValidator.validate(argFieldName, lclProvidedValue);
				int lclNewErrorCount = lclValidator.getErrors().size();
				if (lclNewErrorCount > lclOldErrorCount) {
					getIncorrectFields().add(lclFQName);
					lclUpdateField = false;
				}
			}
			
			/* Check for the existence of a companion _Verify field, as would commonly be used with
			 * password or e-mail addresses.
			 */ 
			if (lclUpdateField) {
				String lclVerifyName = OpalForm.generateVerifyName(argFieldName);
				if (displayed(lclVerifyName)) {
					String lclVerifyValue = getPrefixedParameter(lclVerifyName);
					if (StringUtils.isEmpty(lclVerifyValue)) {
						lclVerifyValue = null;
					}
					String lclFQVerifyName = generateFullyQualifiedName(lclVerifyValue);
					
					/* If there's a validator, let it handle it */
					
					if (lclValidator != null) {
						int lclOldErrorCount = lclValidator.getErrors().size();
						lclValidator.validateVerify(argFieldName, lclProvidedValue, lclVerifyValue);
						int lclNewErrorCount = lclValidator.getErrors().size();
						if (lclNewErrorCount > lclOldErrorCount) {
							getIncorrectFields().add(lclFQName);
							getIncorrectFields().add(lclFQVerifyName);
							lclUpdateField = false;
						}
					} else {
						if (lclProvidedValue == null && lclVerifyValue == null) {
							/* Both are null; fine. */
						} else if (lclProvidedValue != null && lclProvidedValue.equals(lclVerifyValue)) {
							/* Both are equal; fine. */
						} else {
							getErrors().add("The verification value for the " + argFieldName + " field did not match.");
							getIncorrectFields().add(lclFQName);
							getIncorrectFields().add(lclFQVerifyName);
							lclUpdateField = false;
						}
					}
				}
			}
			
			if (lclUpdateField) {
				Object lclNewValue = null;
				Class<?> lclFieldType = FieldUtility.getType(lclUF, argFieldName);
				
				/* Apply updater-specified default */
				if (getSimpleDefaults().containsKey(argFieldName)) {
					lclNewValue = convertSafely(argFieldName, lclFieldType, getSimpleDefaults().get(argFieldName)); // but this may still be changed in the future
				}
				
				if (lclFieldType == Boolean.class && lclProvidedValue == null) {
					lclNewValue = Boolean.FALSE;
				} else if (lclProvidedValue != null) {
					/* FIXME: What's the best way to eliminate this cast?  Do we need to make IdentityFactory inherit
					 * from Factory?
					 */
					try {
						lclNewValue = convertSafely(argFieldName, lclFieldType, lclProvidedValue);
					} catch (DateTimeParseException lclE) {
						String lclTypeName;
						if (lclFieldType == LocalDate.class) {
							lclTypeName = "date";
						} else if (lclFieldType == LocalDateTime.class) {
							lclTypeName = "date and time";
						} else {
							lclTypeName = "date/timestamp"; // ???
						}
						addError(argFieldName, "Could not interpret \"" + lclProvidedValue + "\" as a " + lclTypeName + '.');
						return;
					}
				}
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("New value for " + lclFQName + " is " + lclNewValue + " of type " + (lclNewValue != null ? lclNewValue.getClass().getName() : "none"));
				}
				/* If no explicit value was given, there is no updater-specified default, and the UserFacing is being created,
				 * we won't actually write a value, to give database defaults or a processSpecial() procedure the chance to
				 * fill in values later.
				 */
				if (lclNewValue == null && lclUF.isNew()) {
					/* Do nothing */
				} else {
					/* We only do the update if the value has actually changed. */
					
					Object lclOldValue = FieldUtility.getValue(lclUF, argFieldName);
					if (lclOldValue == null && lclNewValue == null) {
						/* No need to update */
					} else if (lclOldValue != null && lclOldValue.equals(lclNewValue)) {
						/* No need to update */
					} else {
						if (lclNewValue == null && FieldUtility.isNullable(lclUFClass, argFieldName) == Trinary.FALSE) {
							Optional<?> lclDefault = FieldUtility.getDefault(lclUFClass, argFieldName);
							if (lclDefault.isPresent()) {
								lclNewValue = lclDefault.get();
							}
						}
						
						Method lclMutator = FieldUtility.getMutator(lclUF.getClass(), argFieldName);
						if (lclMutator == null) {
							addError(argFieldName, "Could not find mutator for " + argFieldName + " on " + lclUF.getClass());
							return;
						} else {
							try {
								lclMutator.invoke(lclUF, lclNewValue);
							} catch (Exception lclE) {
								String lclDisplayMessage;
								if (lclE.getCause() instanceof ArgumentTooLongException) {
									ArgumentTooLongException lclATLE = (ArgumentTooLongException) lclE.getCause();
									
									Integer lclOverage = lclATLE.getOverage();
									if (lclOverage == null) {
										lclDisplayMessage = "Could not update the " + argFieldName + " field because the argument was too long.";
									} else {
										Validate.isTrue(lclOverage.intValue() > 0);
										String lclCharacters = lclOverage.intValue() == 1 ? "1 character" : String.valueOf(lclOverage) + " characters";
										
										lclDisplayMessage = "Could not update the " + argFieldName + " field because the argument was " + lclCharacters + " too long.";
									}
								} else {
									lclDisplayMessage = "Could not invoke mutator " + lclMutator + " to set " + argFieldName + " on " + lclUF.getClass() + " to " + lclNewValue;
								}
								
								ourLogger.error(lclDisplayMessage, lclE);
								addError(argFieldName, lclDisplayMessage);
							}
						}
					}
				}
				
				if (lclNewValue != null && lclNewValue instanceof Boolean && ((Boolean) lclNewValue).equals(Boolean.TRUE)) {
					noteAsChecked(argFieldName);
				}
			}
		}
	}
	
	public boolean isRequired(String argFieldName) {
		Validate.notBlank(argFieldName);
		
		Class<? extends UserFacing> lclUFClass = getUserFacing().getClass();
		Class<?> lclFieldType = FieldUtility.getType(lclUFClass, argFieldName);
		
		if (lclFieldType == null) {
			return false; // more like, we don't know, but the safe route is to not demand that the user specify something we don't understand
		} else if (lclFieldType == Boolean.class || lclFieldType == boolean.class) {
			return false; // because a null string for the field value is simply how "false" is represented
		} else {
			return FieldUtility.isNullable(lclUFClass, argFieldName) == Trinary.FALSE && FieldUtility.getDefault(lclUFClass, argFieldName).isPresent() == false && providesDefaultFor(argFieldName) == false;
		}
	}
	
	protected boolean providesDefaultFor(String argFieldName) {
		Validate.notBlank(argFieldName);
		
		return getSimpleDefaults().containsKey(argFieldName) || getFieldsWithComplexDefaults().contains(argFieldName);
	}
	
	protected Collection<String> getFieldsWithComplexDefaults() {
		return Collections.emptySet();
	}
	
	protected Map<String, String> getSimpleDefaults() {
		return Collections.emptyMap();
	}
	
	public static boolean handlesDeletionFor(Class<? extends OpalFormUpdater<?>> argUpdaterClass, String argFieldName) {
		OpalFormUpdater<?> lclUpdater;
		try {
			lclUpdater = argUpdaterClass.getConstructor(
				HttpServletRequest.class,
				String.class,
				String.class,
				Validator.class
			).newInstance(
				null,
				null,
				null,
				null
			);
		} catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException lclE) {
			throw new IllegalStateException("Could not create Updater object of class " + argUpdaterClass.getName(), lclE);
		}
		Validate.notNull(lclUpdater);
		
		return lclUpdater.handlesDeletionFor(argFieldName);
	}
	
	protected boolean handlesDeletionFor(String argFieldName) {
		Validate.notBlank(argFieldName);
		
		return getChildrenToUnlinkUponDeletion().contains(argFieldName) || getChildrenWithComplexDeletionHandling().contains(argFieldName);
	}
	
	protected Collection<String> getChildrenToUnlinkUponDeletion() {
		return Collections.emptySet();
	}
	
	protected Collection<String> getChildrenWithComplexDeletionHandling() {
		return Collections.emptySet();
	}
	
	public static boolean specifiesDefault(Class<? extends OpalFormUpdater<?>> argUpdaterClass, String argFieldName) {
		Validate.notNull(argUpdaterClass);
		Validate.notBlank(argFieldName);
		
		if (argUpdaterClass.equals(OpalFormUpdater.class)) {
			// Don't bother with the reflection; internally we know that the root OFU just provides emptyMap and emptySet for simple and complex defaults
			return false;
		}
		
		OpalFormUpdater<?> lclUpdater;
		try {
			lclUpdater = argUpdaterClass.getConstructor(
				HttpServletRequest.class,
				String.class,
				String.class,
				Validator.class
			).newInstance(
				null,
				null,
				null,
				null
			);
		} catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException lclE) {
			throw new IllegalStateException("Could not create Updater object of class " + argUpdaterClass.getName(), lclE);
		}
		Validate.notNull(lclUpdater);
		
		return lclUpdater.providesDefaultFor(argFieldName);
	}
	
	protected void validateAgainstAnnotations(String argFieldName, String argValue) {
		Validate.notNull(argFieldName);
		// ourLogger.debug("Validating against annotations for " + argFieldName + " and " + argValue);
		
		Class<? extends UserFacing> lclUFClass = getUserFacing().getClass();
		Class<?> lclFieldType = FieldUtility.getType(lclUFClass, argFieldName);
		
		if (isRequired(argFieldName) && argValue == null) {
			addError(argFieldName, argFieldName + " is required.");
			return;
		}
		
		if (argValue != null && lclFieldType == String.class) {
			OptionalLong lclMin = FieldUtility.getMinimumLength(lclUFClass, argFieldName);
			if (lclMin.isPresent() && argValue.length() < lclMin.getAsLong()) {
				long lclMinLength = lclMin.getAsLong();
				String lclMust;
				if (lclMinLength == 0L) {
					lclMust = " must not be blank.";
				} else if (lclMinLength == 1L) {
					lclMust = " must be at least one character long.";
				} else {
					lclMust = " must be at least " + lclMinLength + " characters long.";
				}
				
				addError(argFieldName, argFieldName + ' ' + lclMust);
			}
			
			OptionalLong lclMax = FieldUtility.getMaximumLength(lclUFClass, argFieldName);
			if (lclMax.isPresent() && argValue.length() > lclMax.getAsLong()) {
				long lclMaxLength = lclMax.getAsLong();
				String lclMust;
				if (lclMaxLength == 0L) {
					lclMust = " must be blank.";
				} else if (lclMaxLength == 1L) {
					lclMust = " cannot be more than one character long.";
				} else {
					lclMust = " cannot be longer than " + lclMax.getAsLong() + " characters.";
				}
				
				addError(argFieldName, argFieldName + ' ' + lclMust);
			}
		}
	}
	
	protected boolean displayed(String argName) {
		Validate.notNull(argName);
		String[] lclFieldNames = getPrefixedParameterValues("Displayed");
		
		String lclPrefixedName = generateFullyQualifiedName(argName);
		
		if (lclFieldNames != null) {
			for (String lclS : lclFieldNames) {
				if (lclS.equals(lclPrefixedName)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	protected boolean special(String argName) {
		Validate.notNull(argName);
		
		return getPrefixedParameter("SpecialHandler_" + argName) != null;
	}
	
	protected String generateSuccessURI() {
		return appendParameter("SuccessURI");
	}
	
	protected String generateContinueURI() {
		return appendParameter("ContinueURI");
	}
	
	protected String generateFailureURI() {
		return appendParameter("FailureURI");
	}
	
	protected String generateCancelURI() {
		return appendParameter("CancelURI");
	}
	
	protected String appendParameter(String argPrefixedParameterName) {
		Validate.notBlank(argPrefixedParameterName);
		
		IdentityUserFacing lclUF = getUserFacing();
		String lclURI = getPrefixedParameter(argPrefixedParameterName);
		
		if (lclUF == null || lclUF.isNew()) {
			return lclURI;
		} else if (lclURI.contains("?")) {
			return lclURI + '&' + HTMLUtility.percentEncode(getUniqueStringParameterName()) + '=' + HTMLUtility.percentEncode(getPresentableUniqueString());
		} else {
			return lclURI + '?' + HTMLUtility.percentEncode(getUniqueStringParameterName()) + '=' + HTMLUtility.percentEncode(getPresentableUniqueString());
		}
	}
	
	protected String getPresentableUniqueString() {
		IdentityUserFacing lclUF = getUserFacing();
		if (lclUF == null || lclUF.isNew()) {
			return null;
		} else {
			return lclUF.getUniqueString();
		}
	}
	
	protected String generateDeleteURI() {
		return getPrefixedParameter("DeleteURI");
	}
	
	protected List<String> getErrors() {
		return myErrors;
	}
	
	/* Note that errors from the Validator might not have been added yet. */
	protected boolean hasErrors() {
		return getErrors().isEmpty() == false || getChildUpdaters().stream().anyMatch(OpalFormUpdater::hasErrors);
	}
	
	protected void markField(String argFieldName) {
		Validate.notNull(argFieldName);
		getIncorrectFields().add(generateFullyQualifiedName(argFieldName));
	}
	
	/* package */ Set<Pair<String, String>> getCheckedNameValuePairs() {
		return myCheckedNameValuePairs;
	}
	
	protected void noteAsChecked(String argName, String argValue) {
		getCheckedNameValuePairs().add(Pair.of(generateFullyQualifiedName(argName), argValue));
	}
	
	protected void noteAsChecked(String argName) {
		getCheckedNameValuePairs().add(Pair.of(generateFullyQualifiedName(argName), HTMLUtility.DEFAULT_TRUE_STRING));
	}
	
	/* These next two methods are public so they can be accessed by a SpecialHandler<> that is not in the the
	 * same package as this class.
	 */
	public void addError(String argError) {
		if (StringUtils.isNotBlank(argError)) {
			getErrors().add(argError);
		}
	}
	
	public void addError(String argFieldName, String argError) {
		if (argFieldName != null) {
			markField(argFieldName);
		}
		addError(argError);
	}
	
	protected Set<String> getIncorrectFields() {
		return myIncorrectFields;
	}
	
	@RequiresActiveTransaction
	protected void processReferences() {
		automaticReferences();
	}
	
	@RequiresActiveTransaction
	protected void automaticReferences() {
		IdentityUserFacing lclUF = getUserFacing();
		// ourLogger.debug("automaticReferences for " + lclUF);
		Method[] lclMethodArray = lclUF.getClass().getMethods();
		for (Method lclM : lclMethodArray) {
			/* Is it a mutator? */
			if (lclM.getName().startsWith("set") && lclM.getParameterCount() == 1) {
				/* For a UserFacing? */
				Class<?> lclOnlyParameterType = lclM.getParameterTypes()[0];
				if (UserFacing.class.isAssignableFrom(lclOnlyParameterType)) {
					@SuppressWarnings("unchecked")
					Class<? extends IdentityUserFacing> lclType = (Class<? extends IdentityUserFacing>) lclOnlyParameterType;
					
					String lclName = lclM.getName().substring(3);
					// ourLogger.debug("Looking for " + lclName);
					if (displayed(lclName)) {
						automaticReference(lclName, lclType);
					}
				}
			}
		}
	}
	
	@RequiresActiveTransaction
	protected void automaticReference(String argFieldName, Class<? extends IdentityUserFacing> argType) {
		Validate.notBlank(argFieldName);
		Validate.notNull(argType);
		
		String lclParam = getPrefixedParameter(argFieldName);
		IdentityUserFacing lclUF = getUserFacing();
		
		if (StringUtils.isEmpty(lclParam)) {
			if (getUserFacing().isNew() && isRequired(argFieldName) && providesDefaultFor(argFieldName) == false) {
				if (getParent() == null || argType.isAssignableFrom(getParent().getClass()) == false) {
					/* TODO: This isn't perfect.  But the idea is that if we're creating this UserFacing as a child, then we don't need to set its parent here. */
					addError(argFieldName, argFieldName + " is required.");
				}
			} else if (isRequired(argFieldName) == false && providesDefaultFor(argFieldName) == false) {
				Method lclSet = null;
				try {
					lclSet = lclUF.getClass().getMethod("set" + argFieldName, argType);
					lclSet.invoke(lclUF, new Object[] {null});
				} catch (Exception lclE) { /* FIXME:  List possible exceptions */
					throw new IllegalStateException("Could not update target reference for " + argFieldName + " to null; lclUF = " + lclUF.toString() + " lclSet = " + String.valueOf(lclSet), lclE);
				}
			}
		} else {
			/* Did the form ask the user to input this information? */
			if (ourLogger.isDebugEnabled()) {
				ourLogger.debug("Working on "+ argFieldName + "; posted value is \"" + lclParam + "\"");
			}
			/* FIXME: This should use the new FactoryMap by default. */
			IdentityFactory<? extends IdentityUserFacing> lclTargetFactory = determineReferenceFactory(argFieldName, argType);
			IdentityUserFacing lclTarget;
			if (special(argFieldName)) {
				// ourLogger.debug("handling special reference for " + argFieldName + " / " + lclTargetFactory);
				try {
					lclTarget = handleSpecialReference(argFieldName, lclTargetFactory);
				} catch (IllegalArgumentException | NullPointerException lclE) {
					addError(argFieldName, lclE.getMessage());
					lclTarget = null;
				}
			} else if (StringUtils.isBlank(lclParam)) {
				lclTarget = null;
			} else {
				// ourLogger.debug(argFieldName + " is not special");
				lclTarget = lclTargetFactory.forUniqueString(lclParam);
			}
			// ourLogger.debug("lclTarget = " + lclTarget);
			if (lclTarget == null && isRequired(argFieldName)) {
				if (getUserFacing().isNew() && providesDefaultFor(argFieldName) == false) {
					addError(argFieldName, argFieldName + " is required.");
				}
			}
			Method lclSet = null;
			try {
				lclSet = lclUF.getClass().getMethod("set" + argFieldName, argType);
				lclSet.invoke(lclUF, lclTarget);
			} catch (Exception lclE) { /* FIXME:  List possible exceptions */
				throw new IllegalStateException("Could not update target reference for " + argFieldName + "; lclUF = " + lclUF.toString() + " lclSet = " + String.valueOf(lclSet) + " lclTarget = " + String.valueOf(lclTarget), lclE);
			}
		}
	}
	
	@RequiresActiveTransaction
	protected <T extends IdentityUserFacing> IdentityUserFacing handleSpecialReference(String argName, IdentityFactory<T> argTargetFactory) {
		Validate.notNull(argName);
		String lclSpecialHandlerClassName = getPrefixedParameter("SpecialHandler_" + argName);
		Validate.notNull(lclSpecialHandlerClassName);
		try {
			@SuppressWarnings("unchecked")
			Class<? extends SpecialHandler<T>> lclHandlerClass = (Class<? extends SpecialHandler<T>>) Class.forName(lclSpecialHandlerClassName);
			SpecialHandler<T> lclHandler = lclHandlerClass.getDeclaredConstructor().newInstance();
			return lclHandler.determineUserFacing(this, argTargetFactory, generateFullyQualifiedName(argName), getPrefixedParameter(argName));
		} catch (ClassNotFoundException|InstantiationException|IllegalAccessException|NoSuchMethodException|InvocationTargetException lclE) {
			throw new IllegalStateException("Could not instantiate SpecialHandler of class \"" + lclSpecialHandlerClassName + "\".", lclE);
		}
	}
	
	/* Determine the Factory that should be used to obtain the Opal that was specified as the value of
	 * a reference (foreign key) from the Opal being edited.  This is currently only called by automaticReferences. 
	 */
	protected <I extends IdentityUserFacing> IdentityFactory<I> determineReferenceFactory(String argName, Class<I> argUserFacingClass) {
		Validate.notNull(argName);
		
		/* We look up the name of the Factory that was specified (via a HIDDEN form field) in the request.  This will
		 * be specified by the OpalForm.dropdown(...) method. 
		 */
		String lclFactoryClassName = getPrefixedParameter(argName + OpalForm.FACTORY_CLASS_NAME_FORM_PARAMETER_SUFFIX);
		
		/* Did we find it? */
		if (lclFactoryClassName == null) {
			/* No.  Assume that the Factory for the referenced Opal is in the same package as that of the Opal that owns
			 * this form.  This is going to be true most of the time, but it could fail if Opals are placed in separate
			 * packages or in cases of polymorphism where the Opal and its Factory do not share a common "base name."
			 */
			String lclClassName = argUserFacingClass.getSimpleName();
			String lclPackageName = argUserFacingClass.getPackage().getName();
			lclFactoryClassName = lclPackageName + '.' + lclClassName + "Factory";
		}
		
		/* Now that we have the name of the Factory, we make use of it via reflection. */
		try {
			
			/* Look the Factory class up by name and cast it. */
			
			@SuppressWarnings("unchecked")
			Class<? extends IdentityFactory<I>> lclFactoryClass = (Class<? extends IdentityFactory<I>>) Class.forName(lclFactoryClassName);
			
			/* Look up the getInstance() method. */
			Method lclM = lclFactoryClass.getMethod("getInstance");
			
			/* Invoke that method to obtain an actual instance of the Factory. */
			@SuppressWarnings("unchecked")
			IdentityFactory<I> lclFactory = (IdentityFactory<I>) lclM.invoke(null);
			
			/* Return it. */
			return lclFactory;
		} catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException lclE) {
			/* Crap. */
			throw new IllegalStateException("Could not determine the target factory for " + argUserFacingClass.getName(), lclE);
		}
		
	}
	
	@RequiresActiveTransaction
	protected void processTargets() {
		automaticTargets();
	}
	
	@RequiresActiveTransaction
	protected void automaticTargets() {
		IdentityUserFacing lclUF = getUserFacing();
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("In automaticTargets for " + lclUF);
		}
		Method[] lclMethodArray = lclUF.getClass().getMethods();
		for (Method lclM : lclMethodArray) {
			/* Is it an accessor? */
			if (lclM.getName().startsWith("get")) {
				/* For a UserFacing? */
				Class<?> lclType = lclM.getReturnType();
				if (UserFacing.class.isAssignableFrom(lclType)) {
					String lclName = lclM.getName().substring(3);
					/* Did the form ask the user to input this information? */
					if (getPrefixedParameter(lclName + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "UserFacingUniqueString") != null) {
						if (ourLogger.isDebugEnabled()) {
							ourLogger.debug("Found displayed target \"" + lclName + "\"");
						}
						OpalFormUpdater<?> lclTargetUpdater = createChildUpdater(lclName, (Validator<?>) null);
						if (ourLogger.isDebugEnabled()) {
							ourLogger.debug("Just before update");
						}
						
						boolean lclTargetDataExistence = determineTargetDataExistence(lclName);
						IdentityUserFacing lclChildUF;
						if (lclTargetDataExistence) {
							lclTargetUpdater.update();

							getErrors().addAll(lclTargetUpdater.getErrors());
							getIncorrectFields().addAll(lclTargetUpdater.getIncorrectFields());
							
							lclChildUF = lclTargetUpdater.getUserFacing();
						} else {
							lclChildUF = null;
						}
						if (ourLogger.isDebugEnabled()) {
							ourLogger.debug("After update, lclChildUF = " + lclChildUF);
						}
						if (lclChildUF != null) {
							if (ourLogger.isDebugEnabled()) {
								ourLogger.debug("lclChildUF.isDeleted() == " + lclChildUF.isDeleted());
							}
						}
						
						try {
							IdentityUserFacing lclTarget = (IdentityUserFacing) lclM.invoke(lclUF);
							if (ourLogger.isDebugEnabled()) {
								ourLogger.debug("lclTarget = " + lclTarget);
							}
							if (lclTarget != null) {
								if (ourLogger.isDebugEnabled()) {
									ourLogger.debug("lclTarget.isDeleted() = " + lclTarget.isDeleted());
								}
							}
							if (lclTarget == null) {
								if (lclChildUF != null) {
									if (lclChildUF.isDeleted() == false) {
										if (ourLogger.isDebugEnabled()) {
											ourLogger.debug("lclTarget is null, so calling set" + lclName);
										}
										Method lclSet = lclUF.getClass().getMethod("set" + lclName, lclType);
										if (ourLogger.isDebugEnabled()) {
											ourLogger.debug("lclSet = " + lclSet);
										}
										lclSet.invoke(lclUF, lclChildUF);
										if (ourLogger.isDebugEnabled()) {
											ourLogger.debug("Invoked.");
										}
									} else {
										if (ourLogger.isDebugEnabled()) {
											ourLogger.debug("Not calling set because the child has been deleted.");
										}
									}
								} else {
									if (ourLogger.isDebugEnabled()) {
										ourLogger.debug("Not calling set because the child is null.");
									}
								}
							} else {
								/* THINK: Is this right? */
								if (ourLogger.isDebugEnabled()) {
									ourLogger.debug("Not calling set because the target is not null.");
								}
							}
						} catch (Exception lclE) {
							ourLogger.error("Could not set target to newly created object", lclE);
							throw new IllegalStateException("Could not set target to newly created object", lclE);
						}
					}
				}
			}
		}
	}
	
	protected boolean determineTargetDataExistence(String argName) {
		String lclUniqueString = getPrefixedParameter(argName + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "UserFacingUniqueString");
		if (StringUtils.isNotBlank(lclUniqueString)) {
			return true;
		}
		String[] lclDisplayed = getPrefixedParameterValues(argName + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "Displayed");
		boolean lclData = false;
		if (lclDisplayed != null) {
			for (String lclFieldName : lclDisplayed) {
				if (ourLogger.isDebugEnabled()) {
					ourLogger.debug("Looking for parameter named " + lclFieldName);
				}
				String lclValue = getPrefixedParameter(lclFieldName);
				if (StringUtils.isNotBlank(lclValue)) {
					if (ourLogger.isDebugEnabled()) {
						ourLogger.debug(lclFieldName + " is not blank (" + lclValue + "), so we have data");
					}
					lclData = true;
				}
			}
		}
		/* This next clause might be redundant since we now create and execute a child updater whenever lclChildUniqueString
		 * is not "".
		 */
		if (lclData == false) {
			String lclValue = getPrefixedParameter(argName + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "Delete");
			if (StringUtils.isNotBlank(lclValue)) {
				lclData = true;
			}
		}
		return lclData;
	}
	
	
	@RequiresActiveTransaction
	protected void processChildren() {
		automaticChildren();
	}
	
	private static final String TARGET_INDICATOR = "Target:";
	
	@RequiresActiveTransaction
	protected void automaticChildren() {
		if (ourLogger.isDebugEnabled()) {
			ourLogger.debug("Doing automaticChildren for " + getUserFacing());
		}
		IdentityUserFacing lclUF = getUserFacing();
		Method[] lclMethodArray = lclUF.getClass().getMethods();
		for (Method lclM : lclMethodArray) {
			/* Is it a creator method for a child array?  We use createChildArray() instead of getChildSet() because the latter can be confused with regular field accessors */
			// TODO: Can this use FieldUtility.getChildren()?
			if (lclM.getName().startsWith("create") && lclM.getName().endsWith("Array")) {
				Class<?> lclArrayType = lclM.getReturnType();
				if (lclArrayType.isArray()) {
//					Class<?> lclType = lclArrayType.getComponentType();
					String lclMethodName = lclM.getName();
					String lclPrefixedReferenceName = StringUtils.removeEnd(StringUtils.removeStart(lclMethodName, "create"), "Array");
					String lclChildUniqueString;
					for (int lclI = 0; (lclChildUniqueString = getPrefixedParameter(lclPrefixedReferenceName + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + lclI + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "UserFacingUniqueString")) != null; ++lclI) {
						if (ourLogger.isDebugEnabled()) {
							ourLogger.debug("Doing child " + lclPrefixedReferenceName + " row #" + lclI);
						}
						/* There's a row for this child (number lclI), but it might be a row provided to allow
						 * for the insertion of new data.  If that's the case, we need to make sure that at least
						 * some data was entered for it, otherwise we don't want to create the new object.
						 */
						String[] lclDisplayed = getPrefixedParameterValues(lclPrefixedReferenceName + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + lclI + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "Displayed");
						boolean lclData = false;
						if (lclDisplayed != null) {
							for (String lclFieldName : lclDisplayed) {
								if (lclFieldName.contains(TARGET_INDICATOR)) {
									String lclTargetType = lclFieldName.substring(lclFieldName.indexOf(TARGET_INDICATOR) + TARGET_INDICATOR.length());
									if (ourLogger.isDebugEnabled()) {
										ourLogger.debug("A target was displayed: " + lclTargetType);
									}
									// FIXME: This will only work if one level of targeting produces real data. We really need some sort of recursion.
									String[] lclTargetDisplayed = getPrefixedParameterValues(lclPrefixedReferenceName + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + lclI + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + lclTargetType + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "Displayed");
									if (lclTargetDisplayed != null) {
										for (String lclTargetName : lclTargetDisplayed) {
											if (ourLogger.isDebugEnabled()) {
												ourLogger.debug("Looking for target form's parameter named " + lclTargetName);
											}
											String lclValue = getPrefixedParameter(lclTargetName);
											if (StringUtils.isNotBlank(lclValue)) {
												if (ourLogger.isDebugEnabled()) {
													ourLogger.debug(lclTargetName + " in target " + lclTargetType + " is not blank (" + lclValue + "), so we have data");
												}
												lclData = true;
											}
										}
									}
								} else {
									if (ourLogger.isDebugEnabled()) {
										ourLogger.debug("Looking for parameter named " + lclFieldName);
									}
									String lclValue = getPrefixedParameter(lclFieldName);
									if (StringUtils.isNotBlank(lclValue)) {
										if (ourLogger.isDebugEnabled()) {
											ourLogger.debug(lclFieldName + " is not blank (" + lclValue + "), so we have data");
										}
										lclData = true;
									}
								}
							}
						}
						/* This next clause might be redundant since we now create and execute a child updater whenever lclChildUniqueString
						 * is not "".
						 */
						if (lclData == false) {
							String lclValue = getPrefixedParameter(lclPrefixedReferenceName + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + lclI + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "Delete");
							if (StringUtils.isNotBlank(lclValue)) {
								lclData = true;
							}
						}
						
						// ourLogger.debug("lclData = " + lclData);
						if (lclData || StringUtils.isNotEmpty(lclChildUniqueString) || mustCreateChild(lclPrefixedReferenceName)) {
							OpalFormUpdater<?> lclChildUpdater;
							lclChildUpdater = createChildUpdater(lclPrefixedReferenceName + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + lclI, (Validator<?>) null, lclPrefixedReferenceName);
							Validate.notNull(lclChildUpdater);
							lclChildUpdater.update();
							
							/* THINK: How do we handle the incorrect fields and errors from the child? */
							getErrors().addAll(lclChildUpdater.getErrors());
							getIncorrectFields().addAll(lclChildUpdater.getIncorrectFields());
						}
					}
				}
			}
		}
	}
	
	// Override in subclasses
	protected boolean mustCreateChild(@SuppressWarnings("unused") String argPrefixedReferenceName) {
		return false;
	}
	
	protected <T extends IdentityUserFacing> OpalFormUpdater<T> createChildUpdater(String argNewPrefix) {
		OpalFormUpdater<T> lclOFU = createChildUpdater(argNewPrefix, (Validator<T>) null);
		lclOFU.setUsername(getUsername());
		return lclOFU;
	}
	
	protected <T extends IdentityUserFacing> OpalFormUpdater<T> createChildUpdater(String argNewPrefix, Validator<T> argValidator) {
		OpalFormUpdater<T> lclOFU = createUpdater(getRequest(), getPrefix() + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + argNewPrefix, null, argValidator);
		lclOFU.setUsername(getUsername());
		myChildUpdaters.add(lclOFU);
		return lclOFU;
	}
	
	protected <T extends IdentityUserFacing> OpalFormUpdater<T> createChildUpdater(String argNewPrefix, Validator<T> argValidator, String argPrefixedReferenceName) {
		Validate.notBlank(argNewPrefix);
		Validate.notBlank(argPrefixedReferenceName);
		
		OpalFormUpdater<T> lclOFU = createUpdater(getRequest(), getPrefix() + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + argNewPrefix, null, argValidator, argPrefixedReferenceName, getUserFacing());
		lclOFU.setUsername(getUsername());
		myChildUpdaters.add(lclOFU);
		return lclOFU;
	}
	
	/* package */ static <T extends IdentityUserFacing> OpalFormUpdater<T> createUpdater(HttpServletRequest argRequest, String argPrefix, String argUniqueStringParameterName) {
		return createUpdater(argRequest, argPrefix, argUniqueStringParameterName, (Validator<T>) null);
	}
	
	/* package */ static <T extends IdentityUserFacing> OpalFormUpdater<T> createUpdater(HttpServletRequest argRequest, String argPrefix, String argUniqueStringParameterName, Validator<T> argValidator) {
		return createUpdater(argRequest, argPrefix, argUniqueStringParameterName, argValidator, null, null);
	}
	
	/* package */ static <T extends IdentityUserFacing> OpalFormUpdater<T> createUpdater(HttpServletRequest argRequest, String argPrefix, String argUniqueStringParameterName, Validator<T> argValidator, String argPrefixedReferenceName, UserFacing argParent) {
		Validate.isTrue((argPrefixedReferenceName == null) == (argParent == null), "argPrefixedReferenceName may be null if and only if argParent is null");
		
		String lclUpdaterClassName = argRequest.getParameter(argPrefix + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "UpdaterClassName");
		
		final Class<OpalFormUpdater<T>> lclUpdaterClass;
		
		if (lclUpdaterClassName == null) {
			// Class<?> lclC = OpalFormUpdater.class;
			// lclUpdaterClass = (Class<OpalFormUpdater<T>>) lclC;
			
			/* THINK: So help me God, it seems like we ought to be able to remove the Class<?> cast from this line,
			 * but Eclipse refuses to compile it that way.  If anybody knows the answer of why that second cast makes
			 * this work (albeit with a warning), please let me know. -- RRH
			 */
			
			@SuppressWarnings("unchecked")
			Class<OpalFormUpdater<T>> lclUpdaterHolder = (Class<OpalFormUpdater<T>>) ((Class<?>) OpalFormUpdater.class);
			lclUpdaterClass = lclUpdaterHolder;
		} else {
			try {
				@SuppressWarnings("unchecked")
				Class<OpalFormUpdater<T>> lclUpdaterHolder = (Class<OpalFormUpdater<T>>) Class.forName(lclUpdaterClassName);
				lclUpdaterClass = lclUpdaterHolder;
			} catch (Exception lclE) {
				throw new IllegalStateException("Could not find Updater class", lclE);
			}
		}
		
		final Validator<T> lclValidator;
		String lclValidatorClassName = argRequest.getParameter(argPrefix + OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "ValidatorClassName");
		
		if (lclValidatorClassName == null) {
			lclValidator = argValidator;
		} else {
			try {
				@SuppressWarnings("unchecked")
				Validator<T> lclValidatorHolder = (Validator<T>) Class.forName(lclValidatorClassName).getDeclaredConstructor().newInstance();
				lclValidator = lclValidatorHolder;
			} catch (Exception lclE) {
				throw new IllegalStateException("Could not create Validator", lclE);
			}
		}
		
		OpalFormUpdater<T> lclOFU;
		try {
			lclOFU = lclUpdaterClass.getConstructor(
				HttpServletRequest.class,
				String.class,
				String.class,
				Validator.class
			).newInstance(
				argRequest,
				argPrefix,
				argUniqueStringParameterName,
				lclValidator
			);
		} catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException lclE) {
			throw new IllegalStateException("Could not create Updater object of class " + lclUpdaterClass.getName(), lclE);
		}
		
		if (argPrefixedReferenceName != null) {
			lclOFU.setPrefixedReferenceName(argPrefixedReferenceName);
			lclOFU.setParent(argParent);
		}
		
		return lclOFU;
	}
	
	public String getUsername() {
		return myUsername;
	}
	
	public void setUsername(String argUsername) {
		myUsername = argUsername;
	}
	
	public void successfulSubmit() {
		return;
	}
	
	public void successfulContinue() {
		return;
	}
	
	public String customAction() {
		return null;
	}
	
	protected String getPrefixedReferenceName() {
		return myPrefixedReferenceName;
	}
	
	protected void setPrefixedReferenceName(String argPrefixedReferenceName) {
		myPrefixedReferenceName = argPrefixedReferenceName;
	}
	
	protected UserFacing getParent() {
		return myParent;
	}
	
	/* package */ Collection<OpalFormUpdater<?>> getChildUpdaters() {
		return myChildUpdaters;
	}
	
	@RequiresActiveTransaction
	protected void setParent(UserFacing argParent) {
		myParent = argParent;
	}
	
	protected long getDefaultTimeout() {
		return TransactionContext.DEFAULT_TIME_OUT;
	}
	
	// Override in subclasses. This will only be used from the Updater for the MainForm.
	public long determineDeletionTimeout() {
		return getDefaultTimeout();
	}
	
	// Override in subclasses. This will only be used from the Updater for the MainForm.
	public long determineUpdateTimeout() {
		return getDefaultTimeout();
	}
	
	protected <T> T convertSafely(String argFieldName, Class<T> argTargetType, Object argInput) {
		try {
			return convert(argFieldName, argTargetType, argInput);
		} catch (Exception lclE) {
			ourLogger.warn("Could not convert \"" + String.valueOf(argInput) + "\" (" + (argInput == null ? null : argInput.getClass().getName()) + ") to a " + argTargetType.getName() + " for the " + argFieldName + " field of " + getFactory().getUserFacingInterface().getSimpleName() + "; user is " + getUsername(), lclE);
			
			addError(argFieldName, "We couldn't understand the value \"" + String.valueOf(argInput) + "\".");
			
			return null;
		}
	}
	
	// Could override in subclasses
	// argFieldName is present so that an overriding method can be selective, e.g. if argFieldName is x, then do y, otherwise call super.convert
	protected <T> T convert(@SuppressWarnings("unused") String argFieldName, Class<T> argTargetType, Object argInput) {
		// For dates and datetimes, we're implicitly relying on the OpalUtility to use the same formats as DateField.WIRE_FORMAT and DateTimeField.WIRE_FORMAT, which themselves come from the HTML specification
		return OpalUtility.convertTo(argTargetType, argInput);
	}
}
