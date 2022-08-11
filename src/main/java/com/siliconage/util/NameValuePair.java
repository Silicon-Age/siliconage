package com.siliconage.util;
import java.util.ArrayList;

public final class NameValuePair {
	private String myName;
	private String myValue;
	
	private static final ArrayList<NameValuePair> ourInstances = new ArrayList<>();
	
	public NameValuePair() {
		super();
	}
	
	public void checkIn() {
		synchronized(ourInstances) {
			clear();
			ourInstances.add(this);
		}
	}
	
	public static NameValuePair checkOut() {
		synchronized (ourInstances) {
			int lclSize = ourInstances.size();
			if (lclSize == 0) {
				return new NameValuePair();
			} else {
				return ourInstances.remove(lclSize-1);
			}
		}
	}
	
	public void clear() {
		setName(null);
		setValue(null);
	}
	
	public java.lang.String getName() {
		return myName;
	}
	
	public java.lang.String getValue() {
		return myValue;
	}
	
	public void parseLine(String argS) {
		clear();
		
		if (argS == null) {
			return;
		}
		
		int lclPos = argS.indexOf('=');
		if (lclPos == -1) {
			setName(argS);
			return;
		}
		setName(argS.substring(0, lclPos));
		setValue(argS.substring(lclPos+1, argS.length()));
	}
	
	public void setName(java.lang.String newName) {
		myName = newName;
	}
	
	public void setValue(java.lang.String newValue) {
		myValue = newValue;
	}
	
	@Override
	public String toString() {
		StringBuilder lclSB = new StringBuilder();
		
		lclSB.append(String.valueOf(getName()));
		lclSB.append('=');
		lclSB.append(String.valueOf(getValue()));
		return lclSB.toString();
	}
}
