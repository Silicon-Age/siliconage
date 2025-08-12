package com.opal.cma;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.siliconage.util.Trinary;

public class OpalFormConfiguration {
	private static final OpalFormConfiguration ourInstance = new OpalFormConfiguration();
	
	protected OpalFormConfiguration() {
		super();
	}
	
	public static OpalFormConfiguration getInstance() {
		return ourInstance;
	}
	
	public String beforeFormTagOpen() {
		return "";
	}
	
	public String afterOpen() {
		return "";
	}
	
	public String beforeClose() {
		return "";
	}
	
	public String afterCloseBeforeScript() {
		return "";
	}
	
	public String afterCloseScript() {
		return "";
	}
	
	
	public String getDefaultInsertButtonLabel() {
		return "Create";
	}
	
	public String getDefaultInsertButtonCssClass() {
		return "insert";
	}
	
	public String getDefaultUpdateButtonLabel() {
		return "Update";
	}
	
	public String getDefaultUpdateButtonCssClass() {
		return "update";
	}
	
	public String getDefaultCancelButtonLabel() {
		return "Reset";
	}
	
	public String getDefaultCancelButtonCssClass() {
		return "reset";
	}
	
	public String getDefaultDeleteButtonLabel() {
		return "Delete";
	}
	
	public String getDefaultDeleteButtonCssClass() {
		return "delete";
	}
	
	public String getDefaultContinueButtonLabel() {
		return "Continue";
	}
	
	public String getDefaultContinueButtonCssClass() {
		return "continue";
	}
	
	public String getDefaultSubformDeleteLabel() {
		return "Delete";
	}
	
	public String getDefaultSubformUndeleteLabel() {
		return "Undelete";
	}
	
	public String getSubformDeleteCssClass() {
		return "of-subform-delete";
	}
	
	public String getErrorCssClass() {
		return "passback errors of-errors"; // That's opalform-errors, not the Hebrew "vanity of vanities" construction
	}
	
	public String formatErrors(List<String> argErrors) {
		return formatDisplayable(argErrors, getErrorCssClass());
	}
	
	public String getMessageCssClass() {
		return "passback messages of-messages";
	}
	
	public String formatMessages(List<String> argMessages) {
		return formatDisplayable(argMessages, getMessageCssClass());
	}
	
	public String formatDisplayable(List<String> argItems, String argCssClass) {
		if (argItems == null || argItems.isEmpty()) {
			return "";
		} else if (argItems.size() == 1) {
			return formatSingleItem(argItems.get(0), argCssClass);
		} else {
			return formatMultipleItems(argItems, argCssClass);
		}
	}
	
	protected String formatSingleItem(String argItem, String argCssClass) {
		Validate.notBlank(argItem);
			
		if (StringUtils.isBlank(argCssClass)) {
			return "<p>" + String.valueOf(argItem) + "</p>"; // THINK: Escape?
		} else {
			return "<p class=\"" + argCssClass + "\">" + argItem + "</p>"; // THINK: Escape?
		}
	}
	
	protected String formatMultipleItems(List<String> argItems, String argCssClass) {
		Validate.notEmpty(argItems);
		Validate.isTrue(argItems.size() > 1);
		// argCssClass may be null
		
		StringBuilder lclSB = new StringBuilder();
		lclSB.append("<ul");
		if (StringUtils.isNotBlank(argCssClass)) {
			lclSB.append(" class=\"").append(argCssClass).append('\"');
		}
		lclSB.append('>');
		
		for (String lclItem : argItems) {
			lclSB.append("<li>").append(lclItem).append("</li>"); // THINK: Escape?
		}
		
		lclSB.append("</ul>");
		
		return lclSB.toString();
	}
	
	protected String displayResult(@SuppressWarnings("unused") OpalFormAction argAttemptedAction, @SuppressWarnings("unused") Trinary argSuccess) {
		// argAttemptedAction may be null, indicating that nothing was attempted (fresh form load) or we don't know what was attempted
		// argSuccess should never be null, but it might be UNKNOWN
		
		return "";
	}
	
	protected String getSubmissionConflictMessage() {
		return "Between the time you loaded the form and the time you submitted it, other changes were made, perhaps in another tab or by another user. Please use the &#8220;" + getDefaultCancelButtonLabel() + "&#8221; button to reset the form, then try your changes again.";
	}
}