package com.siliconage.table;
import java.text.Format;

/**
 * @author topquark
 */
public abstract class AbstractWrapperTableModel extends TableModel {
	private final TableModel myBaseTable;
	
	protected AbstractWrapperTableModel(TableModel argTable) {
		super();
		if (argTable == null) {
			throw new IllegalArgumentException("argTable is null");
		}
		myBaseTable = argTable;
	}
	
	public TableModel getBaseTable() {
		return myBaseTable;
	}
	
	@Override
	public int getRowCount() {
		return getBaseTable().getRowCount();
	}
	
	/**
	 * @see TableModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return getBaseTable().getColumnCount();
	}
	
	/**
	 * @see TableModel#getElement(int, int)
	 */
	@Override
	public Object getElement(int argRow, int argColumn) {
		return getBaseTable().getElement(argRow, argColumn);
	}
	
	@Override
	public Format getFormat(int argRow, int argColumn) {
		return getBaseTable().getFormat(argRow, argColumn);
	}
	
	/**
	 * @see TableModel#setElement(int, int, Object)
	 */
	@Override
	public void setElement(int argRow, int argColumn, Object argValue) {
		getBaseTable().setElement(argRow, argColumn, argValue);
	}
}
