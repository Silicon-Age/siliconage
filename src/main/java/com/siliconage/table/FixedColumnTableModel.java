package com.siliconage.table;
import java.text.Format;
import java.util.ArrayList;

/**
 * @author topquark
 */
public class FixedColumnTableModel extends TableModel {
	private ArrayList<Object[]> myRows;
	private final int myColumns;
	
	public FixedColumnTableModel(int argColumns) {
		super();
		if (argColumns < 0) {
			throw new IllegalArgumentException("argColumns must be non-negative");
		}
		myColumns = argColumns;
	}
	
	@Override
	public int getRowCount() {
		return getRows().size();
	}
	
	@Override
	public int getColumnCount() {
		return myColumns;
	}
	
	protected ArrayList<Object[]> getRows() {
		if (myRows == null) {
			myRows = new ArrayList<>();
		}
		return myRows;
	}
	
	@Override
	public Object getElement(int argRow, int argColumn) {
		return (getRows().get(argRow))[argColumn];
	}
	
	@Override
	public void setElement(int argRow, int argColumn, Object argValue) {
		(getRows().get(argRow))[argColumn] = argValue;
	}
	
	@Override
	public Format getFormat(int argRow, int argColumn) {
		return null;
	}
	
	public Object[] addRow(Object[] argRow) {
		if (argRow == null) {
			throw new IllegalArgumentException("argRow is null");
		}
		if (argRow.length != getColumnCount()) {
			throw new IllegalArgumentException("argRow has wrong column count.");
		}
		getRows().add(argRow);
		return argRow;
	}
	
	public Object[] addNewRow() {
		Object[] lclNewRow = new Object[getColumnCount()];
		getRows().add(lclNewRow);
		return lclNewRow;
	}
	
	public void clear() {
		myRows = null;
	}
}
