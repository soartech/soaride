package com.soartech.soar.ide.ui.editors.text;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class SoarDatabaseTagEditor extends AbstractSoarDatabaseMultiRuleEditor {
	public static final String ID = "com.soartech.soar.ide.ui.editors.text.SoarDatabaseTagEditor";
	
	@Override
	protected void addRow(SoarDatabaseRow newRow) {
		SoarDatabaseRow row = getInput().getRow();
		SoarDatabaseRow.joinRows(row, newRow, row.getDatabaseConnection());
	}
}
