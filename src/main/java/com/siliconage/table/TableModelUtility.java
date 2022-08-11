package com.siliconage.table;
import java.text.Format;

/**
 * @author topquark
 */
public abstract class TableModelUtility {
	public static void appendTableAsHTML(StringBuffer argSB, TableModel argTable) {
		if (argSB == null) {
			throw new IllegalArgumentException("argSB is null");
		}
		if (argTable == null) {
			throw new IllegalArgumentException("argTable is null");
		}
		argSB.append("<table border=\"1\" cellpadding=\"3\" cellspacing=\"0\" class=\"normal\">\n");
		for (int lclR = 0; lclR < argTable.getRowCount(); ++lclR) {			
			argSB.append("<tr>\n");
			for (int lclC = 0; lclC < argTable.getColumnCount(); ++lclC) {
				argSB.append("<td class=\"normal\">");
				Format lclF = argTable.getFormat(lclR, lclC);
				Object lclO = argTable.getElement(lclR, lclC);
				if (lclF == null) {
					argSB.append(String.valueOf(lclO));
				} else {
					lclF.format(lclO, argSB, null);
				}
				argSB.append("</td>");
			}
			argSB.append("</tr>");
		}
	}
}
