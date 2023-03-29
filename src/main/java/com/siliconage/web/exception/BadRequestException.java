package com.siliconage.web.exception;

import javax.servlet.http.HttpServletResponse;

public class BadRequestException extends WebException {
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
