package com.opal;

/* THINK: Can we explain why this doesn't have to extend OpalKey<something>?  It seems like it should . . . */
public interface DatabaseOpalKey<O extends IdentityOpal<? extends IdentityUserFacing>> {
	 
	public String[] getColumnNames();
	
	public Object[] getParameters();
	
}
