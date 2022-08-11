package com.siliconage.web.form;

/**
 * @author topquark
 */
public abstract class NullSafeNameCodeExtractor<C> extends NameCodeExtractor<C> {
	public static final String NULL_STRING = "";
	public static final String NULL_CODE = "";
	
	protected NullSafeNameCodeExtractor() {
		super();
	}
	
	@Override
	public final String extractName(C argObject) {
		return argObject == null ? NULL_STRING : extractNameInternal(argObject);
	}
	
	@Override
	public final String extractCode(C argObject) {
		return argObject == null ? NULL_CODE : extractCodeInternal(argObject);
	}
	
	protected abstract String extractNameInternal(C argObject);
	
	protected abstract String extractCodeInternal(C argObject);
}
