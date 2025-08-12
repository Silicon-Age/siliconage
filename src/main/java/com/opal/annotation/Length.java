package com.opal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Length {
	public static final long DEFAULT_MINIMUM = 0L;
	public static final long DEFAULT_MAXIMUM = Long.MAX_VALUE;
	
	long minimum() default DEFAULT_MINIMUM;
	long maximum() default DEFAULT_MAXIMUM;
}
