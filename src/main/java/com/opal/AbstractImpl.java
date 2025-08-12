package com.opal;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

public abstract class AbstractImpl<U extends UserFacing, O extends Opal<? extends U>> implements OpalBacked<U, O> {

	protected AbstractImpl() {
		super();
	}
	
	/* This is the same as the default implementation, but we reimplement it to make it final.
	 */
	@Override
	public final boolean equals(Object lclO) {
		return this == lclO;
	}
	
	/* This is the same as the default implementation, but we reimplement it to make it final.
	 */
	@Override
	public final int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public abstract O getBottomOpal();
	
	// FIXME: Obviously not correct. */
	@Override
	public O getOpal() {
		return getBottomOpal();
	}
	
	@SuppressWarnings("unused")
	protected void outputSuperclassOpalFields(PrintStream argPS) throws IOException {
		return;
	}
	
	@SuppressWarnings("unused")
	protected void outputSuperclassOpalFields(PrintWriter argPW) throws IOException {
		return;
	}
	
	public void output(PrintStream argPS) throws IOException {
		outputSuperclassOpalFields(argPS);
		Opal<? extends U> lclO = getOpal();
		lclO.output(argPS);
	}
	
	public void output(PrintWriter argPW) throws IOException {
		outputSuperclassOpalFields(argPW);
		Opal<? extends U> lclO = getOpal();
		lclO.output(argPW);
	}
	
	@Override
	public String toString() {
		return getClass().getName() + "/" + String.valueOf(getBottomOpal());
	}
	
	public int getFieldCount() {
		return getBottomOpal().getFieldCount();
	}
	
	public int getFieldIndex(String argFieldName) {
		return getBottomOpal().getFieldIndex(argFieldName);
	}
	
	public String getFieldName(int argFieldIndex) {
		return getBottomOpal().getFieldName(argFieldIndex);
	}

	public Object getField(int argFieldIndex) {
		return getBottomOpal().getField(argFieldIndex);
	}
	
	public void setField(int argFieldIndex, Object argValue) {
		Opal<? extends U> lclOpal = getBottomOpal();
		if (lclOpal instanceof UpdatableOpal<? extends U> lclUO) {
			lclUO.setField(argFieldIndex, argValue);
		} else if (lclOpal instanceof ImmutableOpal<? extends U> lclIO) {
			throw new UnsupportedOperationException("Called setField on UserFacing backed by ImmutableOpal.");
		} else {
			throw new UnsupportedOperationException("Called setField on UserFacing backed by unknown Opal type.");
		}
	}
	
	public Class<?> getFieldType(int argFieldIndex) {
		return getBottomOpal().getFieldType(argFieldIndex);
	}
	
}
