package com.siliconage.web.form;

import java.util.function.Function;

public class FunctionalNameCodeExtractor<C> extends NullSafeNameCodeExtractor<C> {
	private final Function<? super C, String> myNamer;
	private final Function<? super C, String> myCoder;
	
	public FunctionalNameCodeExtractor(Function<? super C, String> argNamer, Function<? super C, String> argCoder) {
		super();
		
		myNamer = argNamer == null ? Object::toString : argNamer;
		myCoder = argCoder == null ? Object::toString : argCoder;
	}
	
	@Override
	protected String extractNameInternal(C argObject) {
		return myNamer.apply(argObject);
	}
	
	@Override
	protected String extractCodeInternal(C argObject) {
		return myCoder.apply(argObject);
	}
}
