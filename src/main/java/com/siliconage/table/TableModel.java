package com.siliconage.table;
import java.text.Format;

/**
 * @author topquark
 */
public abstract class TableModel {
	protected TableModel() {
		super();
	}
	
	public abstract int getRowCount();
	
	public abstract int getColumnCount();
	
	public abstract Object getElement(int argRow, int argColumn);
	
	public abstract Format getFormat(int argRow, int argColumn);
	
	public abstract void setElement(int argRow, int argColumn, Object argValue);
}
