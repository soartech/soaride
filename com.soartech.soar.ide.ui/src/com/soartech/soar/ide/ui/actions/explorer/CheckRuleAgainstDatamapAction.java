package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

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
		if (errors.size() > 0) {
			SoarDatabaseSearchResultsView.setResults(errors.toArray());
		} else {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog dialog = new MessageDialog(shell,
					"No inconsistencies found",
					null,
					"Rule checks out against its datamaps.",
					MessageDialog.INFORMATION,
					new String[] { "OK" }, 0);
			dialog.open();
			SoarDatabaseSearchResultsView.setResults(new Object[0]);
		}
	}
}
