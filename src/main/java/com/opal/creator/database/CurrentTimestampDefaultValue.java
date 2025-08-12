package com.opal.creator.database;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.commons.lang3.Validate;

import com.opal.LocalDateCache;
import com.opal.annotation.CurrentDateDefault;
import com.opal.annotation.CurrentDateTimeDefault;

/**
 * @author topquark
 * @author jonah
 */
public final class CurrentTimestampDefaultValue extends DefaultValue {
	private static final CurrentTimestampDefaultValue ourInstance = new CurrentTimestampDefaultValue();
	
	public static final CurrentTimestampDefaultValue getInstance() {
		return ourInstance;
	}
	
	private CurrentTimestampDefaultValue() {
		super();
	}
	
	@Override
	public String generateAnnotation(Class<?> argType) {
		Validate.notNull(argType);
		
		if (argType == LocalDate.class) {
			return "@" + CurrentDateDefault.class.getName();
		} else if (argType == LocalDateTime.class) {
			return "@" + CurrentDateTimeDefault.class.getName();
		} else {
			return null;
		}
	}
	
	@Override
	public String generateDefinition(Class<?> argType, String argMemberName) {
		return null;
	}
	
	@Override
	public String generateCodeToApply(Class<?> argType, int argFieldIndex, String argDefaultValueMemberName) {
		Validate.isTrue(argFieldIndex >= 0);
		
		String lclMethod;
		if (argType == LocalDate.class) {
			lclMethod = "today()";
		} else if (argType == LocalDateTime.class) {
			lclMethod = "now()";
		} else {
			return null;
		}
		
		return "\t\tgetNewValues()[" + argFieldIndex + "] = " + LocalDateCache.class.getName() + '.' + lclMethod + ';';
	}
}
