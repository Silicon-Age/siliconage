package com.opal;

public interface OpalBacked<U extends UserFacing/*<? super U>*/, O extends Opal<? extends U>> { // OPALFIXME
	
	public O getOpal();
	public O getBottomOpal();
}
