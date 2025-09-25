package com.siliconage.web;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Supplier;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import com.siliconage.web.exception.WebException;
import com.siliconage.web.exception.AuthorizationException;
import com.siliconage.web.exception.BadRequestException;
import com.siliconage.web.exception.DataConflictException;
import com.siliconage.web.exception.MethodNotAllowedException;
import com.siliconage.web.exception.NotFoundException;
import com.siliconage.web.form.FormFieldRequirement;
import com.siliconage.web.form.PriorInput;

public abstract class ControllerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(LoginControllerServlet.class.getName());
	
	private static ThreadLocal<Map<String, Object>> ourPassBacks = new ThreadLocal<>();
	
	public static final String MESSAGES_PASSBACK_KEY = "MESSAGES";
	public static final String ERRORS_PASSBACK_KEY = "ERRORS";
	public static final String LOAD_TIME_KEY = "LOAD_TIME";
	public static final String DISABLED_FIELDS_PASSBACK_KEY = "DISABLED_FIELDS";
	public static final String INCORRECT_FIELDS_PASSBACK_KEY = "INCORRECT_FIELDS";
	public static final String REQUIRED_FIELDS_PASSBACK_KEY = "REQUIRED_FIELDS";
	public static final String CHECKED_NAME_VALUE_PAIRS_PASSBACK_KEY = "CHECKED_NAME_VALUE_PAIRS";
	
	/* If an entry with this key and any non-null value is added to the PassBack Map, then no PriorInput object
	 * will be be created for use on the page to which ControllerServlet forwards.  This is typically done
	 * when the form submission is "successful" so that the redisplay will use the values from the underlying
	 * model object (such as a UserFacing).  This clears out garbage but, more importantly, means there is a chance
	 * for the system to internally update fields on the object (such as add a default provided by the database)
	 * and have those non-user-supplied values immediately seen.
	 */
	public static final String DO_NOT_PASS_BACK_PRIOR_INPUT_KEY = "NO_PRIOR_INPUT";
	
	@SuppressWarnings("unused")
	protected void checkSecurity(HttpServletRequest argRequest) {
		return;
	}
	
	protected static void checkSecurity(boolean argOkay) throws AuthorizationException {
		requireOrThrow(argOkay, () -> new AuthorizationException());
	}
	
	public static <T, E extends WebException> T requireNonNullOrThrow(T argObject, Supplier<E> argExceptionSupplier) throws E {
		requireOrThrow(argObject != null, argExceptionSupplier);
		return argObject;
	}
	
	public static <E extends WebException> void requireOrThrow(boolean argCondition, Supplier<E> argExceptionSupplier) throws E {
		if (argCondition == false) {
			throw argExceptionSupplier.get();
		}
	}
	
	protected boolean allowGet() {
		return true;
	}
	
	protected boolean allowPost() {
		return true;
	}
	
	@Override
	public void doGet(HttpServletRequest argRequest, HttpServletResponse argResponse) throws ServletException, IOException {
		if (allowGet()) {
			process(argRequest, argResponse);
		} else {
			forbiddenMethod(argRequest, argResponse, "GET");
		}
	}
	
	@Override
	public void doPost(HttpServletRequest argRequest, HttpServletResponse argResponse) throws ServletException, IOException {
		if (allowPost()) {
			process(argRequest, argResponse);
		} else {
			forbiddenMethod(argRequest, argResponse, "POST");
		}
	}
	
	protected void forbiddenMethod(HttpServletRequest argRequest, HttpServletResponse argResponse, String argAttemptedMethod) throws IOException {
		String lclUsername = getUsername(argRequest);
		String lclUsernameExplanation = lclUsername == null ? "no user" : "username is '" + lclUsername + "'";
		
		ourLogger.warn(this.getClass().getName() + " does not allow " + argAttemptedMethod + "; " + lclUsernameExplanation + "; referer is " + argRequest.getHeader("referer"));
		
		argResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, argAttemptedMethod + " is not allowed");
	}
	
	/**
	 * Process incoming requests for information
	 * @param argRequest the request to the servlet 
	 * @param argResponse the response from the servlet
	 * @throws ServletException if any Exception is thrown internally
	 */
	public void process(HttpServletRequest argRequest, HttpServletResponse argResponse) throws ServletException {
		try {
			checkSecurity(argRequest);
			
			HttpSession lclSession = argRequest.getSession();
			
			String lclPassBack = StringUtils.trimToNull(argRequest.getParameter("PASSBACK"));
			ourLogger.info("lclPassBack = \"" + lclPassBack + "\"");
			initializePassBack(lclPassBack, lclSession);
			
//			Principal lclPrincipal = argRequest.getUserPrincipal();
			
			String lclUsername = getUsername(argRequest);
			
			String lclDestination = processInternal(argRequest, lclSession, lclUsername);
			Validate.notNull(lclDestination, "Not sure where to go next");
			
			if (lclPassBack != null) {
				if (getPassBack().get(DO_NOT_PASS_BACK_PRIOR_INPUT_KEY) == null) {
					passBackRequestParameters(argRequest);
				} else {
					getPassBack().put("REQUEST_MAP", null);
				}
			}
			
			argResponse.setStatus(HttpServletResponse.SC_SEE_OTHER);
			argResponse.setHeader("Location", argResponse.encodeRedirectURL(lclDestination));
		} catch (WebException lclE) {
			try {
				argRequest.setAttribute("com.siliconage.web.exception.WebException", lclE);
				argResponse.sendError(lclE.getHttpStatus(), lclE.getPlainTextMessageForUser());
			} catch (IOException lclE2) {
				ourLogger.error("Couldn't send error redirect", lclE2);
				throw new ServletException(lclE2);
			}
		} catch (Exception lclE) {
			ourLogger.error(lclE.toString(), lclE);
			throw new ServletException(lclE);
		} finally {
			ourPassBacks.set(null);
		}
	}
	
	protected static Map<String, Object> getPassBack() {
		Map<String, Object> lclMap = ourPassBacks.get();
		if (lclMap == null) {
			lclMap = new HashMap<>();
			ourPassBacks.set(lclMap);
			return lclMap;
		}
		return lclMap;
	}
	
	protected static void passBack(String argName, Object argValue) {
		ourLogger.info("Passing back \"" + argName + "\" as " + String.valueOf(argValue));
		getPassBack().put(argName, argValue);
	}
	
	protected static void passBack(String argName, int argValue) {
		passBack(argName, Integer.valueOf(argValue));
	}
	
	protected static void passBack(String argName, boolean argValue) {
		passBack(argName, Boolean.valueOf(argValue));
	}
	
	protected static void passBackMessage(String argMessage) {
		Map<String, Object> lclPassBack = getPassBack();
		Object lclO = lclPassBack.get(MESSAGES_PASSBACK_KEY);
		if (lclO == null || lclO instanceof List<?>) {
			
			@SuppressWarnings("unchecked")
			List<String> lclMessages = (List<String>) lclPassBack.get(MESSAGES_PASSBACK_KEY); // THINK: Why doesn't this use lclO?
			
			if (lclMessages == null) {
				lclPassBack.put(MESSAGES_PASSBACK_KEY, lclMessages = new ArrayList<>());
			}
			lclMessages.add(argMessage);
		} else {
			throw new IllegalStateException("lclO is not a List but " + lclO.getClass().getName() + ".");
		}
	}
	
	// // I believe this will be live.
	public static Map<String, Object> getPassBack(HttpSession argSession, String argPassBackCode) {
		@SuppressWarnings("unchecked")
		Map<String, Object> lclPassBack = (Map<String, Object>) argSession.getAttribute(argPassBackCode);
		if (lclPassBack == null) {
			initializePassBack(argPassBackCode, argSession);
			
			/* lclTemp exists solely to have something to which to attach the @SuppressWarnings annotation. */
			@SuppressWarnings("unchecked")
			Map<String, Object> lclTemp = (Map<String, Object>) argSession.getAttribute(argPassBackCode); 
			lclPassBack = lclTemp;
			
			Validate.notNull(lclPassBack, "Passback is null even after initialization");
			return lclPassBack;
		} else {
			return lclPassBack;
		}
	}
	
	// Never returns null.  Makes a defensive copy -- you're never getting a live list.
	protected static List<String> getPassedBackStringList(HttpSession argSession, String argPassBackKey, String argPassBackCode, boolean argRemoveFromSession) {
		if (argSession == null || argPassBackKey == null || argPassBackCode == null) {
			return Collections.emptyList();
		}
		
		Map<String, Object> lclPassBack = getPassBack(argSession, argPassBackCode);
		
		@SuppressWarnings("unchecked")
		List<String> lclList = (List<String>) lclPassBack.get(argPassBackKey);
		if (lclList == null) {
			return Collections.emptyList();
		} else {
			if (argRemoveFromSession) {
				lclPassBack.put(argPassBackKey, null);
			}
			
			return new ArrayList<>(lclList); // defensive copy
		}
	}
	
	protected static String makeHTML(List<String> argValues, String argCssClass) {
		if (argValues == null || argValues.isEmpty()) {
			return "";
		} else if (argValues.size() == 1) {
			String lclValue = argValues.get(0);
			
			if (StringUtils.isBlank(argCssClass)) {
				return "<p>" + String.valueOf(lclValue) + "</p>"; // THINK: Escape?
			} else {
				return "<p class=\"" + argCssClass + "\">" + lclValue + "</p>"; // THINK: Escape?
			}
		} else {
			StringBuilder lclSB = new StringBuilder(1024);
			lclSB.append("<ul");
			if (StringUtils.isNotBlank(argCssClass)) {
				lclSB.append(" class=\"").append(argCssClass).append('\"');
			}
			lclSB.append('>');
			for (String lclValue : argValues) {
				lclSB.append("<li>").append(lclValue).append("</li>"); // THINK: Escape?
			}
			lclSB.append("</ul>");
			return lclSB.toString();
		}
	}
	
	public static List<String> getMessages(HttpSession argSession, String argPassBackCode, boolean argRemoveFromSession) {
		return getPassedBackStringList(argSession, MESSAGES_PASSBACK_KEY, argPassBackCode, argRemoveFromSession);
	}
	
	public static List<String> getMessages(HttpSession argSession, String argPassBackCode) {
		return getMessages(argSession, argPassBackCode, false);
	}
	
	public static String generatePassBackMessageHTML(HttpSession argSession, String argPassBackCode, String argCssClass) {
		return generatePassBackMessageHTML(argSession, argPassBackCode, argCssClass, false);
	}
	
	public static String generatePassBackMessageHTML(HttpSession argSession, String argPassBackCode, String argCssClass, boolean argRemoveFromSession) {
		return makeHTML(getMessages(argSession, argPassBackCode, argRemoveFromSession), argCssClass);
	}
	
	public static List<String> getErrors(HttpSession argSession, String argPassBackCode, boolean argRemoveFromSession) {
		return getPassedBackStringList(argSession, ERRORS_PASSBACK_KEY, argPassBackCode, argRemoveFromSession);
	}
	
	public static List<String> getErrors(HttpSession argSession, String argPassBackCode) {
		return getErrors(argSession, argPassBackCode, false);
	}
	
	public static String generatePassBackErrorHTML(HttpSession argSession, String argPassBackCode, String argCssClass) {
		return generatePassBackErrorHTML(argSession, argPassBackCode, argCssClass, false);
	}
	
	public static String generatePassBackErrorHTML(HttpSession argSession, String argPassBackCode, String argCssClass, boolean argRemoveFromSession) {
		return makeHTML(getErrors(argSession, argPassBackCode, argRemoveFromSession), argCssClass);
	}
	
	// TODO: Reduce scope
	protected void passBackError(String argError) {
		Map<String, Object> lclPassBack = getPassBack();
		@SuppressWarnings("unchecked")
		List<String> lclErrors = (List<String>) lclPassBack.get(ERRORS_PASSBACK_KEY); // FIXME: What if it's not a List?
		if (lclErrors == null) {
			lclPassBack.put(ERRORS_PASSBACK_KEY, lclErrors = new ArrayList<>());
		}
		lclErrors.add(argError);
	}
	
	// TODO: Reduce scope
	protected void markField(String argFieldName) {
		Map<String, Object> lclPassBack = getPassBack();
		@SuppressWarnings("unchecked")
		Set<String> lclS = (Set<String>) lclPassBack.get(INCORRECT_FIELDS_PASSBACK_KEY);
		if (lclS == null) {
			lclPassBack.put(INCORRECT_FIELDS_PASSBACK_KEY, lclS = new HashSet<>());
		}
		lclS.add(argFieldName);
	}
	
	protected void passBackError(String argFieldName, String argError) {
		markField(argFieldName);
		passBackError(argError);
	}
	
	protected boolean passBackMessagesExist() {
		Map<String, Object> lclPassBack = getPassBack();
		@SuppressWarnings("unchecked")
		List<String> lclMessages = (List<String>) lclPassBack.get(MESSAGES_PASSBACK_KEY);
		return lclMessages != null && lclMessages.isEmpty() == false;
	}
	
	protected boolean passBackErrorsExist() {
		Map<String, Object> lclPassBack = getPassBack();
		@SuppressWarnings("unchecked")
		List<String> lclErrors = (List<String>) lclPassBack.get(ERRORS_PASSBACK_KEY);
		return lclErrors != null && lclErrors.isEmpty() == false;
	}
	
	protected static String getRequiredParameter(HttpServletRequest argRequest, String argParameter) {
		String lclString = StringUtils.trimToNull(argRequest.getParameter(argParameter));
		if (lclString == null) {
			throw new IllegalArgumentException("Required HTTP request parameter \"" + argParameter + "\" not found.");
		}
		return lclString;
	}
	
	public static String getOptionalParameter(HttpServletRequest argRequest, String argParameter, String argDefault) {
		String lclString = StringUtils.trimToNull(argRequest.getParameter(argParameter));
		return lclString == null ? argDefault : lclString;
	}
	
	public static String getParedParameter(HttpServletRequest argRequest, String argParameter) {
		return StringUtils.trimToNull(argRequest.getParameter(argParameter));
	}
	
	public static int getRequiredIntParameter(HttpServletRequest argRequest, String argParameter) throws BadRequestException {
		return HTMLUtility.getIntParameter(argRequest, argParameter);
	}
	
	public static int getOptionalIntParameter(HttpServletRequest argRequest, String argParameter, int argDefault) {
		return HTMLUtility.getOptionalIntParameter(argRequest, argParameter, argDefault);
	}
	
	public static boolean getBooleanParameter(HttpServletRequest argRequest, String argParameter) {
		return HTMLUtility.getBooleanParameter(argRequest, argParameter);
	}
	
	public static boolean getOptionalBooleanParameter(HttpServletRequest argRequest, String argParameter, boolean argDefault) {
		String lclS = StringUtils.trimToNull(argRequest.getParameter(argParameter));
		if (lclS == null) {
			return argDefault;
		}
		return getBooleanParameter(argRequest, argParameter);
	}
	
	@SuppressWarnings("unchecked")
	public static void initializePassBack(String argPassbackCode, HttpSession argSession) {
		if (argSession == null) {
			throw new IllegalArgumentException("argSession is null");
		}
		if (argPassbackCode != null) {
			Object lclO = argSession.getAttribute(argPassbackCode);
			Map<String, Object> lclMap;
			if (lclO == null || !(lclO instanceof Map<?, ?>)) {
				// ourLogger.info("Initializing a new passback map for " + argPassbackCode);
				argSession.setAttribute(argPassbackCode, lclMap = new HashMap<>());
			} else {
				// ourLogger.info("Initializing an existing passback map for " + argPassbackCode);
				lclMap = (Map<String, Object>) lclO;
				
				lclMap.clear();
				// lclMap.put("MESSAGES", null);
				// lclMap.put("ERRORS", null);
			}
			ourPassBacks.set(lclMap);
		}
	}
	
	@SuppressWarnings("cast")
	protected void passBackRequestParameters(HttpServletRequest argRequest) {
		if (argRequest == null) {
			throw new IllegalArgumentException("argRequest is null");
		}
		Map<String, String[]> lclMap = (Map<String, String[]>) argRequest.getParameterMap();
		Map<String, String[]> lclCopy = new HashMap<>(lclMap);
//		lclCopy.putAll(lclMap);
		// ourLogger.info("Putting " + lclCopy.size() + " request parameters in PassBack object");
		// ourLogger.info("Filling REQUEST_MAP with " + lclCopy.size() + " request parameters.");
		getPassBack().put("REQUEST_MAP", lclCopy);
	}
	
	protected static void passBack(HttpServletRequest argRequest, String argKey) {
		if (argRequest == null) {
			throw new IllegalArgumentException("argRequest is null");
		}
		if (argKey == null) {
			throw new IllegalArgumentException("argKey is null");
		}
		getPassBack().put(argKey, argRequest.getParameter(argKey));
	}
	
	public static PriorInput getPriorInput(HttpSession argSession, String argPassBackCode) {
		if (argSession == null) {
			throw new IllegalArgumentException("argSession is null");
		}
		if (argPassBackCode == null) {
			throw new IllegalArgumentException("argPassBackCode is null");
		}
		
		Map<String, Object> lclPassBack = getPassBack(argSession, argPassBackCode);
		
		if (lclPassBack == null) {
			return new PriorInput(); // No data in this PriorInput
		}
		
		@SuppressWarnings("unchecked")
		Map<String, String[]> lclRequestMap = (Map<String, String[]>) lclPassBack.get("REQUEST_MAP");
		
		long lclLoadTime = System.currentTimeMillis();
		Object lclLoadTimeObj = lclPassBack.get(LOAD_TIME_KEY);
		if (lclLoadTimeObj != null) {
			try {
				lclLoadTime = Long.parseLong(StringUtils.trimToNull(lclLoadTimeObj.toString()));
			} catch (NumberFormatException lclE) {
				ourLogger.warn("Could not parse '" + lclLoadTime + "' as a long");
			}
		}
		
		@SuppressWarnings("unchecked")
		Set<String> lclDisabledFields = (Set<String>) lclPassBack.get(DISABLED_FIELDS_PASSBACK_KEY);
		
		@SuppressWarnings("unchecked")
		Set<String> lclIncorrectFields = (Set<String>) lclPassBack.get(INCORRECT_FIELDS_PASSBACK_KEY);
		
		@SuppressWarnings("unchecked")
		Map<String, FormFieldRequirement> lclFieldRequirements = (Map<String, FormFieldRequirement>) lclPassBack.get(REQUIRED_FIELDS_PASSBACK_KEY);
		
		@SuppressWarnings("unchecked")
		Set<Pair<String, String>> lclCheckedNameValuePairs = (Set<Pair<String, String>>) lclPassBack.get(CHECKED_NAME_VALUE_PAIRS_PASSBACK_KEY);
		
		return new PriorInput(convertArrayValueMultimapToCollectionValueMultimap(lclRequestMap), lclLoadTime, lclDisabledFields, lclIncorrectFields, lclFieldRequirements, lclCheckedNameValuePairs);
	}

	/* This should be used when a JSP page (like a search page) is posting back to itself; that is, when there is no
	 * ControllerServlet involved in the chain that has a chance to copy the HttpRequest parameters into the
	 * HttpSession.
	 */
	public static PriorInput getPriorInput(HttpServletRequest argRequest, String argPassBackCode) {
		/* If we have no request at all, then there are no prior inputs to fill in input fields. */
		if (argRequest == null) {
			ourLogger.info("No request so null PriorInput.");
			return new PriorInput(); // No data in this PriorInput
		}
		/* If the request we're given is from a different page, then we don't want to use its
		 * prior inputs to fill in input fields.
		 */
		String lclRequestPassBack = StringUtils.trimToNull(argRequest.getParameter("PASSBACK"));
		// ourLogger.info("lclRequestPassBack = \"" + lclRequestPassBack + "\"");
		if (lclRequestPassBack == null || lclRequestPassBack.equals(argPassBackCode) == false) {
			// ourLogger.info("Null or didn't match, so null PriorInput.");
			return new PriorInput();
		}
		assert lclRequestPassBack.equals(argPassBackCode);
		/* If it was from this page, then we do want to use its prior inputs to fill in input fields. */
		// ourLogger.info("Returning a real PriorInput.");
		
		return new PriorInput(convertArrayValueMultimapToCollectionValueMultimap(argRequest.getParameterMap()));
	}

	/* This should be used when a JSP page can either post back to itself or go through a ControllerServlet. argSession dominates. */
	public static PriorInput getPriorInput(HttpSession argSession, HttpServletRequest argRequest, String argPassBackCode) {
		if (argSession == null && argRequest == null) {
			return new PriorInput();
		} else if (argPassBackCode == null) {
			return new PriorInput();
		} else {
			PriorInput lclPI = getPriorInput(argSession, argPassBackCode);
			if (lclPI == null || lclPI.containsRequestData() == false) {
				return getPriorInput(argRequest, argPassBackCode); // which still may be useless
			} else {
				return lclPI;
			}
		}
	}
	
	public static String generatePassBack(String argPassBackCode) {
		if (argPassBackCode == null) {
			throw new IllegalArgumentException("argPassBackCode is null");
		}
		return "<input type=\"hidden\" name=\"PASSBACK\" value=\"" + argPassBackCode + "\" />";
	}
	
	protected abstract String processInternal(HttpServletRequest argRequest, HttpSession argSession, String argUsername) throws Exception;
	
	public static void finishProcess() {
		passBack(DO_NOT_PASS_BACK_PRIOR_INPUT_KEY, true);
	}
	
	public static <K, V> Map<K, Collection<V>> convertArrayValueMultimapToCollectionValueMultimap(Map<K, V[]> argInput) {
		if (argInput == null) {
			return null;
		} else {
			Map<K, Collection<V>> lclOutput = new HashMap<>(argInput.size());
			
			for (Map.Entry<K, V[]> lclEntry : argInput.entrySet()) {
				K lclKey = lclEntry.getKey();
				V[] lclValuesArray = lclEntry.getValue();
				Collection<V> lclValues;
				
				if (lclValuesArray == null || lclValuesArray.length == 0) {
					lclValues = new ArrayList<>(); // Not Collections.emptyList() so it will be modifiable
				} else {
					lclValues = new ArrayList<>(Arrays.asList(lclValuesArray)); // Copy so it will be modifiable
				}
				
				// ourLogger.debug(String.valueOf(lclKey) + " -> " + lclValues.size() + " values" + (lclValues.size() == 1 ? " (\"" + String.valueOf(lclValuesArray[0]) + "\")" : ""));
				
				lclOutput.put(lclKey, lclValues);
			}
			
			return lclOutput;
		}
	}
	
	protected static String getUsername(HttpServletRequest argRequest) {
		if (argRequest == null) { // ???
			return null;
		} else {
			Principal lclPrincipal = argRequest.getUserPrincipal();
			return lclPrincipal == null ? null : lclPrincipal.getName();
		}
	}
}
