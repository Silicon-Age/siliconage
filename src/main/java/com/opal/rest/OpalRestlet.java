package com.opal.rest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.opal.IdentityFactory;
import com.opal.IdentityUserFacing;
import com.opal.TransactionContext;
import com.opal.types.UTCDateTime;

public abstract class OpalRestlet<U extends IdentityUserFacing, A> extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalRestlet.class.getName());

	private static final Gson ourSerializer = new GsonBuilder().disableHtmlEscaping().create();

	public static final String CONTENT_TYPE = "application/json";

	public static final DateTimeFormatter JSON_ZONED_DATE_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	public static final DateTimeFormatter JSON_LOCAL_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
	
	private static final String SEARCH_RESULT_KEY = "items";

	private static final String DEFAULT_ERROR_MESSAGE = "Your request could not be processed.";
	
	public OpalRestlet() {
		super();
	}
	
	protected abstract IdentityFactory<U> getFactory();
	
	protected abstract Authenticator<A> getAuthenticator();
	
	protected abstract boolean isEntityGetAllowed();	
	protected abstract boolean isSearchGetAllowed();	
	protected abstract boolean isPostAllowed();	
	protected abstract boolean isPutAllowed();
	protected abstract boolean isDeleteAllowed();

	protected abstract boolean checkAccess(U argUF, A argCredential);

//	@SuppressWarnings("unused")
//	protected boolean checkAccess(U argUF, A argCredential) {
//		ourLogger.warn("Default checkAccess(...) is being called.");
//		return false;
//	}
	
	protected boolean checkGetAccess(U argUF, A argCredential) {
		return checkAccess(argUF, argCredential);
	}

	protected boolean checkPutAccess(U argUF, A argCredential) {
		return checkAccess(argUF, argCredential);
	}

	protected boolean checkDeleteAccess(U argUF, A argCredential) {
		return checkAccess(argUF, argCredential);
	}

	protected boolean checkPostAccess(A argCredential) {
		return checkAccess(null, argCredential);
	}

	protected void ensureAccess(U argUF, A argCredential) throws RestResultException {
		if (checkAccess(argUF, argCredential) == false) {
			throw new RestResultException(RestResult.forbidden());
		}
	}
	
	protected void ensureGetAccess(U argUF, A argCredential) throws RestResultException {
		if (checkGetAccess(argUF, argCredential) == false) {
			throw new RestResultException(RestResult.forbidden());
		}
	}

	protected void ensurePutAccess(U argUF, A argCredential) throws RestResultException {
		if (checkPutAccess(argUF, argCredential) == false) {
			throw new RestResultException(RestResult.forbidden());
		}
	}

	protected void ensureDeleteAccess(U argUF, A argCredential) throws RestResultException {
		if (checkDeleteAccess(argUF, argCredential) == false) {
			throw new RestResultException(RestResult.forbidden());
		}
	}

	protected void ensurePostAccess(A argCredential) throws RestResultException {
		if (checkPostAccess(argCredential) == false) {
			throw new RestResultException(RestResult.forbidden());
		}
	}

	protected boolean isGetAllowed() {
		return isEntityGetAllowed() || isSearchGetAllowed();
	}

	protected static UTCDateTime parseUTC(HttpServletRequest argRequest, String argParameterName) throws RestResultException {
		if (argParameterName == null) {
			return null;
		}
		return parseUTC(argRequest.getParameter(argParameterName));
	}
	
	protected static UTCDateTime parseUTC(String argDate) throws RestResultException {
		if (argDate == null) {
			return null;
		}
		try {
			return UTCDateTime.parse(argDate, JSON_ZONED_DATE_TIME_FORMAT);
		} catch (DateTimeParseException lclE) {
			throw new RestResultException(RestResult.badRequest("Could not parse UTC datetime \"" + argDate + "\"", lclE)); // THINK: Is this always a bad request?
		}
	}
	
	protected static LocalDate parseLocal(HttpServletRequest argRequest, String argParameterName) throws RestResultException {
		if (argParameterName == null) {
			return null;
		}
		return parseLocal(argRequest.getParameter(argParameterName));
	}
	
	protected static LocalDate parseLocal(String argDate) throws RestResultException {
		if (argDate == null) {
			return null;
		}
		try {
			return LocalDate.parse(argDate, JSON_LOCAL_DATE_FORMAT);
		} catch (DateTimeParseException lclE) {
			throw new RestResultException(RestResult.badRequest("Could not parse local date \"" + argDate + "\"", lclE)); // THINK: Is this always a bad request?
		}
	}
	
	protected static String format(UTCDateTime argUTCDT) {
		if (argUTCDT == null) {
			return null;
		}
		return JSON_ZONED_DATE_TIME_FORMAT.format(argUTCDT);
	}
	
	protected static String format(LocalDate argLD) {
		if (argLD == null) {
			return null;
		}
		return JSON_LOCAL_DATE_FORMAT.format(argLD);
	}
	
	protected static int intValueOf(String argS) throws RestResultException {
		if (argS == null) {
			throw new RestResultException(RestResult.badRequest("Invalid integer null"));
		}
		try {
			return Integer.parseInt(argS);
		} catch (NumberFormatException lclE) {
			throw new RestResultException(RestResult.badRequest("Invalid integer \"" + argS + "\""));
		}
	}
	
	protected Pair<U, String> getEntityFromPathInfo(HttpServletRequest argRequest, boolean argIdRequired) throws RestResultException {
		String lclPathInfo = argRequest.getPathInfo();
		if (lclPathInfo == null) {
			if (argIdRequired) {
				throw new RestResultException(RestResult.badRequest("Missing identifier"));
			} else {
				return null;
			}
		} else {
			Validate.isTrue(lclPathInfo.length() > 0);
			Validate.isTrue(lclPathInfo.charAt(0) == '/');
			ourLogger.warn("lclPIString = \"" + lclPathInfo + "\"");
			String lclPIString = lclPathInfo.substring(1);
			int lclIndex = lclPIString.indexOf('/');
			final String lclIdString;
			final String lclChildren;
			if (lclIndex == -1) {
				lclChildren = null;
				lclIdString = lclPIString;
			} else {
				lclChildren = lclPIString.substring(lclIndex + 1); // THINK: What about a trailing /?
				lclIdString = lclPIString.substring(0, lclIndex);
			}
			ourLogger.warn("lclIdString = \"" + lclIdString + "\", lclChildren = \"" + lclChildren + "\"");
			U lclUF = getFactory().forUniqueString(lclIdString);
			ourLogger.warn("lclUF = " + lclUF);
			if (lclUF != null) {
				return Pair.of(lclUF, lclChildren);
			} else {
				throw new RestResultException(RestResult.notFound("Could not find entity \"" + lclIdString + "\""));
			}
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest argRequest, HttpServletResponse argResponse) /* throws ServletException, IOException */ {
		RestResult lclRR;
		
		if (isGetAllowed() == false) {
			lclRR = RestResult.methodNotAllowed(); 
		} else {
			try {
				
				A lclCredential = getAuthenticator().getCredential(argRequest);
				
				Pair<U, String> lclPair = getEntityFromPathInfo(argRequest, isSearchGetAllowed() == false);
				U lclUF;
				String lclChildren;
				if (lclPair != null) {
					lclUF = lclPair.getLeft();
					lclChildren = lclPair.getRight();
				} else {
					lclUF = null;
					lclChildren = null;
				}
				
				if (lclUF != null) {
					if (isEntityGetAllowed()) {
						ensureGetAccess(lclUF, lclCredential);
						if (lclChildren == null) {
							lclRR = processEntityGet(argRequest, lclCredential, lclUF);
						} else {
							// FIXME: We should have error-checking around whether this Restlet supports this kind of thing (or not)
							lclRR = processEntityChildGet(argRequest, lclChildren, lclCredential, lclUF);
						}
					} else {
						lclRR = RestResult.badRequest("Entity GET not allowed"); /* THINK: Is this the correct status code? */
					}
				} else {
					if (isSearchGetAllowed()) {
						lclRR = processSearchGet(argRequest, lclCredential);
					} else {
						lclRR = RestResult.badRequest("Search GET not allowed"); /* THINK: Is this the correct status code? */
					}
				}
			} catch (RuntimeException lclE) {
				ourLogger.error(lclE.getMessage(), lclE);
				lclRR = new RestResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, lclE);
			} catch (RestResultException lclE) {
				lclRR = lclE.getRestResult();
	//		} catch (Exception lclE) {
	//			ourLogger.error(lclE.getMessage(), lclE);
	//			throw lclE;
			}
		}
		sendResponse(argResponse, lclRR);
		return;
	}
	
	@SuppressWarnings("unused")
	protected RestResult processEntityGet(HttpServletRequest argRequest, A argCredential, U argUF) {
		Validate.notNull(argUF);
		
		return RestResult.ok(toJson(argUF));
	}
	
	@SuppressWarnings("unused")
	protected RestResult processEntityChildGet(HttpServletRequest argRequest, String argChildCollection, A argCredential, U argUF) throws RestResultException {
		Validate.notNull(argUF);
		throw new RestResultException(RestResult.internalError("Default implementation of processEntityChildGet invoked",  null));
	}
	
	protected RestResult processSearchGet(HttpServletRequest argRequest, A argCredential) throws RestResultException {
		Collection<U> lclUFs;
		lclUFs = executeSearch(argRequest, argCredential);
			
		if (lclUFs == null) {
			ourLogger.warn("executeSearch returned null");
			lclUFs = Collections.emptyList();
		}
		JsonArray lclJA = new JsonArray();
		for (U lclUF : lclUFs) {
			lclJA.add(toJson(lclUF));
		}
		JsonObject lclJO = new JsonObject();
		lclJO.add(SEARCH_RESULT_KEY, lclJA);
		return RestResult.ok(lclJO);
	}
	
	@SuppressWarnings("unused")
	protected Collection<U> executeSearch(HttpServletRequest argRequest, A argCredential) throws RestResultException {
		throw new RestResultException(RestResult.internalError("Default implementation of executeSearch invoked", null));
	}
	
	@Override
	protected void doPut(HttpServletRequest argRequest, HttpServletResponse argResponse) /* throws ServletException, IOException */ {
		RestResult lclRR;
		
		if (isPutAllowed() == false) {
			lclRR = RestResult.methodNotAllowed(); 
		} else {
			try {
				A lclCredential = getAuthenticator().getCredential(argRequest);
	
				Pair<U, String> lclPI = getEntityFromPathInfo(argRequest, true);
				U lclUF = lclPI.getLeft(); // THINK: Is it allowed to pass a child collection name?  Throw an exception if it isn't and we do.
	
				if (lclUF != null || mayPutNewObject()) {
					ensurePutAccess(lclUF, lclCredential);
					JsonObject lclRequestJson = requestBodyToJson(argRequest).getAsJsonObject(); // THINK: What if it's not a JsonObject but a JsonXXX?
					
					try (TransactionContext lclTC = TransactionContext.createAndActivate()) { // THINK: Virtual method for time out?
						lclRR = processPut(argRequest, lclRequestJson, lclCredential, lclUF);
						lclTC.complete();
					}
				} else {
					lclRR = null;
					// FIXME: What should we do here?
				}
			} catch (RuntimeException lclE) {
				ourLogger.error(lclE.getMessage(), lclE);
				lclRR = RestResult.internalError(lclE);
			} catch (IOException lclE) { // Could be thrown by parsing the request
				ourLogger.error(lclE.getMessage(), lclE);
				lclRR = RestResult.internalError("Could not read body for parsing into Json", lclE);
			} catch (RestResultException lclE) {
				lclRR = Validate.notNull(lclE.getRestResult());
	//		} catch (Exception lclE) {
	//			ourLogger.error(lclE.getMessage(), lclE);
	//			throw lclE;
			}
		}
		Validate.notNull(lclRR);
		sendResponse(argResponse, lclRR);
		return;
	}
	
	protected boolean mayPutNewObject() {
		return false;
	}
	
	@SuppressWarnings("unused")
	protected RestResult processPut(HttpServletRequest argRequest, JsonObject argRequestJson, A argCredential, U argUF) {
		return RestResult.methodNotAllowed(); 
	}
	
	@Override
	protected void doPost(HttpServletRequest argRequest, HttpServletResponse argResponse) /* throws ServletException, IOException */ {
		RestResult lclRR;
		
		if (isPostAllowed() == false) {
			lclRR = RestResult.methodNotAllowed(); 
		}
		try {
			A lclCredential = getAuthenticator().getCredential(argRequest);
			
			ensurePostAccess(lclCredential);

			JsonObject lclRequestJson = requestBodyToJson(argRequest).getAsJsonObject(); // THINK: What if it's not a JsonObject but a JsonXXX?

			/* It would make sense to create a TransactionContext here for the use of processPost(), but if we don't complete it within
			 * processPost(), then the method won't have access to the database-generated Id for new objects.  And it can't therefore
			 * include those in the response.  I'll have to think about how to properly structure this since it really *seems* like this
			 * superclass should handle those details.
			 */
//			try (TransactionContext lclTC = TransactionContext.createAndActivate()) { // THINK: Abstract method for time out?
				lclRR = processPost(argRequest, lclRequestJson, lclCredential);
//				lclTC.complete();
//			}
		} catch (RuntimeException lclE) {
			ourLogger.error(lclE.getMessage(), lclE);
			lclRR = RestResult.internalError(lclE);
		} catch (IOException lclE) { // Could be thrown by parsing the request
			ourLogger.error(lclE.getMessage(), lclE);
			lclRR = RestResult.internalError("Could not read body for parsing into Json", lclE);
		} catch (RestResultException lclE) {
			lclRR = lclE.getRestResult();
//		} catch (Exception lclE) {
//			ourLogger.error(lclE.getMessage(), lclE);
//			throw lclE;
		}
		sendResponse(argResponse, lclRR);
		return;
	}

	@SuppressWarnings("unused")
	protected RestResult processPost(HttpServletRequest argRequest, JsonObject argRequestJson, A argCredential) {
		return RestResult.methodNotAllowed();
	}

	@Override
	protected void doDelete(HttpServletRequest argRequest, HttpServletResponse argResponse) /* throws ServletException, IOException */ {
		RestResult lclRR;
		
		if (isDeleteAllowed() == false) {
			lclRR = RestResult.methodNotAllowed(); 
		}
		try {
			A lclCredential = getAuthenticator().getCredential(argRequest);

			Pair<U, String> lclPI = getEntityFromPathInfo(argRequest, true);
			U lclUF = lclPI.getLeft(); // FIXME: Is it allowed to pass a child collection here?  If not, detect it.
						
			if (lclUF != null) {
				ensureDeleteAccess(lclUF, lclCredential);
				lclRR = processDelete(argRequest, lclCredential, lclUF);
			} else {
				/* THINK: What do we do here? */
				lclRR = null;
			}
		} catch (RuntimeException lclE) {
			ourLogger.error(lclE.getMessage(), lclE);
			lclRR = RestResult.internalError(lclE);
		} catch (RestResultException lclE) {
			lclRR = lclE.getRestResult();
//		} catch (Exception lclE) {
//			ourLogger.error(lclE.getMessage(), lclE);
//			throw lclE;
		}
		sendResponse(argResponse, lclRR);
		return;
	}

	@SuppressWarnings("unused")
	protected RestResult processDelete(HttpServletRequest argRequest, A argCredential, U argUF) {
		return RestResult.methodNotAllowed();
	}

	protected boolean requestBodyMustBeValidJson() {
		return false;
	}
	
	protected void sendResponse(HttpServletResponse argResponse, RestResult argRR) /* throws IOException */ {
		Validate.notNull(argRR);
		argResponse.setContentType(CONTENT_TYPE); // THINK: How is the JsonFilter supposed to work?
		int lclSC = argRR.getStatusCode();
		argResponse.setStatus(lclSC);
		JsonElement lclJE = argRR.getBody();
		if (lclJE == null) {
			if (lclSC >= 400) { // Error
				String lclErrorMessage = argRR.getErrorMessage();
				Throwable lclT = argRR.getThrowable();
				if (lclErrorMessage == null && lclT != null) {
					lclErrorMessage = lclT.getMessage();
				}
				if (lclErrorMessage == null) {
					lclErrorMessage = DEFAULT_ERROR_MESSAGE;
				}
				JsonObject lclJO = new JsonObject();
				lclJO.addProperty("error_message", lclErrorMessage);
				if (lclT != null) {
					lclJO.addProperty("exception_message", lclT.getMessage());
					// lclJO.addProperty("exception_stack_trace", lclT.getStackTrace()); // Do we want to do this?
				}
				lclJE = lclJO;
			} else {
				lclJE = JsonNull.INSTANCE;
			}
		}
		try {
			argResponse.getWriter().write(ourSerializer.toJson(lclJE));
			argResponse.getWriter().write('\n');
		} catch (IOException lclE) {
			ourLogger.error("Could not write to the HttpServletResponse in sendResponse(...)", lclE);
		}
	}
	
//	protected void sendError(final HttpServletResponse argResponse, final Exception argE) throws IOException {
//		Validate.notNull(argResponse);
//		Validate.notNull(argE);
//		
//		final JsonObject lclErrorAsJson = new JsonObject();
//		lclErrorAsJson.addProperty(
//			"error_message",
//			ObjectUtils.firstNonNull(argE.getMessage(), "Your request could not be processed") // THINK: Do we ever need to sanitize this?
//		);
//		
//		ourLogger.error(argE.getMessage(), argE);
//		
//		argResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//		
//		sendResponse(argResponse, lclErrorAsJson);
//	}
	
	protected JsonElement requestBodyToJson(final HttpServletRequest argRequest) throws JsonIOException, JsonSyntaxException, IOException {
		Validate.notNull(argRequest);
		
		try {
			return new JsonParser().parse(argRequest.getReader()); // THINK: Do we need to create a new JsonParser every time?
		} catch (JsonSyntaxException lclE) {
			if (requestBodyMustBeValidJson()) {
				throw lclE;
			} else {
				return null;
			}
		}
	}
	
	/* THINK: Should we do a default implementation that just dumps out the Opal fields? */
	@SuppressWarnings("unused")
	protected JsonObject toJson(U argUF) {
		throw new IllegalStateException("toJSON() called, but the default implementation has not been overwritten.");
	}
	
	protected static String getRequiredString(JsonObject argJO, String argPropertyName) throws RestResultException { // TODO: Move to as-yet-hypothetical superclass
		if (argJO == null) {
			throw new IllegalArgumentException("argJO is null");
		}
		if (argPropertyName == null) {
			throw new IllegalArgumentException("argPropertyName is null");
		}
		JsonElement lclJE = argJO.get(argPropertyName);
		if (lclJE == null) {
			throw new RestResultException(RestResult.badRequest("Missing " + argPropertyName));
		}
		if (lclJE.isJsonNull()) {
			throw new RestResultException(RestResult.badRequest("JS-Null " + argPropertyName));
		}
		String lclV = lclJE.getAsString();
		if (lclV == null) {
			throw new RestResultException(RestResult.badRequest("Java-Null " + argPropertyName));
		}
		if ("".equals(lclV)) {
			throw new RestResultException(RestResult.badRequest("Empty " + argPropertyName));
		}
		return lclV;
	}

	protected static String getOptionalString(JsonObject argJO, String argPropertyName) /* throws RestResultException */ { // TODO: Move to as-yet-hypothetical Move to superclass
		if (argJO == null) {
			throw new IllegalArgumentException("argJO is null");
		}
		if (argPropertyName == null) {
			throw new IllegalArgumentException("argPropertyName is null");
		}
		JsonElement lclJE = argJO.get(argPropertyName);
		if (lclJE == null) {
			return null; // CHECK: Does this happen?
		}
		if (lclJE.isJsonNull()) {
			return null; // CHECK: Does this happen?
		}
		String lclV = lclJE.getAsString();
		if (lclV == null) {
			return null; // CHECK: Does this happen?
		}
//		if ("".equals(lclV)) { // THINK: Do we want an option to preclude this?
//			throw new RestResultException(RestResult.badRequest("Empty " + argPropertyName));
//		}		
		return lclV;
	}
	
}
