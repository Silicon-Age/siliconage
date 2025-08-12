package com.opal.creator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;

import com.opal.creator.database.RelationalDatabaseAdapter;

public class XMLCreator {
	
	private final String myFilename;
	private final String mySourceDirectory;

	/* These member variables store the database configuration information that is passed in via the command-line.  Similar
	 * data can be provided via the <Database> element in the configuration file, but it is preferred to pass via the
	 * command-line for security reasons (as the configuration file is often checked into a repository.
	 * 
	 * The <Database> element will take priority over anything passed in on the command line.
	 */

	private final DBOpts myCLDBOpts; // CL = command-line
	
	public XMLCreator(String argFilename,
			String argSourceDirectory,
			DBOpts argCLDBOpts) {
		
		super();
		
		myFilename = Validate.notNull(argFilename, "argFilename is null");
		mySourceDirectory = Validate.notNull(argSourceDirectory, "argSourceDirectory is null");
		myCLDBOpts = Validate.notNull(argCLDBOpts, "argCLDBOpts is null");
	}
	
//	public XMLCreator(String argFilename, String argSourceDirectory) {
//		this(argFilename,
//				argSourceDirectory,
//				new DBOpts(
//						null,
//						null,
//						null,
////						null,
////						null,
//						null,
//						null
//						)
//				);
//	}
	
	protected String getFilename() {
		return myFilename;
	}
	
	protected String getSourceDirectory() {
		return mySourceDirectory;
	}
	
	protected DBOpts getCommandLineDBOpts() {
		return myCLDBOpts;
	}
	
//	protected String getCommandLineDatabaseUsername() {
//		return myCommandLineDatabaseUsername;
//	}
//	
//	protected String getCommandLineDatabasePassword() {
//		return myCommandLineDatabasePassword;
//	}
	
	protected static Map<String, String> processCommandLine(String[] argS) {
		Map<String, String> lclOpts = new HashMap<>();
		
		int lclUnnamedCount = 0;
		if (argS != null) {
			for (int lclI = 0; lclI < argS.length; ++lclI) {
				String lclOpt = argS[lclI];
				if (lclOpt == null) {
					continue;
				}
				lclOpt = lclOpt.trim();
//				System.out.println("Processing \"" + lclOpt + "\".");
				final String lclOptName;
				final String lclOptValue;
				if ((lclOpt.length() >= 2) && (lclOpt.charAt(0) == '-') && (lclOpt.charAt(1) == '-')) {
					int lclOptNameStart = 2;
					int lclOptNameEnd;
					int lclEquals = lclOpt.indexOf('=', lclOptNameStart);
					if (lclEquals == -1) {
						lclOptNameEnd = lclOpt.length();
					} else {
						lclOptNameEnd = lclEquals;
					}
					lclOptName = lclOpt.substring(lclOptNameStart, lclOptNameEnd);
					if (lclOptNameEnd < lclOpt.length()) {
						lclOptValue = lclOpt.substring(lclOptNameEnd + 1);
					} else {
						lclOptValue = null;
					}
				} else {
					lclOptName = "unnamed_" + lclUnnamedCount;
					lclOptValue = lclOpt;
					++lclUnnamedCount;
				}
//				System.out.println("Opt name = " + lclOptName + " value = " + lclOptValue);
				lclOpts.put(lclOptName, lclOptValue);
			}
		}
		return lclOpts;
	}
	
	public static void main(String[] argS) {

		Validate.isTrue(argS.length >= 2, "XMLCreator requires at least two command-line arguments: the configuration file and the source directory.  After that, you may optionally give the database username, and the database password.");

		Map<String, String> lclOpts = processCommandLine(argS);
				
		String lclFilename = Validate.notNull(lclOpts.get("unnamed_0"), "No configuration file name was provided; it should be the first (non-option) command-line argument");
		
		String lclSourceDirectory = Validate.notNull(lclOpts.get("unnamed_1"), "No source directory was provided; it should be the second (non-option) command-line argument");

		DBOpts lclCLDBOpts = new DBOpts( // CL = from the command-line 
				lclOpts.get("connect-string"), // Any or all of these can be null
				lclOpts.get("driver-name"),
				lclOpts.get("jndi-name"),
//				lclOpts.get("default-database"),
//				lclOpts.get("default-owner"),
				lclOpts.get("username"),
				lclOpts.get("password")
				);
		
		try {
			XMLCreator lclXMLCreator = new XMLCreator(lclFilename, lclSourceDirectory, lclCLDBOpts);
			
			lclXMLCreator.process();
		} catch (Throwable lclT) {
			lclT.printStackTrace(System.err);
		}
		
	}
	
	public void process() throws IOException {
		File lclFile = new File(getFilename());
		if (!lclFile.exists()) {
			throw new IOException("Configuration file " + getFilename() + " not found.");
		}
		
		try (FileInputStream lclFIS = new FileInputStream(lclFile)) {
			
			DocumentBuilderFactory lclFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder lclBuilder = lclFactory.newDocumentBuilder();
			
			Document lclD = lclBuilder.parse(lclFIS);
			
			OpalParseContext lclOPC = new OpalParseContext(getCommandLineDBOpts());
			lclOPC.setElementPackageName("com.opal.creator");
			lclOPC.setSourceDirectory(getSourceDirectory());
			
			lclOPC.parse(lclD);
			
			RelationalDatabaseAdapter lclRDA = lclOPC.getRelationalDatabaseAdapter();
			
			/* Now we have all the MappedClasses */
			
			lclRDA.generateClasses(lclOPC);
		} catch (Exception lclE) {
			lclE.printStackTrace(System.err);
		}
	}
	
	protected static void complain(MessageLevel argML, String argMessage) {
		complain(argML, null, argMessage);
	}
	
	protected static void complain(MessageLevel argML, MappedClass argMC, String argMessage) {
		if (argML == null) {
			complain(MessageLevel.Error, argMC, "complain() called with null MessageLevel (argMessage = \"" + argMessage + "\")"); 
		} else {
			MessageLevel argThreshold = MessageLevel.Warning;
			if (argMC != null) {
				argThreshold = argMC.getMessageLevel();
			} else {
				argThreshold = MessageLevel.Warning;
			}
			if (argML.compareTo(argThreshold) >= 0) {
				System.out.println(argML.name() + ": [" + argMC + "] " + argMessage);
			}
		}
	}
	
}
