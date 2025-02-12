package com.siliconage.web.exception;

import javax.servlet.http.HttpServletResponse;

public class AuthorizationException extends WebException {
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
