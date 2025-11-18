package com.siliconage.web.exception;

import jakarta.servlet.http.HttpServletResponse;

public class AuthorizationException extends WebException {

	private static final long serialVersionUID = 1L;

	public AuthorizationException() {
		super("Not Authorized", "You are not authorized to access that.");
	}
	
	public AuthorizationException(String argMessageForUser) {
		super("Not Authorized", argMessageForUser);
	}
	
	@Override
	public int getHttpStatus() {
		return HttpServletResponse.SC_FORBIDDEN;
	}
}
