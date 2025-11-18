package com.opal.creator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Mappings extends OpalXMLElement {
	
	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(Mappings.class.getName());

	private final boolean myGenerateHttpRequestFactories;
	private final boolean myGenerateFluentMutators;
	private final boolean mySampleCollections;
	private final String myCollections;
	
	public Mappings(OpalXMLElement argParent, Node argNode) {
		super(argParent, argNode);
		
		Element lclE = (Element) argNode;
		
		myGenerateHttpRequestFactories = "True".equalsIgnoreCase(lclE.getAttribute("HttpRequestFactories"));
		ourLogger.debug("Global GenerateHttpRequestFactories parameter is " + myGenerateHttpRequestFactories);

		myGenerateFluentMutators = "True".equalsIgnoreCase(lclE.getAttribute("FluentMutators"));
		ourLogger.debug("Global FluentMutators parameter is " + myGenerateFluentMutators);
		
		mySampleCollections ="True".equalsIgnoreCase(lclE.getAttribute("SampleCollections"));
		ourLogger.debug("Global SampleCollections parameter is " + mySampleCollections);
		
		String lclCollectionsString = lclE.getAttribute("Collections");
		if (lclCollectionsString == null) {
			myCollections = "Java";
		} else if ("Trove".equalsIgnoreCase(lclCollectionsString)) {
			myCollections = "Trove";
		} else if ("Java".equalsIgnoreCase(lclCollectionsString)) {
			myCollections = "Java";
		} else {
			ourLogger.error("The only allowed values of the Collections attribute on the <Mappings> element are \"Java\" and \"Trove\".");
			myCollections = "Java";
		}
		
	}
	
	public boolean generateHttpRequestFactories() {
		return myGenerateHttpRequestFactories;
	}
	
	public boolean generateFluentMutators() {
		return myGenerateFluentMutators;
	}
	
	public boolean sampleCollections() {
		return mySampleCollections;
	}
	
	public String getCollections() {
		return myCollections;
	}
	
}
