package com.opal.cma;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.stream.Collectors;
import javax.naming.InitialContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import com.siliconage.util.NetworkUtils;
import com.siliconage.web.ControllerServlet;

import com.opal.TransactionContext;

public class OpalFormController extends ControllerServlet {
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(OpalFormController.class.getName());
	public static final String ACTION_KEY = "OPAL_FORM_ACTION";
	public static final String SUCCESS_KEY = "OPAL_FORM_SUCCESS";
	
	private static final long serialVersionUID = 1L;
	
	@Override
	protected boolean allowGet() {
		return false;
	}
	
	@Override
	protected String processInternal(HttpServletRequest argRequest, HttpSession argSession, String argUsername) throws Exception {
		boolean lclPassedSecurityCheck = checkSecurityDigest(argRequest);
		if (lclPassedSecurityCheck == false) {
			List<Pair<String, String>> lclParameterList = getParameterList(argRequest);
			String lclRequestURL = argRequest.getRequestURL().toString();
			ourLogger.warn(
				"Failed security check on OpalForm.\n" +
				"IP: " + NetworkUtils.getClientIpAddress(argRequest) + '\n' +
				"Referer: " + argRequest.getHeader("referer") + '\n' + 
				"Request: " + lclRequestURL + '\n' +
				"Query string: " + argRequest.getQueryString() + '\n' +
				"Parameter map: " + lclParameterList.toString()
			);
			
			// This should really result in an error status code, namely HTTP 400 Bad Request. Our current framework doesn't really allow that, but we should change it so it does.
			throw new IllegalArgumentException("Security check failed");
		}
		
		
		String lclUniqueStringParameterName = argRequest.getParameter(OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "UniqueStringParameterName");
		if (lclUniqueStringParameterName == null) {
			List<Pair<String, String>> lclParameterList = getParameterList(argRequest);
			String lclRequestURL = argRequest.getRequestURL().toString();
			ourLogger.warn(
				"Missing /UniqueStringParameterName in OpalForm.\n" +
				"IP: " + NetworkUtils.getClientIpAddress(argRequest) + '\n' +
				"Referer: " + argRequest.getHeader("referer") + '\n' + 
				"Request: " + lclRequestURL + '\n' +
				"Query string: " + argRequest.getQueryString() + '\n' +
				"Parameter map: " + lclParameterList.toString()
			);
			passBackError("The form you just submitted had an internal technical error.  Please reset the form and try again.  If the problem persists, contact the webmaster.");
			
			return lclRequestURL; // THINK: Is that any good?
		} else {
			OpalFormUpdater<?> lclOFU = OpalFormUpdater.createUpdater(argRequest, "", lclUniqueStringParameterName);
			
			Principal lclPrincipal = argRequest.getUserPrincipal();
			String lclUsername = lclPrincipal == null ? null : StringUtils.trimToNull(lclPrincipal.getName());
			lclOFU.setUsername(lclUsername);
			
			OpalFormAction lclAction = lclOFU.determineAction();
			
			passBack(ACTION_KEY, lclAction);
			
			switch (lclAction) {
				case CANCEL:
					passBack(DO_NOT_PASS_BACK_PRIOR_INPUT_KEY, "TRUE");
					
					// THINK: Does this really make sense?  Is the notion of success just inapplicable for cancellation?
					passBack(SUCCESS_KEY, true);
					
					finishProcess();
					
					return lclOFU.generateCancelURI();
				case DELETE:
					try (TransactionContext lclTC = TransactionContext.createAndActivate(lclOFU.determineDeletionTimeout())) {
						lclOFU.delete();
						if (lclOFU.hasErrors()) {
							lclTC.rollback();
							for (String lclE : lclOFU.getErrors()) {
								passBackError(lclE);
							}
							passBack(SUCCESS_KEY, false);
							return lclOFU.generateFailureURI();
						} else {
							lclTC.complete();
							lclOFU.afterDeleteCommit();
							passBack(DO_NOT_PASS_BACK_PRIOR_INPUT_KEY, "TRUE");
							passBack(SUCCESS_KEY, true);
							finishProcess();
							return lclOFU.generateDeleteURI();
						}
					}
				case SUBMIT:
				case CONTINUE:
					try (TransactionContext lclTC = TransactionContext.createAndActivate(lclOFU.determineUpdateTimeout())) {
						lclOFU.update();
						// ourLogger.info("OpalFormController: just returned from update().  hasErrors == " + lclOFU.hasErrors());
						if (lclOFU.hasErrors()) {
							// ourLogger.info("OpalFormController:  rolling back");
							lclTC.rollback();
							
							for (String lclE : lclOFU.getErrors()) {
								passBackError(lclE);
							}
							// ourLogger.info("Incorrect Field Count: " + lclOFU.getIncorrectFields().size());
							
							String lclLoadTime = StringUtils.trimToNull(argRequest.getParameter(OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "LoadTime"));
							
							passBack(ControllerServlet.LOAD_TIME_KEY, lclLoadTime);
							passBack(ControllerServlet.INCORRECT_FIELDS_PASSBACK_KEY, lclOFU.getIncorrectFields());
							passBack(ControllerServlet.CHECKED_NAME_VALUE_PAIRS_PASSBACK_KEY, lclOFU.getCheckedNameValuePairs());
							
							passBack(SUCCESS_KEY, false);
							return lclOFU.generateFailureURI();
						} else {
							// ourLogger.info("OpalFormController:  completing");
							lclTC.complete();
							lclOFU.afterCommit();
							lclOFU.runChildrenAfterCommits();
							
							// Update the update-timestamp records for everything
							OpalFormUpdateTimes lclCache = OpalFormUpdateTimes.getInstance();
							Queue<OpalFormUpdater<?>> lclUpdaters = new ArrayDeque<>();
							lclUpdaters.add(lclOFU);
							while (!lclUpdaters.isEmpty()) {
								OpalFormUpdater<?> lclU = lclUpdaters.remove();
								if (lclOFU.getUserFacing() != null && !lclOFU.getUserFacing().isDeleted()) {
									lclCache.setUpdatedNow(lclOFU.getUserFacing());
								}
								lclUpdaters.addAll(lclU.getChildUpdaters());
							}
							
							/* We want to clear the PriorInput if the submission was successful. */
							passBack(DO_NOT_PASS_BACK_PRIOR_INPUT_KEY, "TRUE");
							
							String lclNewURI;
							if (lclAction == OpalFormAction.SUBMIT) {
								lclOFU.successfulSubmit();
								lclNewURI = lclOFU.generateSuccessURI();
							} else {
								Validate.isTrue(lclAction == OpalFormAction.CONTINUE);
								lclOFU.successfulContinue();
								lclNewURI = lclOFU.generateContinueURI();
							}
							finishProcess();
							passBack(SUCCESS_KEY, true);
							return lclNewURI;
						}
					}
				// TODO: Custom actions -- how should they work?
				default:
					throw new IllegalStateException("Unknown action");
			}
		}
	}
	
	private boolean checkSecurityDigest(HttpServletRequest argRequest) {
		String lclDynamicSalt = argRequest.getParameter(OpalFormSecurityUtil.DYNAMIC_SALT_FIELD_NAME);
		if (StringUtils.isBlank(lclDynamicSalt)) {
			return false;
		}
		
		String lclProvidedDigest = argRequest.getParameter(OpalFormSecurityUtil.DIGEST_FIELD_NAME);
		if (StringUtils.isBlank(lclProvidedDigest)) {
			return false;
		}
		
		List<String> lclDisplayed = new ArrayList<>();
		for (Map.Entry<String, String[]> lclEntry : argRequest.getParameterMap().entrySet()) {
			if (lclEntry.getKey().endsWith(OpalForm.FULLY_QUALIFIED_NAME_SEPARATOR + "Displayed")) { // Is there a better way to do this, making sure we get the descendant forms' displayed fields too?
				lclDisplayed.addAll(Arrays.asList(lclEntry.getValue()));
			}
		}
		
		String lclExpectedDigest = OpalFormSecurityUtil.generateDigest(lclDisplayed, lclDynamicSalt);
		return lclProvidedDigest.equals(lclExpectedDigest);
	}
	
	private List<Pair<String, String>> getParameterList(HttpServletRequest argRequest) {
		return argRequest.getParameterMap().entrySet().stream()
			.map(argEntry -> Pair.of(argEntry.getKey(), Arrays.toString(argEntry.getValue())))
			.toList();
	}
}
