package com.opal;

/**
 * @author topquark
 */
public abstract class AbstractIdentityFactory<U extends IdentityUserFacing, O extends IdentityOpal<U>> extends AbstractFactory<U, O> implements IdentityFactory<U> {
	// private final IdentityOpalFactory<U, O> myOpalFactory;
	
	protected AbstractIdentityFactory(IdentityOpalFactory<U, O> argOF) {
		super(argOF);
	}
	
	/* FIXME: This should eventually return to being protected. */
	@Override
	public IdentityOpalFactory<U, O> getOpalFactory() {
		return (IdentityOpalFactory<U, O>) super.getOpalFactory();
	}
}
