package com.siliconage.web.exception;

import jakarta.servlet.http.HttpServletResponse;

public class AuthorizationException extends WebException {

	private static final long serialVersionUID = 1L;

	public AuthorizationException() {
		this("You are not authorized to access that.");
	}
	
	public AuthorizationException(String argMessageForUser) {
		this(argMessageForUser, null);
	}
	
	public AuthorizationException(String argMessageForUser, Throwable argCause) {
		super("Not Authorized", argMessageForUser, argCause);
	}
	
	@Override
	public int getHttpStatus() {
		return HttpServletResponse.SC_FORBIDDEN;
	}
}
