package com.opal.rest;

import java.util.Collection;

import jakarta.servlet.http.HttpServletRequest;

import com.opal.IdentityUserFacing;

public abstract class TypeOpalRestlet<U extends IdentityUserFacing, A> extends OpalRestlet<U, A> {
	private static final long serialVersionUID = 1L;
//	private static final org.slf4j.Logger ourLogger = org.slf4j.LoggerFactory.getLogger(TypeOpalRestlet.class.getName());

	public TypeOpalRestlet() {
		super();
	}
	
	@Override
	protected final boolean isEntityGetAllowed() {
		return true;
	}
	
	@Override
	protected final boolean isSearchGetAllowed() {
		return true;
	}
	
	@Override
	protected final boolean isPostAllowed() {
		return false;
	}
	
	@Override
	protected final boolean isPutAllowed() {
		return false;
	}

	@Override
	protected final boolean isDeleteAllowed() {
		return false;
	}

	@Override
	protected boolean checkAccess(U argUF, A argCredential) {
		return true; // Type tables don't generally have complicated permissions
	}
	
	@Override
	protected Collection<U> executeSearch(HttpServletRequest argRequest, A argCredential) throws RestResultException {
		return getFactory().getAll();
	}
	
}
