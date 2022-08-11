package com.siliconage.web;
import java.io.PrintWriter;

/**
 * @author topquark
 */
public class JavadocHelper {
	private final PrintWriter myPrintWriter;
//	private final int myWidth;
	
	private int myIndent;
	private boolean myFirstParagraph = true;
	
	public JavadocHelper(PrintWriter argPrintWriter) {
		super();
		myPrintWriter = argPrintWriter;
//		myWidth = argWidth;
	}
	
	private void indent() {
		for (int lclI = 0; lclI < myIndent; ++lclI) {
			print('\t');
		}
		return;
	}
	
	private void print(char argC) {
		myPrintWriter.print(argC);
	}
	
	private void print(String argS) {
		myPrintWriter.print(argS);
	}
	
	private void println(String argS) {
		myPrintWriter.println(argS);
	}
	
	private void println() {
		myPrintWriter.println();
	}
	
	public void start(int argIndent) {
		myIndent = argIndent;
		indent();
		println("/**");
		myFirstParagraph = true;
	}
	
	public void para(String argS) {
		indent();
		print(" * ");
		if (!myFirstParagraph) {
			print("<p>");
		}
		print(argS);
		if (!myFirstParagraph) {
			print("</p>");
		}
		println();
		indent();
		println(" *");
		myFirstParagraph = false;
	}
	
	/* Intended for primitive parameters for which the question of null or not null does not arise. */
	public void param(String argParam, String argText) {
		indent();
		print(" * @param ");
		print(argParam);
		print(' ');
		println(argText);
	}
	
	/* Intended for reference parameters. */
	public void param(String argParam, String argText, boolean argNull) {
		indent();
		print(" * @param ");
		print(argParam);
		print(' ');
		print(argText);
		if (argNull) {
			print("  May be <code>null</code>.");
		} else {
			print("  May not be <code>null</code>.");
		}
		println();
	}
	
	public void returntag(String argS) {
		indent();
		print(" * @return ");
		print(argS);
		println();
	}
	
	public void returntag(String argS, boolean argNull) {
		indent();
		if ("void".equalsIgnoreCase(argS)) {
			return;
		}
		print(" * @return ");
		print(argS);
		if (argNull) {
			print("  May be <code>null</code>.");
		} else {
			print("  Will not be <code>null</code>.");
		}
		println();
	}
	
	public void throwstag(String argException, String argText) {
		indent();
		print(" * @throws ");
		print(argException);
		print(' ');
		println(argText);
	}
	
	public void author(String argS) {
		indent();
		print(" * @author\t\t");
		println(argS);
	}
	
	public void version(String argS) {
		print(" * @version\t\t");
		println(argS);
	}
	
	public void end() {
		indent();
		println(" */");
	}
}
