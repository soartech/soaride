package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.TraversalUtil;
import com.soartech.soar.ide.core.sql.Triple;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.sql.datamap.DatamapInconsistency;
import com.soartech.soar.ide.core.sql.datamap.DatamapUtil;
import com.soartech.soar.ide.ui.views.search.SoarDatabaseSearchResultsView;

public class CheckRuleAgainstDatamapAction extends Action {
	
	SoarDatabaseRow row;
	
	public CheckRuleAgainstDatamapAction(SoarDatabaseRow row) {
		super ("Check Rule Against Datamaps");
		this.row = row;
	}
	
	@Override
	public void run() {
		ArrayList<DatamapInconsistency> errors = DatamapUtil.getInconsistancies(row);
		SoarDatabaseSearchResultsView.setResults(errors.toArray());
	}
}
