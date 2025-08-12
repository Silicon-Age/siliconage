package com.opal;

public interface IdentityUserFacing extends UserFacing {
	public void unlink();
	public void reload();
	
	public String getUniqueString();
	
	public boolean isNew();
	public boolean isDeleted();
}
