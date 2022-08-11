package com.siliconage.xml;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class ParseContext {
	private String myElementPackageName;
	
	public ParseContext() {
		super();
	}
	
	public XMLElement createXMLElement(XMLElement argParent, Node argNode) {
		if (argNode == null) {
			throw new IllegalArgumentException("argNode is null");
		}
		if (!(argNode instanceof Element)) {
			throw new IllegalArgumentException("argNode is not an element, but a " + argNode.getClass().getName());
		}
		String lclClassName = getElementPackageName() + '.' + argNode.getNodeName();
		XMLElement lclXMLElement;
		try {
			@SuppressWarnings("unchecked")
			Class<XMLElement> lclClass = (Class<XMLElement>) Class.forName(lclClassName);
			
			@SuppressWarnings("unchecked")
			Constructor<XMLElement> lclCtor = (Constructor<XMLElement>) lclClass.getConstructors()[0];

			/* FIXME:  Look through the constructors to find one that can be invoked with a subclass of XMLElement and a Node */
			lclXMLElement = lclCtor.newInstance(argParent, argNode);
			
			return lclXMLElement;
		} catch (ClassNotFoundException lclX) {
			throw new RuntimeException("No class found for element " + argNode, lclX);
		} catch (InvocationTargetException lclX) {
			throw new RuntimeException("Invocation exception for element " + argNode, lclX);
		} catch (InstantiationException lclX) {
			throw new RuntimeException("Could not instantiate class for element " + argNode, lclX);
		} catch (IllegalAccessException lclX) {
			throw new RuntimeException("Could not access class for element " + argNode, lclX);
		} catch (Exception lclX) {
			throw new RuntimeException("Could not create XMLElement for element " + argNode, lclX);
		}
	}
	
	public String getElementPackageName() {
		return myElementPackageName;
	}
	
	public void parse(Document argD) throws Exception {
		if (argD == null) {
			throw new IllegalArgumentException("argD is null");
		}
		XMLElement lclXMLElement = createXMLElement(null, argD.getDocumentElement());
		lclXMLElement.process(this);
	}
	public void setElementPackageName(java.lang.String argElementPackageName) {
		myElementPackageName = argElementPackageName;
	}
}
