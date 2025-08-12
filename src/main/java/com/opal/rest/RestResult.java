package com.opal.rest;

import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

public class RestResult {

	private final int myStatusCode;
	private final JsonObject myBody;
	private final String myErrorMessage;
	private final Throwable myThrowable;
	
	private static final String CONFLICT_ERROR_MESSAGE = "Conflict"; // THINK: Tell Matt about this
	private static final String FORBIDDEN_ERROR_MESSAGE = "Access is forbidden";
	private static final String NOT_FOUND_ERROR_MESSAGE = "Not found";
	
	public RestResult(int argStatusCode) {
		super();
		
		/* THINK: Should we check for 200 v. 204 and the presnece of a body? */
		myStatusCode = argStatusCode;
		myBody = null;
		myErrorMessage = null;
		myThrowable = null;
	}
	
	public RestResult(int argStatusCode, Throwable argT) {
		super();
		
		if (argStatusCode >= 200 && argStatusCode <= 299 && argT != null) {
			throw new IllegalArgumentException("If argT is not null, the status code may not represent success.");
		}
		
		myStatusCode = argStatusCode;
		myBody = null;
		myErrorMessage = null; // THINK: Use getMessage() from the Throwable?
		myThrowable = argT;
	}
	
	public RestResult(int argStatusCode, String argErrorMessage, Throwable argT) {
		super();
		
		if (argStatusCode >= 200 && argStatusCode <= 299 && argT != null) {
			throw new IllegalArgumentException("If argT is not null, the status code may not represent success.");
		}
		
		myStatusCode = argStatusCode;
		myBody = null;
		myErrorMessage = argErrorMessage;
		myThrowable = argT;
	}
	
	public RestResult(int argStatusCode, JsonObject argBody) {
		super();
		
		/* THINK: Should we check for 200 v. 204 and the presnece of a body? */
		myStatusCode = argStatusCode;
		myBody = argBody;
		myErrorMessage = null;
		myThrowable = null;
	}
	
	public RestResult(int argStatusCode, String argErrorMessage) {
		super();
		
		if (argStatusCode >= 200 && argStatusCode <= 299 && argErrorMessage != null) {
			throw new IllegalArgumentException("If argErrorMessage is not null, the status code may not represent success.");
		}
		
		myStatusCode = argStatusCode;
		myBody = null;
		myErrorMessage = argErrorMessage;
		myThrowable = null;
	}
	
	public RestResult(int argStatusCode, String argErrorMessage, JsonObject argBody) {
		super();
		
		if (argStatusCode >= 200 && argStatusCode <= 299 && argErrorMessage != null) {
			throw new IllegalArgumentException("If argErrorMessage is not null, the status code may not represent success.");
		}
		
		myStatusCode = argStatusCode;
		myBody = argBody;
		myErrorMessage = argErrorMessage;
		myThrowable = null;
	}
	
	/* Convenience factory methods for common types of responses */
	
	static public RestResult ok() {
		return new RestResult(HttpServletResponse.SC_NO_CONTENT, (JsonObject) null);
	}
	
	static public RestResult ok(JsonObject argJO) { // THINK: Do we need to verify that the content isn't null?
		return new RestResult(HttpServletResponse.SC_OK, argJO);
	}
	
	static public RestResult badRequest(String argErrorMessage) {
		return new RestResult(HttpServletResponse.SC_BAD_REQUEST, argErrorMessage);
	}
	
	static public RestResult badRequest(String argErrorMessage, Throwable argT) {
		return new RestResult(HttpServletResponse.SC_BAD_REQUEST, argErrorMessage, argT);
	}
	
	static public RestResult conflict(JsonObject argJO) {
		return new RestResult(HttpServletResponse.SC_CONFLICT, CONFLICT_ERROR_MESSAGE, argJO);
	}
	
	static public RestResult methodNotAllowed() {
		return new RestResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	static public RestResult internalError(String argMessage, Throwable argT) {
		return new RestResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, argMessage, argT);
	}

	static public RestResult internalError(Throwable argT) {
		return new RestResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, argT);
	}

	static public RestResult forbidden() {
		return forbidden(FORBIDDEN_ERROR_MESSAGE);
	}
	
	static public RestResult forbidden(String argMessage) {
		return new RestResult(HttpServletResponse.SC_FORBIDDEN, argMessage);
	}
	
	static public RestResult notFound() {
		return notFound(NOT_FOUND_ERROR_MESSAGE);
	}
	
	static public RestResult notFound(String argMessage) {
		return new RestResult(HttpServletResponse.SC_NOT_FOUND, argMessage);
	}
	
	/* Accessors */
	
	public int getStatusCode() {
		return myStatusCode;
	}
	
	public boolean hasBody() {
		return getBody() != null;
	}
	
	public JsonObject getBody() {
		return myBody;
	}

	public boolean hasErrorMessage() {
		return getErrorMessage() != null;
	}
	
	public String getErrorMessage() {
		return myErrorMessage;
	}
	
	public boolean hasThrowable() {
		return getThrowable() != null;
	}
	
	public Throwable getThrowable() {
		return myThrowable;
	}
}
