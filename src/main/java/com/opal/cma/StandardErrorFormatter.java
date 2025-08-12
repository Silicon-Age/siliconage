package com.opal.cma;

/**
 * @author topquark
 */
public class StandardErrorFormatter extends AbstractErrorFormatter {
	private static final StandardErrorFormatter ourInstance = new StandardErrorFormatter();
	
	public static StandardErrorFormatter getInstance() {
		return ourInstance;
	}
	
	protected StandardErrorFormatter() {
		super();
	}
	
	@Override
	protected void appendListStartHTML(StringBuilder argSB) {
		argSB.append("<div id=\"cma-error\" class=\"cma-error-text\">");
	}
	
	@Override
	protected void appendItemStartHTML(StringBuilder argSB, String argS) {
		argSB.append("");
	}
	
	@Override
	protected void appendItemHTML(StringBuilder argSB, String argS) {
		argSB.append(argS);
	}
	
	@Override
	protected void appendItemEndHTML(StringBuilder argSB, String argS) {
		argSB.append("<br />");
	}
	
	@Override
	protected void appendListEndHTML(StringBuilder argSB) {
		argSB.append("</div>");
	}
}
