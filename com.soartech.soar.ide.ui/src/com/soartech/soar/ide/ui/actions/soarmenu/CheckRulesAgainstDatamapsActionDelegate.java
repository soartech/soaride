package com.soartech.soar.ide.ui.actions.soarmenu;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

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
		if (errors.size() > 0) {
			SoarDatabaseSearchResultsView.setResults(errors.toArray());
		} else {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog dialog = new MessageDialog(shell,
					"No inconsistencies found",
					null,
					"All rules check out against datamaps.",
					MessageDialog.INFORMATION,
					new String[] { "OK" }, 0);
			dialog.open();
			SoarDatabaseSearchResultsView.setResults(new Object[0]);
		}
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
