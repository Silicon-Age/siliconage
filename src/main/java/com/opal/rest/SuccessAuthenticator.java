package com.opal.rest;

import jakarta.servlet.http.HttpServletRequest;

public class SuccessAuthenticator extends Authenticator<Void> {

	@Override
	public Void getCredential(HttpServletRequest argRequest) {
		return null; 
	}
	
}
