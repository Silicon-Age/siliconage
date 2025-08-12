package com.opal;

public abstract class AbstractIdentityImpl<U extends IdentityUserFacing, O extends IdentityOpal<? extends U>> extends AbstractImpl<U, O> {
	
	protected AbstractIdentityImpl() {
		super();
	}
	
	public abstract void unlink();
	
	public abstract void reload();

	@Override
	public abstract O getBottomOpal();

	public String getUniqueString() {
		return getBottomOpal().getUniqueString();
	}

	public boolean isNew() {
		return getBottomOpal().isNew();
	}
	
	public boolean isDeleted() {
		return getBottomOpal().isDeleted();
	}
}
