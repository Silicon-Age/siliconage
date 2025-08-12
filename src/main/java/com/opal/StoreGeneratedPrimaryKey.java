package com.opal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface StoreGeneratedPrimaryKey {
	/* This annotation marks Opals that have a store (e.g., database) generated primary key. */ 
}

