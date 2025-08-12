package com.opal;

public record FieldMetaData(String name, Class<?> type, boolean nullable, int maxLength, FieldValidator validator) {
	
}
