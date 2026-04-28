package com.opal;

public interface IdentityUserFacing/*<U extends UserFacing<U>>*/ extends UserFacing/*<U>*/ { // OPALFIXME
	public void unlink();
	public void reload();
	
	public String getUniqueString();
	
	public boolean isNew();
	public boolean isDeleted();
}
