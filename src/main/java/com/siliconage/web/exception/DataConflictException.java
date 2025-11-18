package com.siliconage.web.exception;

import jakarta.servlet.http.HttpServletResponse;

public class DataConflictException extends WebException {

	private static final long serialVersionUID = 1L;

	public DataConflictException() {
		super("Conflict", "Your request cannot be processed due to conflicting data.");
	}
	
	public DataConflictException(String argMessageForUser) {
		super("Conflict", argMessageForUser);
	}
	
	@Override
	public int getHttpStatus() {
		return HttpServletResponse.SC_CONFLICT;
	}
}
