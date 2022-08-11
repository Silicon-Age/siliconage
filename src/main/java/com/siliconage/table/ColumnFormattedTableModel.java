package com.siliconage.table;
import java.text.Format;

/**
 * @author topquark
 */
public class ColumnFormattedTableModel extends AbstractWrapperTableModel {
	private final Format[] myFormats;
	
	public ColumnFormattedTableModel(TableModel argTable, Format[] argFormats) {
		super(argTable);
		if (argFormats != null) {
			if (argFormats.length != getColumnCount()) {
				throw new IllegalArgumentException("argFormats has incorrect column count.");
			}
		}
		myFormats = argFormats;
	}
	
	public Format[] getFormats() {
		return myFormats;
	}
}
