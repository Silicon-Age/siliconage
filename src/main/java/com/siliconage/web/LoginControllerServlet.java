package com.siliconage.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public abstract class LoginControllerServlet extends ControllerServlet {
	private static final long serialVersionUID = 1L;
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(LoginControllerServlet.class.getName());
	
	protected abstract Object determineIdentity(HttpServletRequest argRequest);
	
	protected abstract String getDefaultSuccessURL(HttpServletRequest argRequest, Object argIdentity);
	
	protected abstract String getFailureURL(HttpServletRequest argRequest);
	
	protected String getSuccessURL(HttpServletRequest argRequest, Object argIdentity) {
		return getDefaultSuccessURL(argRequest, argIdentity);
	}
	
	@Override
	protected String processInternal(HttpServletRequest argRequest, HttpSession argSession, String argUsername) throws Exception {
		try {
			argSession.removeAttribute("IDENTITY");
			
			Object lclO = determineIdentity(argRequest);
			
			if (lclO == null) {
				return getFailureURL(argRequest);
			}
			
			argSession.setAttribute("IDENTITY", lclO);
			
			return getSuccessURL(argRequest, lclO);
		} catch (Exception lclE) {
			ourLogger.error(lclE.toString(), lclE);
			return getFailureURL(argRequest);
		}
	}
}
