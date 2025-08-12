package com.opal;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class OpalReference<O extends IdentityOpal<? extends IdentityUserFacing>> extends SoftReference<O> {
	private final OpalKey<O> myOpalKey;
	
	public OpalReference(O argOpal, OpalKey<O> argOK, ReferenceQueue<O> argRQ) {
		super(argOpal, argRQ);
		myOpalKey = argOK;
	}
	
	public OpalKey<O> getOpalKey() {
		return myOpalKey;
	}
}
