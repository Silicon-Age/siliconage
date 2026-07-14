package com.opal.cma;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.opal.IdentityUserFacing;
import com.siliconage.util.Fast3Set;

/* FIXME: Why does this need a type argument that extends IdentityUserFacing instead of UserFacing? */

public abstract class Validator<U extends IdentityUserFacing> {
	/* FIXME:  This list of errors may be a useless complication; if every Validator is used by an OpalFormUpdater
	 * that has its own list of errors, perhaps that object should pass in its list (or itself) and the Validator
	 * can add errors directly to it.
	 */
	private final ArrayList<String> myErrors = new ArrayList<>();
	private final Set<String> myIncorrectFields = new Fast3Set<>();
	
	protected Validator() {
		super();
	}
	
	public void validate(String argFieldName, String argValue) {
		try {
			String lclMethodName = "validate" + argFieldName;
//			System.out.println("Looking for " + lclMethodName);
			Method lclM = this.getClass().getMethod(lclMethodName, String.class);
			assert lclM != null;
			lclM.invoke(this, argValue);
		} catch (NoSuchMethodException _) {
//			System.out.println("Did not find it.");
			return;
		} catch (IllegalAccessException lclE) {
			throw new IllegalStateException("Did not have access to validation method.", lclE);
		} catch (InvocationTargetException lclE) {
			throw new IllegalStateException("Could not execute validation method.", lclE);
		}
	}
	
	public void validateVerify(String argFieldName, String argValue, String argVerifyValue) {
		try {
			String lclMethodName = "validateVerify" + argFieldName;
			Method lclM = this.getClass().getMethod(lclMethodName, String.class, String.class);
			assert lclM != null;
			lclM.invoke(this, argValue, argVerifyValue);
		} catch (@SuppressWarnings("unused") NoSuchMethodException lclE) {
			if (argValue == null && argVerifyValue == null) {
				/* Both are null; fine. */
			} else if (argValue != null && argValue.equals(argVerifyValue)) {
				/* Both are equal; fine. */
			} else {
				addError(argFieldName, "The verification value for the " + argFieldName + " field did not match.");
			}

			return;
		} catch (IllegalAccessException lclE) {
			throw new IllegalStateException("Did not have access to validation method.", lclE);
		} catch (InvocationTargetException lclE) {
			throw new IllegalStateException("Could not execute validation method.", lclE);
		}
	}
	
	public void validate(@SuppressWarnings("unused") U argUF) {
		return;
	}
	
	@SuppressWarnings("unchecked")
	public final void validate(Object argO) {
		try {
			validate((U) argO);
		} catch (ClassCastException _) {
			throw new IllegalStateException("Validator was passed the wrong kind of object.");
		}
	}
	
	public List<String> getErrors() {
		return myErrors;
	}
	
	public boolean hasErrors() {
		return getErrors().size() > 0;
	}
	
	public void addError(String argError) {
		getErrors().add(Objects.requireNonNull(argError));
	}
	
	public void addError(String argFieldName, String argError) {
		addError(Objects.requireNonNull(argError));
		markField(Objects.requireNonNull(argFieldName));
	}
	
	public Set<String> getIncorrectFields() {
		return myIncorrectFields;
	}
	
	public boolean hasIncorrectFields() {
		return getIncorrectFields().size() > 0;
	}
	
	public void markField(String argFieldName) {
		getIncorrectFields().add(Objects.requireNonNull(argFieldName));
	}
}
