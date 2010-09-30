package com.soartech.soar.ide.ui.actions.soarmenu;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.sql.datamap.DatamapInconsistency;
import com.soartech.soar.ide.core.sql.datamap.DatamapUtil;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.views.search.SoarDatabaseSearchResultsView;

public class CheckRulesAgainstDatamapsActionDelegate implements IWorkbenchWindowActionDelegate {

	@Override
	public void run(IAction action) {
		SoarDatabaseRow agent = SoarUiModelTools.selectAgent();
		if (agent == null) return;
		ArrayList<DatamapInconsistency> errors = new ArrayList<DatamapInconsistency>();
		for (SoarDatabaseRow rule : agent.getChildrenOfType(Table.RULES)) {
			errors.addAll(DatamapUtil.getInconsistancies(rule));
		}
		SoarDatabaseSearchResultsView.setResults(errors.toArray());
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
	}

}
