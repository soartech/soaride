package com.soartech.soar.ide.ui.actions.explorer;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseJoinFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRowFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.views.explorer.SoarExplorerView;

public class AddChildRowActionDelegate implements IObjectActionDelegate {

	StructuredSelection ss;
	IWorkbenchPart targetPart;
	SoarDatabaseRowFolder selectedFolder = null;
	SoarDatabaseJoinFolder selectedJoinFolder = null;
	
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	@Override
	public void run(IAction action) {
		
		Table table = null;
		
		if (selectedFolder != null) {
			table = selectedFolder.getTable();
		} else if (selectedJoinFolder != null) {
			table = selectedJoinFolder.getTable();
		} else {
			// Shouldn't happen
			return;
		}
		
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String title = "New " + table.shortName();
		String message = "Enter Name:";
		String initialValue = "New " + table.shortName();
		InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
		dialog.open();
		String result = dialog.getValue();
		
		if (result != null && result.length() > 0) {
			if (selectedFolder != null) {
				selectedFolder.getRow().createChild(table, result);
			}
			else if (selectedJoinFolder != null) {
				SoarDatabaseRow oldRow = selectedJoinFolder.getRow();
				SoarDatabaseRow newRow = oldRow.getTopLevelRow().createChild(table, result);
				SoarDatabaseRow.joinRows(oldRow, newRow, oldRow.getDatabaseConnection());
			}
		}
		
		if (targetPart instanceof SoarExplorerView) {
			Object element = ss.getFirstElement();
			((SoarExplorerView) targetPart).getTreeViewer().setExpandedState(element, true);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedFolder = null;
		selectedJoinFolder = null;
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			ss = (StructuredSelection)selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseRowFolder) {
				selectedFolder = (SoarDatabaseRowFolder) obj;
				action.setEnabled(true);
				action.setText("Add New " + selectedFolder.getTable().shortName());
			}
			else if (obj instanceof SoarDatabaseJoinFolder) {
				selectedJoinFolder = (SoarDatabaseJoinFolder) obj;
				action.setEnabled(true);
				action.setText("Add New " + selectedJoinFolder.getTable().shortName());
			}
		}
	}
	
}
