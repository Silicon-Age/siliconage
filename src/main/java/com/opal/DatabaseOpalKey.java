package com.opal;

/* THINK: Can we explain why this doesn't have to extend OpalKey<something>?  It seems like it should . . . */

/* Eclipse warns about the type parameter O being unused, which is true, but I can't figure out how to remove
 * or anonymize it without breaking other code.
 */
@SuppressWarnings("unused") // For the type parameter O
public /* value */ interface DatabaseOpalKey<O extends IdentityOpal<? extends IdentityUserFacing>> {
	 
	public String[] getColumnNames();
	
	public Object[] getParameters();
	
}
