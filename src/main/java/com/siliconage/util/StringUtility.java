package com.siliconage.util;

import java.util.Collection;

/**
 * Contains methods that perform routine String manipulations.
 * All non-private methods are static and final.
 * <P>
 * Copyright &copy; 2000, 2001 Silicon Age, Inc. All Rights Reserved.
 *
 * @author	<a href="mailto:kubiwan@silicon-age.com">Chad Kubicek</a>
 * @author	<a href="mailto:matt.bruce@silicon-age.com">Matt Bruce</a>
 * @author	<a href="mailto:info@silicon-age.com">Silicon Age, Inc.</a>
 */
public final class StringUtility {
	/**
	 * Default StringUtility constructor.
	 * Constructor is private as all non-private methods of this class
	 * are static and final.
	 */
	private StringUtility() {
		super();
	}
	
	/**
	 * Breaks the specified String into consecutive pieces based on what is in tags and 
	 * what is not.  Assumes that a <code>&gt;</code> always closes whatever tag we're in.
	 * @param argHTML The Collection to which the given HTML tags will be added
	 * @param argString The String from which to parse HTML tags
	 */
	public static final void acquireHTMLElements(Collection<String> argHTML, String argString) {
		// sanity check on old String
		if (argString == null || argString.length() == 0) {
			return;
		}
	
		int lclLength = argString.length();
		char lclOpenTag = '<';
		char lclCloseTag = '>';
		int lclCurrentIndex = 0;
		int lclNextIndex = -1;
		boolean lclAreWeDoneYet = false;
		boolean lclAreWeInATag = false;
		while (!lclAreWeDoneYet) {
			if (lclCurrentIndex >= lclLength) {
				lclAreWeDoneYet = true;
			} else if (lclAreWeInATag) {
				try {
					lclNextIndex = argString.indexOf (lclCloseTag, lclCurrentIndex);
					if (lclNextIndex < 0) {
						lclAreWeDoneYet = true;
						lclNextIndex = lclLength - 1;
					}
					String lclSub = argString.substring(lclCurrentIndex, lclNextIndex + 1);
					argHTML.add(lclSub);
					lclCurrentIndex = lclNextIndex + 1;
					lclAreWeInATag = (lclCurrentIndex < lclLength && argString.charAt (lclCurrentIndex) == lclOpenTag);
				} catch (ArrayIndexOutOfBoundsException e) {
					lclAreWeDoneYet = true;
				}
			} else {
				try {
					lclNextIndex = argString.indexOf (lclOpenTag, lclCurrentIndex);
					if (lclNextIndex < 0) {
						lclAreWeDoneYet = true;
						lclNextIndex = lclLength;
					}
					String lclSub = argString.substring (lclCurrentIndex, lclNextIndex);
					argHTML.add (lclSub);
					lclCurrentIndex = lclNextIndex;
					lclAreWeInATag = true;
				} catch (ArrayIndexOutOfBoundsException e) {
					lclAreWeDoneYet = true;
				}
			}
		}
	}
	
	/**
	 * Returns an array of integers whose values are represented in the
	 * specified String array.  Assumes that each String in the array
	 * is the String representation of an integer and throws a
	 * NumberFormatException if <i>any</i> String in the array is not.
	 * @author <a href="mailto:matt.bruce@silicon-age.com">Matt Bruce</a>
	 * @param argValueArray An array of <code>String</code>s that can be parsed into <code>int</code>s
	 * @return An array of <code>int</code>s parsed from the input <code>String</code>s
	 */
	public static final int[] getValuesAsIntegers(String[] argValueArray) {
		int lclLength = argValueArray.length;
		int[] lclRetVal = new int[lclLength];
		for (int i = 0; i < lclLength; i++) {
			lclRetVal[i] = Integer.parseInt(argValueArray[i]);
		}
		return lclRetVal;
	}
	
	public static String makeFilename(String argDirectory, String argPackageName, String argClassName) {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append(argDirectory);
		lclSB.append(System.getProperty("file.separator")); /* THINK:  Look this up */
		
		int lclIndex = 0;
		int lclDot = argPackageName.indexOf('.', lclIndex);
		while (lclDot >= 0) {
			lclSB.append(argPackageName.substring(lclIndex, lclDot));
			lclSB.append(System.getProperty("file.separator"));
			lclDot = argPackageName.indexOf('.', lclIndex = lclDot + 1);
		}
		
		if (lclIndex < argPackageName.length()) {
			lclSB.append(argPackageName.substring(lclIndex));
			lclSB.append(System.getProperty("file.separator"));
		}
		
		lclSB.append(argClassName);
		
		lclSB.append(".java");
		
		return lclSB.toString();
	}
	
	public static String makeDirectoryName(String argDirectory, String argPackageName) {
		StringBuilder lclSB = new StringBuilder(128);
		lclSB.append(argDirectory);
		lclSB.append(System.getProperty("file.separator")); /* THINK:  Look this up */
		
		int lclIndex = 0;
		int lclDot = argPackageName.indexOf('.', lclIndex);
		while (lclDot >= 0) {
			lclSB.append(argPackageName.substring(lclIndex, lclDot));
			lclSB.append(System.getProperty("file.separator"));
			lclDot = argPackageName.indexOf('.', lclIndex = lclDot + 1);
		}
		
		if (lclIndex < argPackageName.length()) {
			lclSB.append(argPackageName.substring(lclIndex));
//			lclSB.append(System.getProperty("file.separator"));
		}
		
//		lclSB.append(".java");
		
		return lclSB.toString();
	}
	
	/**
	 * Returns a String similar to the specifed String, but will all characters
	 * between the specified begin and end characters (inclusive) removed.  For
	 * example, removeEscapedText ("Silicon Age", 'i', ' ') would return "SAge".
	 * If the input String is null or blank, quietly returns it.  NOTE:  Removes
	 * only characters that appear BETWEEN TWO escapes.  Thus,
	 * removeEscapedText ("Silicon Age", "i", "l") would return Sicon Age.
	 * @param  argOldString The input from which to remove delimited text
	 * @param  argFirstChar The starting delimiter for the text to be removed
	 * @param  argLastChar The ending delimiter for the text to be removed
	 * @return The input string with text between given escape characters removed
	 */
	public static final String removeEscapedText(String argOldString, char argFirstChar, char argLastChar) {
		// sanity check on old String
		if (argOldString == null || argOldString.length() == 0) {
			return argOldString;
		}
		
		// doesn't waste our time if the escaping just isn't there
		int lclFirstFirst = argOldString.indexOf(argFirstChar);
		int lclLastLast   = argOldString.indexOf(argLastChar);
		if (lclFirstFirst == -1 || lclLastLast == -1) {
			return argOldString;
		}
		
		StringBuilder lclSB = new StringBuilder(128);
		int lclFullLength = argOldString.length();
		boolean lclAreWeSkipping = false;
		
		// loops through the old String and replaces as necessary
		for (int i = 0; i < lclFullLength; i++) {
			char lclNextChar = argOldString.charAt(i);
			if (lclAreWeSkipping) {
				// we're skipping: don't add the char, no matter what
				if (lclNextChar == argLastChar) {
					lclAreWeSkipping = false;
				} else {
					// do nothing
				}
			} else {
				// we're not skipping: add the char, UNLESS it's the first escape
				int lclNextFirst = (i + 1 < lclFullLength ?
						argOldString.indexOf (argFirstChar, i + 1) :
						-1);
				int lclNextLast  = argOldString.indexOf (argLastChar, i);
				if (lclNextChar == argFirstChar &&
						lclNextLast >= 0 &&
						(lclNextLast < lclNextFirst || lclNextFirst < 0)) {
					lclAreWeSkipping = true;
				} else {
					lclSB.append (lclNextChar);
				}
			}
		}
		
		return lclSB.toString();
	}
}
