package com.siliconage.web.exception;

import jakarta.servlet.http.HttpServletResponse;

public class BadRequestException extends WebException {

	private static final long serialVersionUID = 1L;

	public BadRequestException() {
		super("Problem", "There was a problem with your request.");
	}
	
	public BadRequestException(String argMessageForUser) {
		super("Problem", argMessageForUser);
	}
	
	@Override
	public int getHttpStatus() {
		return HttpServletResponse.SC_BAD_REQUEST;
	}
}
