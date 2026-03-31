package com.siliconage.web.exception;

import jakarta.servlet.http.HttpServletResponse;

public class BadRequestException extends WebException {

	private static final long serialVersionUID = 1L;

	public BadRequestException() {
		this("There was a problem with your request.");
	}
	
	public BadRequestException(String argMessageForUser) {
		this(argMessageForUser, null);
	}
	
	public BadRequestException(String argMessageForUser, Throwable argCause) {
		super("Problem", argMessageForUser, argCause);
	}
	
	@Override
	public int getHttpStatus() {
		return HttpServletResponse.SC_BAD_REQUEST;
	}
}
