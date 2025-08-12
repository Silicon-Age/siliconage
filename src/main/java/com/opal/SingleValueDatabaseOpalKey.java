package com.opal;

public abstract class SingleValueDatabaseOpalKey<O extends IdentityOpal<? extends IdentityUserFacing>> extends SingleValueOpalKey<O> implements DatabaseOpalKey<O> {
	 
	protected SingleValueDatabaseOpalKey(Object argKeyValue) {
		super(argKeyValue);
	}
	
	@Override
	public String toString() {
		StringBuilder lclSB = new StringBuilder(64);
		String[] lclColumnNames = getColumnNames();
		Object[] lclValues = getParameters();
		
		lclSB.append('[');
		
		boolean lclFirst = true;
		for (int lclI = 0; lclI < lclColumnNames.length; ++lclI) {
			if (lclFirst) {
				lclFirst = false;
			} else {
				lclSB.append(',');
			}
			lclSB.append(lclColumnNames[lclI]);
			lclSB.append('=');
			lclSB.append(String.valueOf(lclValues[lclI]));
		}
		lclSB.append(']');
		
		return lclSB.toString();
	}
}
