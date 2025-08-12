package com.opal;

public interface OpalBacked<U extends UserFacing, O extends Opal<? extends U>> {
	
	public O getOpal();
	public O getBottomOpal();
}
