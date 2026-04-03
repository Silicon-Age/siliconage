package com.siliconage.web.exception;

import jakarta.servlet.http.HttpServletResponse;

public class DataConflictException extends WebException {

	private static final long serialVersionUID = 1L;

	public DataConflictException() {
		this("Your request cannot be processed due to conflicting data.");
	}
	
	public DataConflictException(String argMessageForUser) {
		this(argMessageForUser, null);
	}
	
	public DataConflictException(String argMessageForUser, Throwable argCause) {
		super("Conflict", argMessageForUser, argCause);
	}
	
	@Override
	public int getHttpStatus() {
		return HttpServletResponse.SC_CONFLICT;
	}
}
