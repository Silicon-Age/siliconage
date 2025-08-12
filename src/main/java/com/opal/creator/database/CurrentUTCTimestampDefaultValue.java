package com.opal.creator.database;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.commons.lang3.Validate;

import com.opal.annotation.CurrentUTCDateDefault;
import com.opal.annotation.CurrentUTCDateTimeDefault;
import com.opal.types.UTCDateTime;

/**
 * @author topquark
 * @author jonah
 */
public final class CurrentUTCTimestampDefaultValue extends DefaultValue {
	private static final CurrentUTCTimestampDefaultValue ourInstance = new CurrentUTCTimestampDefaultValue();
	
	public static final CurrentUTCTimestampDefaultValue getInstance() {
		return ourInstance;
	}
	
	private CurrentUTCTimestampDefaultValue() {
		super();
	}
	
	@Override
	public String generateAnnotation(Class<?> argType) {
		Validate.notNull(argType);
		
		if (argType == LocalDate.class) {
			return "@" + CurrentUTCDateDefault.class.getName();
		} else if (argType == LocalDateTime.class) {
			return "@" + CurrentUTCDateTimeDefault.class.getName();
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
		if (argType == UTCDateTime.class) {
			lclMethod = "now()";
		} else {
			return null; // THINK: Warning?
		}
		
		return "\t\tgetNewValues()[" + argFieldIndex + "] = " + UTCDateTime.class.getName() + '.' + lclMethod + ';';
	}
}
