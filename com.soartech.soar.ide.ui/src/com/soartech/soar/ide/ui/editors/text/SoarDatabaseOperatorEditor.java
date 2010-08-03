package com.soartech.soar.ide.ui.editors.text;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class SoarDatabaseOperatorEditor extends AbstractSoarDatabaseMultiRuleEditor {
	public static final String ID = "com.soartech.soar.ide.ui.editors.text.SoarDatabaseOperatorEditor";

	@Override
	protected void addRow(SoarDatabaseRow newRow) {
		SoarDatabaseRow row = getInput().getRow();
		SoarDatabaseRow.directedJoinRows(row, newRow, row.getDatabaseConnection());
	}
}
