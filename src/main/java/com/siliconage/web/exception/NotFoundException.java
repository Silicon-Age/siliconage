package com.siliconage.web.exception;

import jakarta.servlet.http.HttpServletResponse;

public class NotFoundException extends WebException {

	private static final long serialVersionUID = 1L;

	public NotFoundException() {
		this("Something you requested could not be found.");
	}
	
	public NotFoundException(String argMessageForUser) {
		this(argMessageForUser, null);
	}
	
	public NotFoundException(String argMessageForUser, Throwable argCause) {
		super("Not Found", argMessageForUser, argCause);
	}
	
	@Override
	public int getHttpStatus() {
		return HttpServletResponse.SC_NOT_FOUND;
	}
}
