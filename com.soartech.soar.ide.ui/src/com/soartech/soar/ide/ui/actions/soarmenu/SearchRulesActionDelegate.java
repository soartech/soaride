package com.soartech.soar.ide.ui.actions.soarmenu;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.ui.views.search.SoarDatabaseSearchResultsView;

public class SearchRulesActionDelegate implements IWorkbenchWindowActionDelegate {

	@Override
	public void dispose() {
		
	}

	@Override
	public void init(IWorkbenchWindow window) {
		
	}

	@Override
	public void run(IAction action) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		InputDialog dialog = new InputDialog(shell, "Seach Rules", "Search all rules for string:", "", null);
		dialog.open();
		String query = dialog.getValue();
		if (query != null && query.length() > 0) {
			SoarDatabaseSearchResultsView.searchForRulesWithString(query);
		}
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		
	}

}
