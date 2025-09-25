package com.siliconage.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import com.siliconage.web.exception.BadRequestException;

/**
 * @author topquark
 */
public abstract class HTMLUtility {
	public static final String DEFAULT_TRUE_STRING = String.valueOf(true);
	public static final String DEFAULT_FALSE_STRING = String.valueOf(false);
	
	public static String makeHTMLEmailLink(String argText, String argEmailAddress) {
		return makeHTMLEmailLink(argText, argEmailAddress, null);
	}
	
	public static String makeHTMLEmailLink(String argText, String argEmailAddress, String argClass) {
		if (argEmailAddress == null) {
			return argText;
		} else {
			return makeHTMLEmailLink(argText, "mailto:" + argEmailAddress, argClass);
		}
	}

	public static String makeHTMLLink(String argURL, String argText) {
		return makeHTMLLink(argURL, argText, null);
	}
	
	public static String makeHTMLLink(String argURL, String argText, String argClass) {
		if (argText == null) {
			throw new IllegalArgumentException("argText is null");
		}
		if (argURL == null) {
			return argText;
		}
		StringBuilder lclSB = new StringBuilder(argURL.length() + argText.length() + 16);
		lclSB.append("<a");
		if (argClass != null) {
			lclSB.append(" class=\"");
			lclSB.append(argClass);
			lclSB.append('\"');
		}
		lclSB.append(" href=\"");
		lclSB.append(argURL);
		lclSB.append("\">");
		lclSB.append(argText);
		lclSB.append("</a>");
		return lclSB.toString();
	}
	
	public static int getIntParameter(HttpServletRequest argRequest, String argParameter) throws BadRequestException {
		String lclString = StringUtils.trimToNull(argRequest.getParameter(argParameter));
		if (lclString == null) {
			throw new BadRequestException(argParameter + " is required.");
		}
		try {
			return Integer.parseInt(lclString);
		} catch (NumberFormatException lclE) {
			throw new BadRequestException("We couldn't understand what you provided for " + argParameter + " (which was \"" + lclString + "\").");
		}
	}
	
	public static int getOptionalIntParameter(HttpServletRequest argRequest, String argParameter, int argDefault) {
		String lclString = StringUtils.trimToNull(argRequest.getParameter(argParameter));
		if (lclString == null) {
			return argDefault;
		}
		try {
			return Integer.parseInt(lclString);
		} catch (NumberFormatException lclE) {
			return argDefault;
		}
	}
	
	public static Integer getOptionalIntParameter(HttpServletRequest argRequest, String argParameter) {
		String lclString = StringUtils.trimToNull(argRequest.getParameter(argParameter));
		if (lclString == null) {
			return null;
		}
		try {
			return Integer.parseInt(lclString);
		} catch (NumberFormatException lclE) {
			return null;
		}
	}
	
	/**
	 * @param argRequest the request from which to extract a parameter and convert it into a boolean
	 * @param argParameterName the name of the parameter to extract from {@code argRequest}
	 * @return {@code true} if that parameter is present and {@code stringToBoolean} returns {@code true} for its value; returns {@code false} otherwise.
	 */
	public static boolean getBooleanParameter(HttpServletRequest argRequest, String argParameterName) {
		String lclString = StringUtils.trimToNull(argRequest.getParameter(argParameterName));
		
		/* An unchecked checkbox will not show up in the request, so if it's 
		 * not in the request, it's treated as false. */
		if (lclString == null) {
			return false;
		}
		
		return stringToBoolean(lclString);
	}
	
	/**
	 * @param argString the string to evaluate the truth of
	 * @return {@code true} if {@code Boolean.parseBoolean} returns {@code true} for the trimmed {@code argString}, or the trimmed string is case-insensitively equal to {@code "yes"}, or the trimmed string is equal to {@code "1"}; returns {@code false} otherwise.
	 */
	public static boolean stringToBoolean(String argString) {
		String lclS = StringUtils.trimToNull(argString);
		
		if (lclS == null) {
			return false;
		} else {
			return Boolean.parseBoolean(lclS) || "YES".equalsIgnoreCase(lclS) || "1".equals(lclS);
		}
	}
	
	
	/**
	 * @param argRequest the request from which to extract a parameter and convert it into a boolean
	 * @param argParameterName the name of the parameter to extract from {@code argRequest}
	 * @param argDefault the value to return if {@code argRequest} contains no parameter whose name
	 * is the value of {@code argParameterName}
	 * @return {@code argDefault} if {@code argRequest} contains no parameter whose name is the value of {@code argParameterName}. If that parameter is present, returns {@code true} if that parameter is present and {@code stringToBoolean} returns {@code true} for its value; returns {@code false} otherwise.
	 */
	public static boolean getOptionalBooleanParameter(HttpServletRequest argRequest, String argParameterName, boolean argDefault) {
		String lclString = StringUtils.trimToNull(argRequest.getParameter(argParameterName));
		
		if (lclString == null) {
			return argDefault;
		}
		
		return stringToBoolean(lclString);
	}
	
	public static String getPassBack(HttpSession argSession, String argPassBack, String argName) {
		if (argSession == null) {
			throw new IllegalArgumentException("argSession is null");
		}
		if (argPassBack == null) {
			throw new IllegalArgumentException("argPassBack is null");
		}
		if (argName == null) {
			throw new IllegalArgumentException("argName is null");
		}
		Object lclO = argSession.getAttribute(argPassBack);
		if (lclO == null || !(lclO instanceof Map)) {
			return null;
		}
		Map<Object, Object> lclMap = convertObjectToMap(lclO);
		
		Object lclO2 = lclMap.get(argName);
		
		return lclO2 == null ? null : lclO2.toString();
	}
	
	@SuppressWarnings("unchecked")
	private static Map<Object, Object> convertObjectToMap(Object argO) {
		return (Map<Object, Object>) argO;
	}
	
	public static String percentEncode(String argS) {
		if (argS == null) {
			return null;
		}
		
		try {
			return URLEncoder.encode(argS, "UTF-8");
		} catch (UnsupportedEncodingException lclE) {
			// this should never happen
			return argS;
		}
	}
	
	public static String percentDecode(String argS) {
		if (argS == null) {
			return null;
		}
		
		try {
			return URLDecoder.decode(argS, "UTF-8");
		} catch (UnsupportedEncodingException lclE) {
			// this should never happen
			return argS;
		}
	}
}
