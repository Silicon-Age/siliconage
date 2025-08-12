package com.opal.cma;

import java.util.Collection;

/**
 * @author topquark
 */
public abstract class AbstractErrorFormatter implements ErrorFormatter {
	public AbstractErrorFormatter() {
		super();
	}
	
	@Override
	public String format(Collection<String> argC) {
		if (argC == null) {
			return generateNullResponse();
		}
		if (argC.size() == 0) {
			return generateEmptyResponse();
		}
		return formatInternal(argC);
	}
	
	public String formatInternal(Collection<String> argC) {
		StringBuilder lclSB = new StringBuilder(256);
		appendListStartHTML(lclSB);
		for(String lclS : argC) {
			appendItemStartHTML(lclSB, lclS);
			appendItemHTML(lclSB, lclS);
			appendItemEndHTML(lclSB, lclS);
		}
		appendListEndHTML(lclSB);
		return lclSB.toString();
	}
	
	protected abstract void appendListStartHTML(StringBuilder argSB);
	
	protected abstract void appendItemStartHTML(StringBuilder argSB, String argS);
	
	protected abstract void appendItemHTML(StringBuilder argSB, String argS);
	
	protected abstract void appendItemEndHTML(StringBuilder argSB, String argS);
	
	protected abstract void appendListEndHTML(StringBuilder argSB);
	
	protected String generateNullResponse() {
		return "";
	}
	
	protected String generateEmptyResponse() {
		return "";
	}
}
