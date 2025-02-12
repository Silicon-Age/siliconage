package com.siliconage.web.exception;

import javax.servlet.http.HttpServletResponse;

public class NotFoundException extends WebException {
	public NotFoundException() {
		super("Not Found", "Something you requested could not be found.");
	}
	
	public NotFoundException(String argMessageForUser) {
		super("Not Found", argMessageForUser);
	}
	
	@Override
	public int getHttpStatus() {
		return HttpServletResponse.SC_NOT_FOUND;
	}
}
