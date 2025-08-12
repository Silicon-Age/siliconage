package com.opal.creator.database.sqlserver;

import com.opal.creator.database.CheckConstraint;

/**
 * @author topquark
 */
public class SQLServerCheckConstraint extends CheckConstraint {
	
	protected SQLServerCheckConstraint(String argName, String argText) {
		super(argName, argText);
	}
	
	@Override
	public String generateFieldValidatorCode() {
//		String lclS = getText();
//		if (lclS == null) {
//			return;
//		}
//		int lclLength = lclS.length();
//		if (lclLength < 2) {
//			return;
//		}
//		if (lclS.charAt(0) != '(')) {
//			return null;
//		}
//		if (lclS.charAt(lclLength-1) != ')') {
//			return null;
//		}
//		Object lclTokens = tokenize(lclS.substring(1, lclLength-1);
//		
//		if (lclTokens.length !=3 ) {
//			return null;
//		}
//		if (!(lclTokens[0] instanceof ColumnToken)) {
//			return null;
//		}
//		
		return null;
	}
}
/* Some examples from the NAQT database
([keyword] = upper([keyword]))
([upper_title] = upper([upper_title]))
([minimum_rewrite_size] > 0)
([gender] = 'X' or ([gender] = '?' or ([gender] = 'F' or [gender] = 'M')))
(upper([keyword]) = [keyword])
([minimum_frequency] >= 0)
([working] = 1 or [working] = 0)
([original_language] = 1 or [original_language] = 0)
([usual] = 1 or [usual] = 0)
([name] = upper([name]))
([manually_added] = 1 or [manually_added] = 0)
([minimum_size] > 0)
([name] = upper([name]))
([name] = upper([name]))
([total] >= 0)
([name] = upper([name]))
([weight] >= 0)
([upper_title] = upper([upper_title]))
([weight] >= 0)
*/
