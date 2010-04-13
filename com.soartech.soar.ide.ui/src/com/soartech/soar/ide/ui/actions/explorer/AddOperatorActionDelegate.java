package com.soartech.soar.ide.ui.actions.explorer;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class AddOperatorActionDelegate implements IObjectActionDelegate {

	SoarDatabaseRow selectedRow = null;
	
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	@Override
	public void run(IAction action) {
		
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow() .getShell();
		String title = "New Rule";
		String message = "Enter Name:";
		String initialValue = "New Operator";
		InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
		dialog.open();
		String result = dialog.getValue();
		
		if (result != null && result.length() > 0) {
			selectedRow.createChild(Table.OPERATORS, result);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedRow = null;
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			StructuredSelection ss = (StructuredSelection)selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseRow) {
				selectedRow = (SoarDatabaseRow) obj;
				if (selectedRow.getChildTables().contains(Table.OPERATORS)) {
					action.setEnabled(true);
				}
			}
		}
	}

}
