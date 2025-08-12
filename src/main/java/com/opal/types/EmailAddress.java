package com.opal.types;

import java.io.Serializable;

public class EmailAddress implements Cloneable, Serializable, Comparable<EmailAddress> {
	private static final long serialVersionUID = 1L;
	
	private final String myEmail;
	
	public EmailAddress(String argEmail) {
		super();
		
		validate(argEmail);
		myEmail = argEmail;
		return;
	}
	
	public static final String check(String argEmail) {
		if (argEmail == null) {
			return "Null values are not allowed.";
		}
		
		if (argEmail.indexOf('@') == -1) {
			return "The address must contain a '@'";
		}
		
		return null;
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException lclE) {
			throw new IllegalStateException("Clone is supposed to be supported");
		}
	}
	
	@Override
	public int compareTo(EmailAddress argEA) {
		return this.getEmail().compareTo(argEA.getEmail());
	}
	
	@Override
	public boolean equals(Object argO) {
		if (argO == null) {
			return false;
		}
		
		if (argO.getClass() != this.getClass()) {
			return false;
		}
		
		return getEmail().equals(((EmailAddress) argO).getEmail());
	}
	
	@Override
	public int hashCode() {
		return getEmail().hashCode();
	}
	
	protected String getEmail() {
		return myEmail;
	}
	
	@Override
	public String toString() {
		return myEmail;
	}
	
	public String toValueString() {
		return myEmail;
	}
	
	public static final void validate(String argEmail) {
		String lclReason = check(argEmail);
		if (lclReason == null) {
			return;
		} else {
			throw new IllegalArgumentException("Email Address " + argEmail + " is invalid:  " + lclReason);
		}
	}
}
