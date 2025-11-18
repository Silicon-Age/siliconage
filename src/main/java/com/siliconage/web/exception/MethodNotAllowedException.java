package com.siliconage.web.exception;

import jakarta.servlet.http.HttpServletResponse;

public class MethodNotAllowedException extends WebException {

	private static final long serialVersionUID = 1L;

	public MethodNotAllowedException() {
		super("Impermissible Request", "The type of request you made is not permitted.");
	}
	
	public MethodNotAllowedException(String argMessageForUser) {
		super("Impermissible Request", argMessageForUser);
	}
	
	@Override
	public int getHttpStatus() {
		return HttpServletResponse.SC_METHOD_NOT_ALLOWED;
	}
}
