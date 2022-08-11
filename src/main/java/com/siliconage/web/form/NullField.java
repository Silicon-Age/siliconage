package com.siliconage.web.form;

public class NullField<T extends NullField<?, V>, V> extends SingleValueFormField<T, V> {
	private static final NullField<?, ?> ourInstance = new NullField<>();
	
	private NullField() {
		super("", null);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends NullField<?, V>, V> NullField<T, V> getInstance() {
		return (NullField<T, V>) ourInstance;
	}
	
	@Override
	public String toString() {
		return "";
	}
	
	@Override
	protected void appendFormField(StringBuilder argSB) {
		return;
	}
}
