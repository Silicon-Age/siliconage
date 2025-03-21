package com.siliconage.web.exception;

import javax.servlet.http.HttpServletResponse;

public class DataConflictException extends WebException {
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
