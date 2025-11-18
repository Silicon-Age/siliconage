package com.siliconage.web.exception;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public abstract class WebException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final Logger ourLogger = LoggerFactory.getLogger(WebException.class);
	
	private String myHeaderForUser;
	private String myMessageForUser;
	private String myHtmlMessageForUser;
//	private String myMessageForLog; // Ask Jonah about this?
	
	protected WebException(String argDefaultHeaderForUser, String argMessageForUser) {
		super(argMessageForUser);
		
		myHeaderForUser = argDefaultHeaderForUser;
		myMessageForUser = argMessageForUser;
	}
	
	public abstract int getHttpStatus();
	
	/**
	 * @param argHeader plain text
	 * @return this, for fluent construction
	 */
	public <E extends WebException> E withHeaderForUser(String argHeader) {
		myHeaderForUser = StringUtils.trimToNull(argHeader);
		return castThis();
	}
	
	/**
	 * @param argMessage plain text without any HTML entities, formatting, or anything like that.
	 * It may be displayed between {@code <p>...</p>} tags, but with no other processing.
	 * @return this, for fluent construction
	 */
	public <E extends WebException> E withMessageForUser(String argMessage) {
		myMessageForUser = StringUtils.trimToNull(argMessage);
		return castThis();
	}
	
	/**
	 * @param argHtmlMessage a block-level element or elements, e.g., {@code <p>Explanation of error.</p>} or {@code <p>Explanation of error.</p><p>Instructions to resolve it.</p>}
	 * If it is not provided, the plain-text message will be wrapped in {@code <p>...</p>} and used as the return value for {@link getHtmlMessageForUser()}.
	 * @return this, for fluent construction
	 */
	public <E extends WebException> E withHtmlMessageForUser(String argHtmlMessage) {
		myHtmlMessageForUser = StringUtils.trimToNull(argHtmlMessage);
		return castThis();
	}
	
	/**
	 * @param argHtmlMessage should be a block-level element or elements, e.g., {@code <p>Explanation of error.</p>} or {@code <p>Explanation of error.</p><p>Instructions to resolve it.</p>}
	 * If it is not provided, the plain-text message will be wrapped in {@code <p class="error">...</p>} and used as the return value for {@link getHtmlMessageForUser()}.
	 * @return this, for fluent construction
	 */
	public <E extends WebException> E withMessageForUser(String argPlainTextMessage, String argHtmlMessage) {
		myMessageForUser = StringUtils.trimToNull(argPlainTextMessage);
		myHtmlMessageForUser = StringUtils.trimToNull(argHtmlMessage);
		return castThis();
	}
	
	public <E extends WebException> E withMessageForLog(String argMessage, Level argLevel) {
		if (StringUtils.isNotBlank(argMessage)) {
			ourLogger.atLevel(ObjectUtils.firstNonNull(argLevel, Level.WARN))
				.log(argMessage, this);
		}
		return castThis();
	}
	
	public <E extends WebException> E withMessageForLog(String argMessage) {
		return withMessageForLog(argMessage, Level.WARN);
	}
	
	public String getHeaderForUser() {
		return myHeaderForUser;
	}
	
	public String getPlainTextMessageForUser() {
		return myMessageForUser;
	}
	
	public String getHtmlMessageForUser() {
		if (StringUtils.isBlank(myHtmlMessageForUser)) {
			return "<p class='error'>" + getPlainTextMessageForUser() + "</p>";
		} else {
			return myHtmlMessageForUser;
		}
	}
	
	@SuppressWarnings("unchecked")
	protected <E extends WebException> E castThis() {
		return (E) this;
	}
}
